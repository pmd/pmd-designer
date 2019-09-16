/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import static net.sourceforge.pmd.util.fxdesigner.popups.SimplePopups.showLicensePopup;
import static net.sourceforge.pmd.util.fxdesigner.util.LanguageRegistryUtil.defaultLanguage;
import static net.sourceforge.pmd.util.fxdesigner.util.LanguageRegistryUtil.getLanguageVersionFromExtension;
import static net.sourceforge.pmd.util.fxdesigner.util.LanguageRegistryUtil.getSupportedLanguages;
import static net.sourceforge.pmd.util.fxdesigner.util.LanguageRegistryUtil.isXmlDialect;
import static net.sourceforge.pmd.util.fxdesigner.util.LanguageRegistryUtil.plainTextLanguage;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.reactfx.Subscription;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.util.fxdesigner.app.AbstractController;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.popups.EventLogController;
import net.sourceforge.pmd.util.fxdesigner.popups.SimplePopups;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.LanguageRegistryUtil;
import net.sourceforge.pmd.util.fxdesigner.util.LimitedSizeStack;
import net.sourceforge.pmd.util.fxdesigner.util.SoftReferenceCache;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;
import net.sourceforge.pmd.util.fxdesigner.util.controls.DynamicWidthChoicebox;

import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;


/**
 * Main controller of the app. Mediator for subdivisions of the UI.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
@SuppressWarnings("PMD.UnusedPrivateField")
public class MainDesignerController extends AbstractController {


    /* Menu bar */
    // help
    @FXML
    private MenuItem aboutMenuItem;
    @FXML
    private MenuItem reportIssueMenuItem;
    @FXML
    private MenuItem docMenuItem;
    @FXML
    private MenuItem licenseMenuItem;

    // view
    @FXML
    private MenuItem setupAuxclasspathMenuItem;
    @FXML
    public MenuItem openEventLogMenuItem;

    // file
    @FXML
    private Menu fileMenu;
    @FXML
    private MenuItem openFileMenuItem;
    @FXML
    private MenuItem saveMenuItem;
    @FXML
    private Menu openRecentMenu;

    /* Bottom panel */
    @FXML
    private SplitPane mainHorizontalSplitPane;
    @FXML
    private Tab metricResultsTab;


    /* Children */
    @FXML
    private RuleEditorsController ruleEditorsController;
    @FXML
    private SourceEditorController sourceEditorController;

    @FXML
    private NodeDetailPaneController nodeDetailsTabController;
    @FXML
    private MetricPaneController metricPaneController;
    @FXML
    private ScopesPanelController scopesPanelController;


    private final Var<Language> globalLanguage = Var.newSimpleVar(defaultLanguage());

    // we cache it but if it's not used the FXML is not created, etc
    private final SoftReferenceCache<EventLogController> eventLogController;
    @FXML
    private DynamicWidthChoicebox<Language> languageChoicebox;

    // Other fields
    private final Stack<File> recentFiles = new LimitedSizeStack<>(5);

    public MainDesignerController(@NamedArg("designerRoot") DesignerRoot designerRoot) {
        super(designerRoot);
        eventLogController = new SoftReferenceCache<>(() -> new EventLogController(designerRoot));

        designerRoot.registerService(DesignerRoot.APP_GLOBAL_LANGUAGE, globalLanguage.orElseConst(defaultLanguage()));
    }



    @Override
    protected void beforeParentInit() {
        getDesignerRoot().getService(DesignerRoot.PERSISTENCE_MANAGER).restoreSettings(this);

        licenseMenuItem.setOnAction(e -> showLicensePopup());
        openFileMenuItem.setOnAction(e -> onOpenFileClicked());
        openRecentMenu.setOnAction(e -> updateRecentFilesMenu());
        openRecentMenu.setOnShowing(e -> updateRecentFilesMenu());
        saveMenuItem.setOnAction(e -> getService(DesignerRoot.PERSISTENCE_MANAGER).persistSettings(this));
        fileMenu.setOnShowing(e -> onFileMenuShowing());
        aboutMenuItem.setOnAction(e -> SimplePopups.showAboutPopup(getDesignerRoot()));
        docMenuItem.setOnAction(e -> getService(DesignerRoot.HOST_SERVICES).showDocument(DesignerUtil.DESIGNER_DOC_URL));
        reportIssueMenuItem.setOnAction(e -> getService(DesignerRoot.HOST_SERVICES).showDocument(DesignerUtil.DESIGNER_NEW_ISSUE_URL));
        setupAuxclasspathMenuItem.setOnAction(e -> sourceEditorController.showAuxclasspathSetupPopup());

        openEventLogMenuItem.setOnAction(e -> {
            EventLogController wizard = eventLogController.get();
            Subscription parentToWizSubscription = wizard.errorNodesProperty().values().subscribe(sourceEditorController.currentErrorNodesProperty()::setValue);
            wizard.showPopup(parentToWizSubscription);
        });
        openEventLogMenuItem.textProperty().bind(
            getLogger().numNewLogEntriesProperty().map(i -> "Event _Log (" + (i > 0 ? i : "no") + " new)")
        );

        languageChoicebox.getItems().addAll(getSupportedLanguages().sorted().collect(Collectors.toList()));
        languageChoicebox.setConverter(DesignerUtil.stringConverter(Language::getName, LanguageRegistryUtil::findLanguageByName));

        SingleSelectionModel<Language> langSelector = languageChoicebox.getSelectionModel();
        @NonNull Language restored = globalLanguage.getOrElse(defaultLanguage());

        globalLanguage.bind(langSelector.selectedItemProperty());

        langSelector.select(restored);

        Platform.runLater(() -> {
            langSelector.clearSelection();
            langSelector.select(restored);
        });

    }


    @Override
    protected void afterChildrenInit() {
        updateRecentFilesMenu();

        ruleEditorsController.currentRuleResults()
                             .values()
                             .subscribe(sourceEditorController.currentRuleResultsProperty()::setValue);

        metricPaneController.numAvailableMetrics().values().subscribe(n -> {
            metricResultsTab.setText("Metrics\t(" + (n == 0 ? "none" : n) + ")");
            metricResultsTab.setDisable(n == 0);
        });
    }


    private void onFileMenuShowing() {
        openRecentMenu.setDisable(recentFiles.isEmpty());
    }


    private void onOpenFileClicked() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load source from file");
        File file = chooser.showOpenDialog(getMainStage());
        loadSourceFromFile(file);
    }

    private void loadSourceFromFile(File file) {
        if (file != null) {
            try {
                String source = IOUtils.toString(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8);
                sourceEditorController.setText(source);
                LanguageVersion guess = getLanguageVersionFromExtension(file.getName());
                if (guess == null) {

                    if (!isXmlDialect(getGlobalLanguage())) {
                        // if we're a xml language, assume the file is some xml dialect too,
                        //   otherwise go back to plain text
                        sourceEditorController.setLanguageVersion(plainTextLanguage().getDefaultVersion());
                    }

                    if (getSupportedLanguages().count() > 1) {
                        SimplePopups.showActionFeedback(
                            languageChoicebox,
                            AlertType.INFORMATION,
                            "Pick a language?"
                        );
                    }
                } else if (guess != sourceEditorController.getLanguageVersion()) {
                    // guess the language from the extension
                    sourceEditorController.setLanguageVersion(guess);
                    SimplePopups.showActionFeedback(
                        languageChoicebox,
                        AlertType.CONFIRMATION,
                        "Set language to " + guess.getLanguage().getName()
                    );
                }

                recentFiles.push(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private void updateRecentFilesMenu() {
        List<MenuItem> items = new ArrayList<>();
        List<File> filesToClear = new ArrayList<>();

        for (final File f : recentFiles) {
            if (f.exists() && f.isFile()) {
                CustomMenuItem item = new CustomMenuItem(new Label(f.getName()));
                item.setOnAction(e -> loadSourceFromFile(f));
                item.setMnemonicParsing(false);
                Tooltip.install(item.getContent(), new Tooltip(f.getAbsolutePath()));
                items.add(item);
            } else {
                filesToClear.add(f);
            }
        }
        recentFiles.removeAll(filesToClear);

        if (items.isEmpty()) {
            openRecentMenu.setDisable(true);
            return;
        }

        Collections.reverse(items);

        items.add(new SeparatorMenuItem());
        MenuItem clearItem = new MenuItem();
        clearItem.setText("Clear menu");
        clearItem.setOnAction(e -> {
            recentFiles.clear();
            openRecentMenu.setDisable(true);
        });
        items.add(clearItem);

        openRecentMenu.getItems().setAll(items);
    }


    @PersistentProperty
    public List<File> getRecentFiles() {
        return recentFiles;
    }


    public void setRecentFiles(List<File> files) {
        files.forEach(recentFiles::push);
    }


    @PersistentProperty
    public boolean isMaximized() {
        return getMainStage().isMaximized();
    }


    public void setMaximized(boolean b) {
        getMainStage().setMaximized(!b); // trigger change listener anyway
        getMainStage().setMaximized(b);
    }

    @PersistentProperty
    public Language getGlobalLanguage() {
        return globalLanguage.getValue();
    }

    public void setGlobalLanguage(Language lang) {
        globalLanguage.setValue(lang);
    }

    @Override
    public List<AbstractController> getChildren() {
        return Arrays.asList(ruleEditorsController,
                             sourceEditorController,
                             nodeDetailsTabController,
                             metricPaneController,
                             scopesPanelController);
    }



    @Override
    public String getDebugName() {
        return "MAIN";
    }

}
