/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import static java.util.Collections.emptyList;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil.mapToggleGroupToUserData;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil.sanitizeExceptionMessage;
import static net.sourceforge.pmd.util.fxdesigner.util.LanguageRegistryUtil.defaultLanguageVersion;
import static net.sourceforge.pmd.util.fxdesigner.util.LanguageRegistryUtil.getSupportedLanguageVersions;
import static net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil.latestValue;
import static net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil.rewire;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.controlsfx.control.PopOver;
import org.reactfx.Subscription;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.ClasspathClassLoader;
import net.sourceforge.pmd.util.fxdesigner.app.AbstractController;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.services.ASTManager;
import net.sourceforge.pmd.util.fxdesigner.app.services.ASTManagerImpl;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveTestCase;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveViolationRecord;
import net.sourceforge.pmd.util.fxdesigner.popups.AuxclasspathSetupController;
import net.sourceforge.pmd.util.fxdesigner.util.controls.DragAndDropUtil;
import net.sourceforge.pmd.util.fxdesigner.util.LanguageRegistryUtil;
import net.sourceforge.pmd.util.fxdesigner.util.ResourceUtil;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;
import net.sourceforge.pmd.util.fxdesigner.util.controls.AstTreeView;
import net.sourceforge.pmd.util.fxdesigner.util.controls.NodeEditionCodeArea;
import net.sourceforge.pmd.util.fxdesigner.util.controls.NodeParentageCrumbBar;
import net.sourceforge.pmd.util.fxdesigner.util.controls.PopOverWrapper;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ToolbarTitledPane;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ViolationCollectionView;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;


/**
 * One editor, i.e. source editor and ast tree view. The {@link NodeEditionCodeArea} handles the
 * presentation of different types of nodes in separate layers. This class handles configuration,
 * language selection and such.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public class SourceEditorController extends AbstractController {

    private static final Duration AST_REFRESH_DELAY = Duration.ofMillis(100);
    private final ASTManager astManager;
    private final Var<List<File>> auxclasspathFiles = Var.newSimpleVar(emptyList());
    private final Val<ClassLoader> auxclasspathClassLoader = auxclasspathFiles.<ClassLoader>map(fileList -> {
        try {
            return new ClasspathClassLoader(fileList, SourceEditorController.class.getClassLoader());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }).orElseConst(SourceEditorController.class.getClassLoader());

    private final Var<LiveTestCase> currentlyOpenTestCase = Var.newSimpleVar(null);

    @FXML
    private ToolbarTitledPane testCaseToolsTitledPane;
    @FXML
    private Button violationsButton;
    @FXML
    private Button propertiesMapButton;


    @FXML
    private ToolbarTitledPane astTitledPane;
    @FXML
    private ToolbarTitledPane editorTitledPane;
    @FXML
    private MenuButton languageSelectionMenuButton;
    @FXML
    private AstTreeView astTreeView;
    @FXML
    private NodeEditionCodeArea nodeEditionCodeArea;
    @FXML
    private NodeParentageCrumbBar focusNodeParentageCrumbBar;

    private final PopOverWrapper<LiveTestCase> violationsPopover;

    private Var<LanguageVersion> languageVersionUIProperty;


    public SourceEditorController(DesignerRoot designerRoot) {
        super(designerRoot);
        this.astManager = new ASTManagerImpl(designerRoot);

        designerRoot.registerService(DesignerRoot.AST_MANAGER, astManager);

        violationsPopover = new PopOverWrapper<>(this::rebindPopover);
    }

    private PopOver rebindPopover(LiveTestCase testCase, PopOver existing) {
        if (testCase == null && existing != null) {
            existing.hide();
            return existing;
        }

        if (testCase != null) {
            if (existing == null) {
                return ViolationCollectionView.makePopOver(testCase, getDesignerRoot());
            } else {
                ViolationCollectionView view = (ViolationCollectionView) existing.getUserData();
                view.setItems(testCase.getExpectedViolations());
                return existing;
            }
        }
        return null;
    }


    @Override
    protected void beforeParentInit() {
        initializeLanguageSelector(); // languageVersionProperty() must be initialized

        languageVersionProperty().values()
                                 .filterMap(Objects::nonNull, LanguageVersion::getLanguage)
                                 .distinct()
                                 .subscribe(nodeEditionCodeArea::updateSyntaxHighlighter);

        languageVersionProperty().values()
                                 .filter(Objects::nonNull)
                                 .map(LanguageVersion::getShortName)
                                 .map(lang -> "Source Code (" + lang + ")")
                                 .subscribe(editorTitledPane::setTitle);

        astManager.languageVersionProperty()
                  .changes()
                  .subscribe(c -> astTreeView.setAstRoot(null));

        ((ASTManagerImpl) astManager).classLoaderProperty().bind(auxclasspathClassLoader);

        // default text, will be overwritten by settings restore
        setText(getDefaultText());

        violationsButton.setOnAction(e -> violationsPopover.showOrFocus(p -> p.show(violationsButton)));

        violationsButton.textProperty().bind(
            currentlyOpenTestCase.flatMap(it -> it.getExpectedViolations().sizeProperty())
                                 .map(it -> "Expected violations (" + it + ")")
                                 .orElseConst("Expected violations")
        );

        DragAndDropUtil.registerAsNodeDragTarget(
            violationsButton,
            range -> {
                LiveViolationRecord record = new LiveViolationRecord();
                record.setRange(range);
                record.setExactRange(true);
                currentlyOpenTestCase.ifPresent(v -> v.getExpectedViolations().add(record));
            });
    }

    @Override
    public void afterParentInit() {

        rewire(((ASTManagerImpl) astManager).languageVersionProperty(), languageVersionUIProperty);

        nodeEditionCodeArea.replaceText(astManager.getSourceCode());

        Var<String> areaText = Var.fromVal(
            latestValue(nodeEditionCodeArea.plainTextChanges()
                                           .successionEnds(AST_REFRESH_DELAY)
                                           .map(it -> nodeEditionCodeArea.getText())),
            text -> nodeEditionCodeArea.replaceText(text)
        );

        areaText.bindBidirectional(astManager.sourceCodeProperty());


        nodeEditionCodeArea.moveCaret(0, 0);

        initTreeView(astManager, astTreeView, editorTitledPane.errorMessageProperty());

        getDesignerRoot().registerService(DesignerRoot.RICH_TEXT_MAPPER, nodeEditionCodeArea);

        getService(DesignerRoot.TEST_LOADER)
            .messageStream(true, this)
            .subscribe(this::handleTestOpenRequest);

        currentlyOpenTestCase.values().subscribe(violationsPopover::rebind);


    }

    private void handleTestOpenRequest(@NonNull LiveTestCase liveTestCase) {
        if (currentlyOpenTestCase.isPresent()) {
            // TODO
            currentlyOpenTestCase.getValue().commitChanges();
        }
        if (!liveTestCase.getSource().equals(nodeEditionCodeArea.getText())) {
            nodeEditionCodeArea.replaceText(liveTestCase.getSource());
        }
        currentlyOpenTestCase.setValue(liveTestCase);
        Subscription sub = ReactfxUtil.rewireInit(liveTestCase.sourceProperty(), astManager.sourceCodeProperty());
        liveTestCase.addCommitHandler(t -> sub.unsubscribe());
    }


    private String getDefaultText() {
        try {
            // TODO this should take language into account
            //  it doesn't handle the case where java is not on the classpath

            return IOUtils.resourceToString(ResourceUtil.resolveResource("placeholders/editor.java"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "class Foo {\n"
                + "\n"
                + "    /*\n"
                + "        Welcome to the PMD Rule designer :)\n"
                + "\n"
                + "        Type some code in this area\n"
                + "        \n"
                + "        On the right, the Abstract Syntax Tree is displayed\n"
                + "        On the left, you can examine the XPath attributes of\n"
                + "        the nodes you select\n"
                + "        \n"
                + "        You can set the language you'd like to work in with\n"
                + "        the cog icon above this code area\n"
                + "     */\n"
                + "\n"
                + "    int i = 0;\n"
                + "}";
        }
    }


    private void initializeLanguageSelector() {

        ToggleGroup languageToggleGroup = new ToggleGroup();

        getSupportedLanguageVersions()
            .stream()
            .sorted(LanguageVersion::compareTo)
            .map(lv -> {
                RadioMenuItem item = new RadioMenuItem(lv.getShortName());
                item.setUserData(lv);
                return item;
            })
            .forEach(item -> {
                languageToggleGroup.getToggles().add(item);
                languageSelectionMenuButton.getItems().add(item);
            });

        languageVersionUIProperty = mapToggleGroupToUserData(languageToggleGroup, LanguageRegistryUtil::defaultLanguageVersion);
        // this will be overwritten by property restore if needed
        languageVersionUIProperty.setValue(defaultLanguageVersion());
    }


    public void showAuxclasspathSetupPopup() {
        new AuxclasspathSetupController(getDesignerRoot()).show(getMainStage(), auxclasspathFiles.getValue(), auxclasspathFiles::setValue);
    }


    public Var<List<Node>> currentRuleResultsProperty() {
        return nodeEditionCodeArea.currentRuleResultsProperty();
    }


    public Var<List<Node>> currentErrorNodesProperty() {
        return nodeEditionCodeArea.currentErrorNodesProperty();
    }


    public LanguageVersion getLanguageVersion() {
        return languageVersionUIProperty.getValue();
    }


    public void setLanguageVersion(LanguageVersion version) {
        languageVersionUIProperty.setValue(version);
    }


    public Var<LanguageVersion> languageVersionProperty() {
        return languageVersionUIProperty;
    }


    public String getText() {
        return nodeEditionCodeArea.getText();
    }


    public void setText(String expression) {
        nodeEditionCodeArea.replaceText(expression);
    }


    public Val<String> textProperty() {
        return Val.wrap(nodeEditionCodeArea.textProperty());
    }


    @PersistentProperty
    public List<File> getAuxclasspathFiles() {
        return auxclasspathFiles.getValue();
    }


    public void setAuxclasspathFiles(List<File> files) {
        auxclasspathFiles.setValue(files);
    }


    @Override
    public List<? extends SettingsOwner> getChildrenSettingsNodes() {
        return Collections.singletonList(astManager);
    }

    @Override
    public String getDebugName() {
        return "editor";
    }


    /**
     * Refreshes the AST and returns the new compilation unit if the parse didn't fail.
     */
    private static void initTreeView(ASTManager manager,
                                     AstTreeView treeView,
                                     Var<String> errorMessageProperty) {

        manager.sourceCodeProperty()
               .values()
               .filter(StringUtils::isBlank)
               .subscribe(code -> treeView.setAstRoot(null));

        manager.currentExceptionProperty()
               .values()
               .subscribe(e -> {
                   if (e == null) {
                       errorMessageProperty.setValue(null);
                   } else {
                       errorMessageProperty.setValue(sanitizeExceptionMessage(e));
                   }
               });

        manager.compilationUnitProperty()
               .values()
               .filter(Objects::nonNull)
               .subscribe(node -> {
                   errorMessageProperty.setValue("");
                   treeView.setAstRoot(node);
               });
    }

}
