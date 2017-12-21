/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.popups;

import static net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil.getSupportedLanguageVersions;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil.rewire;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil.stringConverter;

import java.net.URL;
import java.time.Duration;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.RulePriority;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.util.fxdesigner.model.ObservableXPathRuleBuilder;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.DiffMatchPatchWithHooks;
import net.sourceforge.pmd.util.fxdesigner.util.PropertyDescriptorSpec;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.SyntaxHighlightingCodeArea;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.XmlLiveTemplateHelper;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.XmlSyntaxHighlighter;
import net.sourceforge.pmd.util.fxdesigner.util.controls.PropertyTableView;

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
 * which is updated in real time with the values typed in the form. Currently, the area is initialised once, with a
 * template provided by {@link #getBaseRuleElement()}. Subsequent updates use a regex to find the text to replace,
 * and inserts a pre-styled text to avoid color twitching upon insertion.
 *
 * <p>The code area is not editable directly, to preserve the format on which the regexes depend.
 *
 * <p>This wizard is supposed to offer the most customization options about the user's rules. In terms of state, it's
 * wired to the XPathPanel's rule builder while it's open. That way the rule metadata can be saved.
 *
 * TODO:cf Export wizard future improvements
 * * Edition of optional attributes/elements -> find a way to add and remove parts of the template cleanly
 * * Find a more robust system than regex...
 * * Abstract this template, possibly to use it in other export wizards (eg export to test code)
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public class ExportXPathWizardController implements Initializable {

    // Replace patterns. The groups delimit the places which should be kept.
    private static final Pattern NAME_REPLACE_PATTERN = Pattern.compile("(name=\").*?(\")");
    private static final Pattern LANGUAGE_REPLACE_PATTERN = Pattern.compile("(language=\").*?(\")");
    private static final Pattern MESSAGE_REPLACE_PATTERN = Pattern.compile("(message=\").*?(\")");
    private static final Pattern URL_REPLACE_PATTERN = Pattern.compile("(externalInfoUrl=\").*?(\")");
    private static final Pattern DESCRIPTION_REPLACE_PATTERN = Pattern.compile("(<description>\n<!\\[CDATA\\[\n).*?(\n]]>\n {4}</description>)", Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern PRIORITY_REPLACE_PATTERN = Pattern.compile("(<priority>)[1-5](</priority>)");
    private static final Pattern XPATH_REPLACE_PATTERN
            = Pattern.compile("(<property name=\"xpath\">\n\\s*<value>\n<!\\[CDATA\\[\n).*?(\n]]>\n)", Pattern.DOTALL | Pattern.MULTILINE);

    private final Set<Subscription> activeSubs = new HashSet<>();
    private final DiffMatchPatchWithHooks dmp = new DiffMatchPatchWithHooks();

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
        Platform.runLater(() -> propertyView.setOnEditCommit(s -> differentialUpdate()));
    }


    public void bindToRuleBuilder(ObservableXPathRuleBuilder ruleBuilder) {
        // Rewire the rulebuilder to be updated by the ui, initialise the values of the ui
        rewire(ruleBuilder.nameProperty(), this.nameProperty(), this::setName);
        rewire(ruleBuilder.descriptionProperty(), this.descriptionProperty(), this::setDescription);
        rewire(ruleBuilder.languageProperty(), this.languageProperty(), this::setLanguage);
        rewire(ruleBuilder.messageProperty(), this.messageProperty(), this::setMessage);
        rewire(ruleBuilder.priorityProperty(), this.priorityProperty(), this::setPriority);
        rewire(ruleBuilder.rulePropertiesProperty(), this.rulePropertiesProperty(), this::setRuleProperties);
        rewire(ruleBuilder.xpathVersionProperty(), this.xpathVersionProperty(), this::setXpathVersion);
        rewire(ruleBuilder.xpathExpressionProperty(), this.xpathExpressionProperty(), this::setXpathExpression);

        addSubscription(changeSubscription(ruleBuilder.getRuleProperties()));

        initialiseTemplate();
    }


    private void initialiseTemplate() {
        XmlLiveTemplateHelper templateHelper = new XmlLiveTemplateHelper(exportResultArea);

        subscribe(templateHelper.replace(nameProperty(), NAME_REPLACE_PATTERN).asAttribute());
        subscribe(templateHelper.replace(messageProperty(), MESSAGE_REPLACE_PATTERN).asAttribute());
        subscribe(templateHelper.replace(languageProperty(), LANGUAGE_REPLACE_PATTERN).converter(Language::getTerseName).asAttribute());

        subscribe(templateHelper.replace(xpathExpressionProperty(), XPATH_REPLACE_PATTERN).asCdata());
        subscribe(templateHelper.replace(descriptionProperty(), DESCRIPTION_REPLACE_PATTERN).asCdata());
        subscribe(templateHelper.replace(priorityProperty(), PRIORITY_REPLACE_PATTERN).converter(p -> Integer.toString(p.getPriority())));

        initialiseAreaContextMenu();
        exportResultArea.setSyntaxHighlighter(new XmlSyntaxHighlighter());
        exportResultArea.setEditable(false);

        Platform.runLater(() -> exportResultArea.replaceText(getBaseRuleElement()));
    }


    private <T> Subscription changeSubscription(ObservableList<T> value) {
        return EventStreams.changesOf(value)
                           .successionEnds(Duration.ofMillis(100))
                           .subscribe(c -> differentialUpdate());
    }


    // update the text of the code area, changing only the lines that are different from the previous one
    private void differentialUpdate() {
        String up2Date = getBaseRuleElement();
        dmp.patchApply(dmp.patchMake(exportResultArea.getText(), up2Date),
                       exportResultArea.getText(),
                       exportResultArea::replaceText);
    }


    private void initialiseLanguageChoiceBox() {
        languageChoiceBox.getItems().addAll(getSupportedLanguageVersions().stream()
                                                                          .map(LanguageVersion::getLanguage)
                                                                          .distinct()
                                                                          .collect(Collectors.toList()));

        languageChoiceBox.setConverter(stringConverter(Language::getShortName, LanguageRegistry::findLanguageByShortName));
    }


    private void initialiseAreaContextMenu() {
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
                setName("");
                setDescription("");
                setMessage("");
                setPriority(RulePriority.MEDIUM);
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


    public void shutdown() {
        activeSubs.forEach(Subscription::unsubscribe);
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


    /** Shorthand to buildSettingsModel and subscription to the subscription. */
    private <T> void subscribe(XmlLiveTemplateHelper.ReplacementSubscriptionBuilder<T> sub) {
        activeSubs.add(sub.build());
    }


    /** Gets the template used to initialise the code area. */
    private String getBaseRuleElement() {
        // Remember to check the regexes when altering the template
        final String template = "<rule name=\"%s\"\n"
                + "      language=\"%s\"\n"
                + "      message=\"%s\"\n"
                + "      class=\"net.sourceforge.pmd.lang.rule.XPathRule\">\n"
                + "    <description>\n"
                + "<![CDATA[\n"
                + "%s\n"
                + "]]>\n"
                + "    </description>\n"
                + "    <priority>%d</priority>\n"
                + "    <properties>\n"
                + "%s" // other properties
                + "        <property name=\"version\" value=\"%s\"/>\n"
                + "        <property name=\"xpath\">\n"
                + "            <value>\n"
                + "<![CDATA[\n"
                + "%s\n"
                + "]]>\n"
                + "            </value>\n"
                + "        </property>\n"
                + "    </properties>\n"
                + "    <!--<example><![CDATA[]]></example>-->\n"
                + "</rule>";

        return String.format(template,
                             getName(),
                             getLanguage().getTerseName(),
                             getMessage(),
                             getDescription(),
                             getPriority().getPriority(),
                             getPropertiesString(),
                             getXpathVersion(),
                             getXpathExpression()
        );
    }


    private String getPropertiesString() {
        StringBuilder sb = new StringBuilder();
        for (PropertyDescriptorSpec spec : getRuleProperties()) {
            sb.append("        <property name=\"")
              .append(spec.getName())
              .append("\" type=\"")
              .append(spec.getTypeId().getStringId())
              .append("\" value=\"")
              .append(spec.getValue())
              .append("\" description=\"")
              .append(spec.getDescription())
              .append("\"/>\n");
        }
        return sb.toString();
    }


    /**
     * Register a subscription that this wizard will close upon termination.
     *
     * @param sub subscription
     */
    public void addSubscription(Subscription sub) {
        activeSubs.add(sub);
    }


    public String getName() {
        return nameField.getText();
    }


    public Val<String> nameProperty() {
        return Val.wrap(nameField.textProperty());
    }


    public void setName(String name) {
        nameField.setText(name);
    }


    public Val<String> messageProperty() {
        return Val.wrap(messageField.textProperty());
    }


    public String getMessage() {
        return messageField.getText();
    }


    public void setMessage(String message) {
        messageField.setText(message);
    }


    public String getDescription() {
        return descriptionArea.getText();
    }


    public Val<String> descriptionProperty() {
        return Val.wrap(descriptionArea.textProperty());
    }


    public void setDescription(String description) {
        descriptionArea.setText(description);
    }


    public Val<RulePriority> priorityProperty() {
        return priority;
    }


    public void setPriority(RulePriority rulePriority) {
        prioritySlider.setValue(rulePriority.getPriority());
    }


    public Val<String> xpathExpressionProperty() {
        return xpathExpression;
    }


    public String getXpathExpression() {
        return xpathExpression.getValue();
    }


    public void setXpathExpression(String xpath) {
        xpathExpression.setValue(xpath);
    }


    public String getXpathVersion() {
        return xpathVersion.getValue();
    }


    public Val<String> xpathVersionProperty() {
        return xpathVersion;
    }


    public void setXpathVersion(String version) {
        xpathVersion.setValue(version);
    }


    public Language getLanguage() {
        return language.getValue();
    }


    public RulePriority getPriority() {
        return priority.getValue();
    }


    public Val<Language> languageProperty() {
        return language;
    }


    public void setLanguage(Language language) {
        languageChoiceBox.getSelectionModel().select(language);
    }


    public ObservableList<PropertyDescriptorSpec> getRuleProperties() {
        return propertyView.getRuleProperties();
    }


    public void setRuleProperties(ObservableList<PropertyDescriptorSpec> ruleProperties) {
        propertyView.setRuleProperties(ruleProperties);
    }


    public Val<ObservableList<PropertyDescriptorSpec>> rulePropertiesProperty() {
        return Val.wrap(propertyView.rulePropertiesProperty());
    }
}
