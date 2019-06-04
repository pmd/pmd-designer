/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;


import static net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil.sanitizeExceptionMessage;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.kordamp.ikonli.javafx.FontIcon;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;
import org.reactfx.SuspendableEventStream;
import org.reactfx.collection.LiveArrayList;
import org.reactfx.util.FxTimer;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.rule.xpath.XPathRuleQuery;
import net.sourceforge.pmd.util.fxdesigner.app.AbstractController;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.NodeSelectionSource;
import net.sourceforge.pmd.util.fxdesigner.app.XPathUpdateSubscriber;
import net.sourceforge.pmd.util.fxdesigner.app.services.CloseableService;
import net.sourceforge.pmd.util.fxdesigner.app.services.LogEntry.Category;
import net.sourceforge.pmd.util.fxdesigner.model.ObservableRuleBuilder;
import net.sourceforge.pmd.util.fxdesigner.model.ObservableXPathRuleBuilder;
import net.sourceforge.pmd.util.fxdesigner.model.VersionedXPathQuery;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveTestCase;
import net.sourceforge.pmd.util.fxdesigner.popups.ExportXPathWizardController;
import net.sourceforge.pmd.util.fxdesigner.util.DataHolder;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.SoftReferenceCache;
import net.sourceforge.pmd.util.fxdesigner.util.TextAwareNodeWrapper;
import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.CompletionResultSource;
import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.XPathAutocompleteProvider;
import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.XPathCompletionSource;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.SyntaxHighlightingCodeArea;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.XPathSyntaxHighlighter;
import net.sourceforge.pmd.util.fxdesigner.util.controls.HelpfulPlaceholder;
import net.sourceforge.pmd.util.fxdesigner.util.controls.PopOverWrapper;
import net.sourceforge.pmd.util.fxdesigner.util.controls.PropertyCollectionView;
import net.sourceforge.pmd.util.fxdesigner.util.controls.TitleOwner;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ToolbarTitledPane;
import net.sourceforge.pmd.util.fxdesigner.util.controls.XpathViolationListCell;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


/**
 * Editor for an XPath rule. This object maintains an {@link ObservableRuleBuilder} which stores information
 * about the currently edited rule. The properties of that builder are rewired to the export wizard's fields
 * when it's open. The wizard is just one view on the builder's data, which is supposed to offer the most
 * customization options. Other views can be implemented in a similar way, for example, PropertyView
 * implements a view over the properties of the builder.
 *
 * @author Clément Fournier
 * @see ExportXPathWizardController
 * @since 6.0.0
 */
public final class XPathRuleEditorController extends AbstractController implements NodeSelectionSource, TitleOwner, CloseableService {

    private static final String NO_MATCH_MESSAGE = "No match in text";
    private static final Duration XPATH_REFRESH_DELAY = Duration.ofMillis(100);
    private static final Pattern JAXEN_MISSING_PROPERTY_EXTRACTOR = Pattern.compile("Variable (\\w+)");
    private static final Pattern SAXON_MISSING_PROPERTY_EXTRACTOR = Pattern.compile("Undeclared variable in XPath expression: \\$(\\w+)");
    private final SoftReferenceCache<ExportXPathWizardController> exportWizard;
    private final ObservableXPathRuleBuilder ruleBuilder;
    private final Var<ObservableList<Node>> myXpathResults = Var.newSimpleVar(null);
    private final Var<List<Node>> currentResults = Var.newSimpleVar(Collections.emptyList());
    private final PopOverWrapper<ObservableXPathRuleBuilder> propertiesPopover;

    @FXML
    public TestCollectionController testCollectionController;
    @FXML
    private ToolbarTitledPane expressionTitledPane;
    @FXML
    private Button exportXpathToRuleButton;
    @FXML
    private Button showPropertiesButton;
    @FXML
    private MenuButton xpathVersionMenuButton;
    @FXML
    private SyntaxHighlightingCodeArea xpathExpressionArea;
    @FXML
    private ToolbarTitledPane violationsTitledPane;
    @FXML
    private ListView<TextAwareNodeWrapper> xpathResultListView;
    // ui property
    private Var<String> xpathVersionUIProperty = Var.newSimpleVar(XPathRuleQuery.XPATH_2_0);
    private SuspendableEventStream<TextAwareNodeWrapper> selectionEvents;

    public XPathRuleEditorController(DesignerRoot root) {
        this(root, new ObservableXPathRuleBuilder());
    }

    /**
     * Creates a controller with an existing rule builder.
     */
    public XPathRuleEditorController(DesignerRoot root, ObservableXPathRuleBuilder ruleBuilder) {
        super(root);
        this.testCollectionController = new TestCollectionController(root, ruleBuilder);
        this.ruleBuilder = ruleBuilder;

        if (ruleBuilder.getLanguage() == null) {
            ruleBuilder.setLanguage(globalLanguageProperty().getValue());
        }

        this.exportWizard = new SoftReferenceCache<>(() -> new ExportXPathWizardController(root));
        this.propertiesPopover = new PopOverWrapper<>((t, f) -> PropertyCollectionView.makePopOver(t, titleProperty(), root));
    }

    @Override
    protected void beforeParentInit() {

        initGenerateXPathFromStackTrace();
        initialiseVersionSelection();

        expressionTitledPane.titleProperty().bind(xpathVersionUIProperty.map(v -> "XPath Expression (" + v + ")"));

        xpathResultListView.setCellFactory(v -> new XpathViolationListCell(getDesignerRoot()));

        exportXpathToRuleButton.setOnAction(e -> showExportXPathToRuleWizard());

        // this is the source of xpath results
        getRuleBuilder().modificationsTicks().successionEnds(XPATH_REFRESH_DELAY)
                        .map(tick -> new VersionedXPathQuery(
                                 getRuleBuilder().getXpathVersion(),
                                 getRuleBuilder().getXpathExpression(),
                                 getRuleBuilder().getRuleProperties()
                             )
                        )
                        .subscribe(tick -> getService(DesignerRoot.LATEST_XPATH).pushEvent(this, tick));

        new MyXpathSubscriber(getDesignerRoot()).init(getService(DesignerRoot.AST_MANAGER));


        selectionEvents = EventStreams.valuesOf(xpathResultListView.getSelectionModel().selectedItemProperty()).suppressible();

        initNodeSelectionHandling(getDesignerRoot(),
                                  selectionEvents.filter(Objects::nonNull).map(TextAwareNodeWrapper::getNode).map(NodeSelectionEvent::of),
                                  false);

        violationsTitledPane.titleProperty().bind(currentResults.map(List::size).map(n -> "Matched nodes (" + n + ")"));


        showPropertiesButton.setOnAction(e -> propertiesPopover.showOrFocus(p -> p.show(showPropertiesButton)));

        propertiesPopover.rebind(getRuleBuilder());
        propertiesPopover.doFirstLoad(getMainStage());

        expressionTitledPane.errorTypeProperty().setValue("XPath syntax error");
    }

    @Override
    public void setFocusNode(Node node, DataHolder options) {
        Optional<TextAwareNodeWrapper> firstResult = xpathResultListView.getItems().stream()
                                                                        .filter(wrapper -> wrapper.getNode().equals(node))
                                                                        .findFirst();

        // with Java 9, Optional#ifPresentOrElse can be used
        if (firstResult.isPresent()) {
            selectionEvents.suspendWhile(() -> xpathResultListView.getSelectionModel().select(firstResult.get()));
        } else {
            xpathResultListView.getSelectionModel().clearSelection();
        }
    }

    public Val<LiveTestCase> selectedTestCaseProperty() {
        return testCollectionController.selectedTestCase();
    }

    @Override
    public void afterParentInit() {
        bindToParent();

        // init autocompletion only after binding to mediator and settings restore
        // otherwise the popup is shown on startup
        Supplier<CompletionResultSource> suggestionMaker = () -> XPathCompletionSource.forLanguage(getRuleBuilder().getLanguage());
        new XPathAutocompleteProvider(xpathExpressionArea, suggestionMaker).initialiseAutoCompletion();
    }


    // Binds the underlying rule parameters to the parent UI, disconnecting it from the wizard if need be
    private void bindToParent() {
        if (getRuleBuilder().getLanguage() == null) {
            DesignerUtil.rewire(getRuleBuilder().languageProperty(),
                                globalLanguageProperty());
        }

        ReactfxUtil.rewireInit(getRuleBuilder().xpathVersionProperty(), xpathVersionProperty());
        ReactfxUtil.rewireInit(getRuleBuilder().xpathExpressionProperty(), xpathExpressionProperty());

        xpathExpressionArea.setSyntaxHighlighter(new XPathSyntaxHighlighter());
    }

    private void initialiseVersionSelection() {
        ToggleGroup xpathVersionToggleGroup = new ToggleGroup();

        List<String> versionItems = new ArrayList<>();
        versionItems.add(XPathRuleQuery.XPATH_1_0);
        versionItems.add(XPathRuleQuery.XPATH_1_0_COMPATIBILITY);
        versionItems.add(XPathRuleQuery.XPATH_2_0);

        versionItems.forEach(v -> {
            RadioMenuItem item = new RadioMenuItem("XPath " + v);
            item.setUserData(v);
            item.setToggleGroup(xpathVersionToggleGroup);
            xpathVersionMenuButton.getItems().add(item);
        });

        xpathVersionUIProperty = DesignerUtil.mapToggleGroupToUserData(xpathVersionToggleGroup, DesignerUtil::defaultXPathVersion);

        xpathVersionProperty().setValue(XPathRuleQuery.XPATH_2_0);
    }


    private void initGenerateXPathFromStackTrace() {

        ContextMenu menu = new ContextMenu();

        MenuItem item = new MenuItem("Generate from stack trace...");
        item.setOnAction(e -> {
            try {
                Stage popup = new Stage();
                FXMLLoader loader = new FXMLLoader(DesignerUtil.getFxml("generate-xpath-from-stack-trace"));
                Parent root = loader.load();
                Button button = (Button) loader.getNamespace().get("generateButton");
                TextArea area = (TextArea) loader.getNamespace().get("stackTraceArea");

                ValidationSupport validation = new ValidationSupport();

                validation.registerValidator(area, Validator.createEmptyValidator("The stack trace may not be empty"));
                button.disableProperty().bind(validation.invalidProperty());

                button.setOnAction(f -> {
                    DesignerUtil.stackTraceToXPath(area.getText()).ifPresent(xpathExpressionArea::replaceText);
                    popup.close();
                });

                popup.setScene(new Scene(root));
                popup.initStyle(StageStyle.UTILITY);
                popup.initModality(Modality.WINDOW_MODAL);
                popup.initOwner(getDesignerRoot().getMainStage());
                popup.show();
            } catch (IOException e1) {
                throw new RuntimeException(e1);
            }
        });

        menu.getItems().add(item);

        xpathExpressionArea.addEventHandler(MouseEvent.MOUSE_CLICKED, t -> {
            if (t.getButton() == MouseButton.SECONDARY) {
                menu.show(xpathExpressionArea, t.getScreenX(), t.getScreenY());
            }
        });
    }

    @Override
    public void close() {
        xpathExpressionArea.setSyntaxHighlighter(null);
    }


    public void showExportXPathToRuleWizard() {
        ExportXPathWizardController wizard = exportWizard.get();
        wizard.showYourself(bindToExportWizard(wizard));
    }


    /**
     * Binds the properties of the panel to the export wizard.
     *
     * @param exportWizard The caller
     */
    private Subscription bindToExportWizard(ExportXPathWizardController exportWizard) {

        return exportWizard.bindToRuleBuilder(getRuleBuilder()).and(this::bindToParent);

    }


    public Var<String> xpathExpressionProperty() {
        return Var.fromVal(xpathExpressionArea.textProperty(), xpathExpressionArea::replaceText);
    }


    public Var<String> xpathVersionProperty() {
        return xpathVersionUIProperty;
    }


    public ObservableXPathRuleBuilder getRuleBuilder() {
        return ruleBuilder;
    }


    @Override
    public Val<String> titleProperty() {

        Val<Function<String, String>> languagePrefix =
            getRuleBuilder().languageProperty()
                            .map(Language::getTerseName)
                            .map(lname -> rname -> lname + "/" + rname);

        return getRuleBuilder().nameProperty()
                               .orElseConst("NewRule")
                               .mapDynamic(languagePrefix);
    }

    public Val<List<Node>> currentResultsProperty() {
        return currentResults;
    }

    public Var<ObservableList<Node>> xpathResultsProperty() {
        return myXpathResults;
    }

    private void updateResults(boolean xpathError,
                               boolean otherError,
                               List<Node> results,
                               String emptyResultsPlaceholder) {

        javafx.scene.Node emptyLabel = xpathError || otherError
                                       ? getErrorPlaceholder(emptyResultsPlaceholder)
                                       : new Label(emptyResultsPlaceholder);

        xpathResultListView.setPlaceholder(emptyLabel);

        // we wait a bit to do that, so that the rich text is up to date
        FxTimer.runLater(Duration.ofMillis(100), () -> xpathResultListView.setItems(results.stream().map(getDesignerRoot().getService(DesignerRoot.RICH_TEXT_MAPPER)::wrapNode).collect(Collectors.toCollection(LiveArrayList::new))));

        this.currentResults.setValue(results);
        // only show the error label here when it's an xpath error
        expressionTitledPane.errorMessageProperty().setValue(xpathError ? emptyResultsPlaceholder : "");
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private void addProperty(String name) {
        // TODO
    }

    @Override
    public List<AbstractController> getChildren() {
        return Collections.singletonList(testCollectionController);
    }

    private javafx.scene.Node getErrorPlaceholder(String message) {

        return getMissingPropertyName(message)
            .map(
                name ->
                    HelpfulPlaceholder.withMessage("Undeclared property in XPath expression: $" + name)
                                      .withSuggestedAction("Add property", () -> addProperty(name))
            )
            .orElseGet(() -> HelpfulPlaceholder.withMessage(message))
            .withLeftColumn(new FontIcon("fas-exclamation-triangle"))
            .build();
    }


    private Optional<String> getMissingPropertyName(String errorMessage) {
        Pattern nameExtractor = XPathRuleQuery.XPATH_1_0.equals(getRuleBuilder().getXpathVersion())
                                ? JAXEN_MISSING_PROPERTY_EXTRACTOR
                                : SAXON_MISSING_PROPERTY_EXTRACTOR;

        Matcher matcher = nameExtractor.matcher(errorMessage);
        return matcher.matches() ? Optional.of(matcher.group(1)) : Optional.empty();
    }


    private class MyXpathSubscriber extends XPathUpdateSubscriber {


        MyXpathSubscriber(DesignerRoot root) {
            super(root);
        }

        @Override
        public void handleNoCompilationUnit() {
            updateResults(false, true, Collections.emptyList(), "Compilation unit is invalid");
        }

        @Override
        public void handleNoXPath() {
            updateResults(false, false, Collections.emptyList(), "Type an XPath expression to show results");

        }

        @Override
        public void handleXPathSuccess(List<Node> results) {
            updateResults(false, false, results, NO_MATCH_MESSAGE);
            // Notify that everything went OK so we can avoid logging very recent exceptions
            raiseParsableXPathFlag();
        }

        @Override
        public void handleXPathError(Exception e) {
            updateResults(true, false, Collections.emptyList(), sanitizeExceptionMessage(e));
            logUserException(e, Category.XPATH_EVALUATION_EXCEPTION);
        }
    }
}
