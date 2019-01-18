/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.popups;

import static net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil.getSupportedLanguageVersions;
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
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.RulePriority;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.properties.PropertyTypeId;
import net.sourceforge.pmd.util.fxdesigner.model.ObservableRuleBuilder;
import net.sourceforge.pmd.util.fxdesigner.model.ObservableXPathRuleBuilder;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.PropertyDescriptorSpec;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.SyntaxHighlightingCodeArea;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.XmlSyntaxHighlighter;
import net.sourceforge.pmd.util.fxdesigner.util.controls.PropertyTableView;

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
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;


/**
 * Controller for the "Export XPath to rule" dialog. The wizard is split into two parts: a form, and a code area,
 * which is updated in real time with the values typed in the form.
 *
 * <p>This wizard is supposed to offer the most customization options about the user's rules. In terms of state, it's
 * wired to the XPathPanel's rule builder while it's open. That way the rule metadata can be saved.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public class ExportXPathWizardController implements Initializable {


    private final Var<String> xpathExpression = Var.newSimpleVar("");
    private final Var<String> xpathVersion = Var.newSimpleVar(DesignerUtil.defaultXPathVersion());
    private Val<Language> language = Val.wrap(null);
    private Val<RulePriority> priority = Val.wrap(null);
    @FXML
    private SyntaxHighlightingCodeArea exportResultArea;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField messageField;
    @FXML
    private Slider prioritySlider;
    @FXML
    private ChoiceBox<Language> languageChoiceBox;
    @FXML
    private TextField nameField;
    @FXML
    private Accordion infoAccordion;
    @FXML
    private PropertyTableView propertyView;


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initialiseLanguageChoiceBox();

        language = Val.wrap(languageChoiceBox.getSelectionModel().selectedItemProperty());
        priority = Val.map(prioritySlider.valueProperty(), Number::intValue).map(RulePriority::valueOf);

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

    }


    public Subscription bindToRuleBuilder(ObservableXPathRuleBuilder ruleBuilder) {
        // Rewire the rulebuilder to be updated by the ui, initialise the values of the ui
        rewireInit(ruleBuilder.nameProperty(), this.nameProperty());
        rewireInit(ruleBuilder.descriptionProperty(), this.descriptionProperty());
        rewireInit(ruleBuilder.languageProperty(), this.languageProperty());
        rewireInit(ruleBuilder.messageProperty(), this.messageProperty());
        rewireInit(ruleBuilder.priorityProperty(), this.priorityProperty());
        rewireInit(ruleBuilder.rulePropertiesProperty(), this.rulePropertiesProperty());
        rewireInit(ruleBuilder.xpathVersionProperty(), this.xpathVersionProperty());
        rewireInit(ruleBuilder.xpathExpressionProperty(), this.xpathExpressionProperty());

        initialiseAreaContextMenu();
        exportResultArea.setSyntaxHighlighter(new XmlSyntaxHighlighter());
        exportResultArea.setEditable(false);

        return liveTemplateBuilder().toTemplateSubscription(ruleBuilder, exportResultArea::replaceText);
    }


    private void initialiseLanguageChoiceBox() {
        languageChoiceBox.getItems().addAll(getSupportedLanguageVersions().stream()
                                                                          .map(LanguageVersion::getLanguage)
                                                                          .distinct()
                                                                          .collect(Collectors.toList()));

        languageChoiceBox.setConverter(stringConverter(Language::getShortName, LanguageRegistry::findLanguageByShortName));
    }


    private void initialiseAreaContextMenu() {

        // TODO move to a ToolBar

        MenuItem copyToClipBoardItem = new MenuItem("Copy to clipboard");
        copyToClipBoardItem.setOnAction(e -> {
            final ClipboardContent content = new ClipboardContent();
            content.putString(exportResultArea.getText());
            Clipboard.getSystemClipboard().setContent(content);
        });

        MenuItem resetMetadataItem = new MenuItem("Reset rule metadata");
        resetMetadataItem.setOnAction(e -> {
            Alert alert = new Alert(AlertType.CONFIRMATION, "Wipe out the rule's metadata?",
                                    ButtonType.YES, ButtonType.CANCEL);
            alert.showAndWait();

            exportResultArea.setSyntaxHighlighter(new XmlSyntaxHighlighter());
            if (alert.getResult() == ButtonType.YES) {
                nameProperty().setValue("");
                descriptionProperty().setValue("");
                messageProperty().setValue("");
                priorityProperty().setValue(RulePriority.MEDIUM);
            }
        });

        final ContextMenu contextMenu = new ContextMenu(copyToClipBoardItem, new SeparatorMenuItem(), resetMetadataItem);
        exportResultArea.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(
                    exportResultArea,
                    event.getScreenX(),
                    event.getScreenY()
                );
            } else if (event.getButton() == MouseButton.PRIMARY) {
                contextMenu.hide();
            }
        });
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


    public Var<String> nameProperty() {
        return Var.fromVal(nameField.textProperty(), nameField::setText);
    }


    public Var<String> messageProperty() {
        return Var.fromVal(messageField.textProperty(), messageField::setText);
    }


    public Var<String> descriptionProperty() {
        return Var.fromVal(descriptionArea.textProperty(), descriptionArea::setText);
    }


    public Var<RulePriority> priorityProperty() {
        return Var.doubleVar(prioritySlider.valueProperty()).mapBidirectional(d -> RulePriority.valueOf(d.intValue()), p -> Double.valueOf(p.getPriority()));
    }


    public Var<String> xpathExpressionProperty() {
        return xpathExpression;
    }


    public Var<String> xpathVersionProperty() {
        return xpathVersion;
    }


    public Language getLanguage() {
        return language.getValue();
    }


    public RulePriority getPriority() {
        return priority.getValue();
    }


    public Var<Language> languageProperty() {
        return Var.fromVal(language, languageChoiceBox.getSelectionModel()::select);
    }


    public Var<ObservableList<PropertyDescriptorSpec>> rulePropertiesProperty() {
        return Var.fromVal(propertyView.rulePropertiesProperty(), propertyView::setRuleProperties);
    }


    /** Gets the template used to initialise the code area. */
    private static LiveTemplateBuilder<ObservableXPathRuleBuilder> liveTemplateBuilder() {
        return LiveTemplate.<ObservableXPathRuleBuilder>builder()
            .withDefaultIndent("      ")
            .withDefaultEscape(StringEscapeUtils::escapeXml10)
            .append("<rule name=\"").bind(ObservableRuleBuilder::nameProperty).appendLine("\"")
            .appendIndent(1).append("language=\"").bind(ObservableRuleBuilder::languageProperty, Language::getTerseName).appendLine("\"")
            .appendIndent(1).append("message=\"").bind(ObservableRuleBuilder::messageProperty).appendLine("\"")
            .appendIndent(1).append("class=\"").bind(ObservableRuleBuilder::clazzProperty, Class::getCanonicalName).appendLine("\">")
            .withDefaultIndent("   ")
            .appendIndent(1).appendLine("<description>")
            .bind(ObservableRuleBuilder::descriptionProperty).endLine()
            .appendIndent(1).appendLine("</description>")
            .appendIndent(1).append("<priority>").bind(ObservableRuleBuilder::priorityProperty, p -> "" + p.getPriority()).appendLine("</priority>")
            .appendIndent(1).appendLine("<properties>")
            .bindTemplatedSeq(ObservableRuleBuilder::getRuleProperties,
                              prop -> prop.appendIndent(2)
                                          .append("<property name=\"").bind(PropertyDescriptorSpec::nameProperty)
                                          .append("\" type=\"").bind(PropertyDescriptorSpec::typeIdProperty, PropertyTypeId::getStringId)
                                          .append("\" value=\"").bind(PropertyDescriptorSpec::valueProperty)
                                          .appendLine("\"/>"))
            .appendIndent(2).append("<property name=\"version\" value=\"").bind(ObservableXPathRuleBuilder::xpathVersionProperty).appendLine("\"/>")
            .appendIndent(2).appendLine("<property name=\"xpath\">")
            .appendIndent(3).appendLine("<value>")
            .appendLine("<![CDATA[")
            .bind(ObservableXPathRuleBuilder::xpathExpressionProperty).endLine()
            .appendLine("]]>")
            .appendIndent(3).appendLine("</value>")
            .appendIndent(2).appendLine("</property>")
            .appendIndent(1).appendLine("</properties>")
            .appendLine("</rule>");
    }
}
