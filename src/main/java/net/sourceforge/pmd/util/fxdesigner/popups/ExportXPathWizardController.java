/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.popups;

import static com.github.oowekyala.rxstring.ItemRenderer.asString;
import static com.github.oowekyala.rxstring.ItemRenderer.indented;
import static com.github.oowekyala.rxstring.ItemRenderer.surrounded;
import static com.github.oowekyala.rxstring.ItemRenderer.wrapped;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil.rewireInit;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil.stringConverter;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringEscapeUtils;
import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.reactfx.Subscription;
import org.reactfx.value.Var;

import net.sourceforge.pmd.RulePriority;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.rule.xpath.XPathVersion;
import net.sourceforge.pmd.properties.PropertyTypeId;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.model.ObservableRuleBuilder;
import net.sourceforge.pmd.util.fxdesigner.model.ObservableXPathRuleBuilder;
import net.sourceforge.pmd.util.fxdesigner.model.PropertyDescriptorSpec;
import net.sourceforge.pmd.util.fxdesigner.util.AuxLanguageRegistry;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.StageBuilder;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.SyntaxHighlightingCodeArea;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.XmlSyntaxHighlighter;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ControlUtil;
import net.sourceforge.pmd.util.fxdesigner.util.controls.LanguageVersionRangeSlider;
import net.sourceforge.pmd.util.fxdesigner.util.controls.PropertyCollectionView;
import net.sourceforge.pmd.util.fxdesigner.util.controls.RulePrioritySlider;

import com.github.oowekyala.rxstring.LiveTemplate;
import com.github.oowekyala.rxstring.LiveTemplateBuilder;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


/**
 * Controller for the "Export XPath to rule" dialog. The wizard is split into two parts: a form, and a code area,
 * which is updated in real time with the values typed in the form.
 *
 * <p>This wizard is supposed to offer the most customization options about the user's rules. In terms of state, it's
 * wired to the XPathPanel's rule builder while it's open. That way the rule metadata can be persisted.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public final class ExportXPathWizardController implements Initializable {

    // those are not configurable in the form and so need their own property
    private final Var<String> xpathExpression = Var.newSimpleVar("");
    private final Var<XPathVersion> xpathVersion = Var.newSimpleVar(XPathVersion.DEFAULT);
    private final Stage myPopupStage;
    private final DesignerRoot root;
    @FXML
    private PropertyCollectionView propertyCollectionView;
    @FXML
    private Button resetMetadataButton;
    @FXML
    private Button copyResultButton;
    @FXML
    private SyntaxHighlightingCodeArea exportResultArea;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField messageField;
    @FXML
    private RulePrioritySlider rulePrioritySlider;
    @FXML
    private ChoiceBox<Language> languageChoiceBox;
    @FXML
    private TextField nameField;
    @FXML
    private Accordion infoAccordion;
    @FXML
    private LanguageVersionRangeSlider languageVersionRangeSlider;


    public ExportXPathWizardController(DesignerRoot root) {
        this.root = root;
        this.myPopupStage = createStage(root.getMainStage());
        propertyCollectionView.setOwnerStageFactory(myPopupStage);

    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initialiseLanguageChoiceBox();

        Platform.runLater(() -> { // Fixes blurry text in the description text area
            descriptionArea.setCache(false);
            ScrollPane sp = (ScrollPane) descriptionArea.getChildrenUnmodifiable().get(0);
            sp.setCache(false);
            for (Node n : sp.getChildrenUnmodifiable()) {
                n.setCache(false);
            }
        });

        // Expands required info pane
        Platform.runLater(() -> infoAccordion.setExpandedPane((TitledPane) infoAccordion.getChildrenUnmodifiable().get(0)));
        Platform.runLater(this::registerValidators);

        exportResultArea.setSyntaxHighlighter(new XmlSyntaxHighlighter());
        exportResultArea.setEditable(false);

        ControlUtil.copyToClipboardButton(copyResultButton, exportResultArea::getText);

        resetMetadataButton.setOnAction(e -> {
            Alert alert = new Alert(AlertType.CONFIRMATION, "Wipe out the rule's metadata?",
                                    ButtonType.YES, ButtonType.CANCEL);
            alert.showAndWait();

            exportResultArea.setSyntaxHighlighter(new XmlSyntaxHighlighter());
            if (ButtonType.YES.equals(alert.getResult())) {
                nameProperty().setValue("");
                descriptionProperty().setValue("");
                messageProperty().setValue("");
                priorityProperty().setValue(RulePriority.MEDIUM);
            }
        });

        languageVersionRangeSlider.currentLanguageProperty().bind(this.languageProperty());
        Platform.runLater(() -> exportResultArea.moveTo(0));
    }

    public Subscription bindToRuleBuilder(ObservableXPathRuleBuilder ruleBuilder) {
        this.xpathExpressionProperty().setValue(ruleBuilder.xpathExpressionProperty().getValue());
        this.xpathVersionProperty().setValue(ruleBuilder.xpathVersionProperty().getValue());

        return Subscription.multi(
            // Rewire the rulebuilder to be updated by the ui, initialise the values of the ui
            rewireInit(ruleBuilder.nameProperty(), this.nameProperty()),
            rewireInit(ruleBuilder.descriptionProperty(), this.descriptionProperty()),
            rewireInit(ruleBuilder.languageProperty(), this.languageProperty()),
            rewireInit(ruleBuilder.messageProperty(), this.messageProperty()),
            rewireInit(ruleBuilder.priorityProperty(), this.priorityProperty()),
            rewireInit(ruleBuilder.rulePropertiesProperty(), this.rulePropertiesProperty()),
            rewireInit(ruleBuilder.minimumVersionProperty(), this.languageVersionRangeSlider.minVersionProperty()),
            rewireInit(ruleBuilder.maximumVersionProperty(), this.languageVersionRangeSlider.maxVersionProperty()),
            // Initialise the live template
            liveTemplateBuilder().toTemplateSubscription(ruleBuilder, exportResultArea::replaceText)
        );
    }


    private void initialiseLanguageChoiceBox() {
        languageChoiceBox.getItems().setAll(AuxLanguageRegistry.getSupportedLanguages().collect(Collectors.toList()));

        languageChoiceBox.setConverter(stringConverter(Language::getShortName, AuxLanguageRegistry::findLanguageByShortName));
    }


    /** Form validation */
    private void registerValidators() {
        ValidationSupport validationSupport = new ValidationSupport();

        Validator<String> noWhitespaceName = Validator.createRegexValidator("Name cannot contain whitespace", "\\S*+", Severity.ERROR);
        Validator<String> emptyName = Validator.createEmptyValidator("Name required");

        validationSupport.registerValidator(nameField, Validator.combine(noWhitespaceName, emptyName));

        Validator<String> noWhitespaceMessage = Validator.createRegexValidator("Message cannot be whitespace", "(\\s*+\\S.*)?", Severity.ERROR);
        Validator<String> emptyMessage = Validator.createEmptyValidator("Message required");

        validationSupport.registerValidator(messageField, Validator.combine(noWhitespaceMessage, emptyMessage));
    }


    private Var<String> nameProperty() {
        return Var.fromVal(nameField.textProperty(), nameField::setText);
    }


    private Var<String> messageProperty() {
        return Var.fromVal(messageField.textProperty(), messageField::setText);
    }


    private Var<String> descriptionProperty() {
        return Var.fromVal(descriptionArea.textProperty(), descriptionArea::setText);
    }


    private Var<RulePriority> priorityProperty() {
        return rulePrioritySlider.priorityProperty();
    }


    private Var<String> xpathExpressionProperty() {
        return xpathExpression;
    }


    private Var<XPathVersion> xpathVersionProperty() {
        return xpathVersion;
    }


    public Var<Language> languageProperty() {
        return Var.fromVal(languageChoiceBox.getSelectionModel().selectedItemProperty(), languageChoiceBox.getSelectionModel()::select);
    }


    private Var<ObservableList<PropertyDescriptorSpec>> rulePropertiesProperty() {
        return Var.fromVal(propertyCollectionView.itemsProperty(), propertyCollectionView::setItems);
    }


    /** Set the given subscription as close handler and show. */
    public void showYourself(Subscription parentBinding) {
        myPopupStage.setOnCloseRequest(e -> parentBinding.unsubscribe());
        exportResultArea.setSyntaxHighlighter(new XmlSyntaxHighlighter());
        myPopupStage.show();
    }


    /** Builds the new stage, done in the constructor. */
    private Stage createStage(Stage mainStage) {
        return new StageBuilder().withOwner(mainStage)
                                 .withModality(Modality.WINDOW_MODAL)
                                 .withStyle(StageStyle.DECORATED)
                                 .withFxml(DesignerUtil.getFxml("xpath-export-wizard"), root, this)
                                 .withTitle("Export XPath expression to XML rule")
                                 .newStage();
    }

    private static String escapeMessageFormatter(String raw) {
        return raw.replace("\"", "''''");
    }


    /** Gets the template used to initialise the code area. */
    private static LiveTemplateBuilder<ObservableXPathRuleBuilder> liveTemplateBuilder() {
        return LiveTemplate.<ObservableXPathRuleBuilder>newBuilder()
            .withDefaultIndent("      ")
            .withDefaultEscape(StringEscapeUtils::escapeXml10)
            .append("<rule name=\"").bind(ObservableRuleBuilder::nameProperty).appendLine("\"")
            .appendIndent(1).append("language=\"").bind(ObservableRuleBuilder::languageProperty, Language::getTerseName).appendLine("\"")
            .bind(ObservableRuleBuilder::minimumVersionProperty, indented(2, surrounded("minimumLanguageVersion=\"", "\"\n", asString(LanguageVersion::getVersion))))
            .bind(ObservableRuleBuilder::maximumVersionProperty, indented(2, surrounded("maximumLanguageVersion=\"", "\"\n", asString(LanguageVersion::getVersion))))
            .withDefaultEscape(s -> s) // special escape for message
            .appendIndent(1).append("message=\"").bind(b -> b.messageProperty().map(ExportXPathWizardController::escapeMessageFormatter)).appendLine("\"")
            .withDefaultEscape(StringEscapeUtils::escapeXml10) // restore escaper
            .appendIndent(1).append("class=\"").bind(ObservableRuleBuilder::clazzProperty, Class::getCanonicalName).appendLine("\">")
            .withDefaultIndent("   ")
            .appendIndent(1).appendLine("<description>")
            .bind(ObservableRuleBuilder::descriptionProperty, wrapped(55, 2, true, asString())).endLine()
            .appendIndent(1).appendLine("</description>")
            .appendIndent(1).append("<priority>").bind(ObservableRuleBuilder::priorityProperty, p -> "" + p.getPriority()).appendLine("</priority>")
            .appendIndent(1).appendLine("<properties>")
            .bindTemplatedSeq(
                ObservableRuleBuilder::getRuleProperties,
                prop -> prop.appendIndent(2)
                            .append("<property name=\"").bind(PropertyDescriptorSpec::nameProperty)
                            .append("\" type=\"").bind(PropertyDescriptorSpec::typeIdProperty, PropertyTypeId::getStringId)
                            .append("\" value=\"").bind(PropertyDescriptorSpec::valueProperty)
                            .append("\" description=\"").bind(PropertyDescriptorSpec::descriptionProperty)
                            .appendLine("\"/>")
            )
            .appendIndent(2).append("<property name=\"version\" value=\"").bind(ObservableXPathRuleBuilder::xpathVersionProperty).appendLine("\"/>")
            .appendIndent(2).appendLine("<property name=\"xpath\">")
            .appendIndent(3).appendLine("<value>")
            .appendLine("<![CDATA[")
            .withDefaultEscape(s -> s) // stop escaping
            .bind(ObservableXPathRuleBuilder::xpathExpressionProperty).endLine()
            .appendLine("]]>")
            .appendIndent(3).appendLine("</value>")
            .appendIndent(2).appendLine("</property>")
            .appendIndent(1).appendLine("</properties>")
            .appendLine("</rule>");
    }
}
