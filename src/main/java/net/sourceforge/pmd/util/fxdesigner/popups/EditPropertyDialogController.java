/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.popups;

import static net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil.rewireInit;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.reactfx.Subscription;
import org.reactfx.util.Try;
import org.reactfx.value.Var;

import net.sourceforge.pmd.properties.PropertySerializer;
import net.sourceforge.pmd.properties.internal.PropertyTypeId;
import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.model.PropertyDescriptorSpec;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.controls.PropertyCollectionView;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextField;


/**
 * Property edition dialog. Use {@link #bindToDescriptor(PropertyDescriptorSpec, ObservableList)} )}
 * to use this dialog to edit a descriptor spec. Typically owned by a {@link PropertyCollectionView}.
 * The controller must be instantiated by hand.
 *
 * @author Clément Fournier
 * @see PropertyDescriptorSpec
 * @since 6.0.0
 */
public class EditPropertyDialogController implements Initializable, ApplicationComponent {

    private final Var<PropertyTypeId> typeId = Var.newSimpleVar(PropertyTypeId.STRING);
    private final Var<PropertyDescriptorSpec> backingDescriptor = Var.newSimpleVar(null);
    private final Var<ObservableList<PropertyDescriptorSpec>> backingDescriptorList = Var.newSimpleVar(null);

    private final ValidationSupport validationSupport = new ValidationSupport();
    private final DesignerRoot root;
    @FXML
    private TextField nameField;
    @FXML
    private TextField descriptionField;
    @FXML
    private ChoiceBox<PropertyTypeId> typeChoiceBox;
    @FXML
    private TextField valueField;

    public EditPropertyDialogController() {
        // default constructor
        this.root = null;
    }


    public EditPropertyDialogController(DesignerRoot root) {
        this.root = root;
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        Platform.runLater(() -> {
            typeId.bind(typeChoiceBox.getSelectionModel().selectedItemProperty());
            typeChoiceBox.setConverter(DesignerUtil.stringConverter(PropertyTypeId::getStringId,
                                                                    PropertyTypeId::lookupMnemonic));
            typeChoiceBox.getItems().addAll(PropertyTypeId.typeIdsToConstants().values());
            FXCollections.sort(typeChoiceBox.getItems());
        });

        Platform.runLater(this::registerBasicValidators);

        typeIdProperty().values()
                        .filter(Objects::nonNull)
                        .subscribe(this::registerTypeDependentValidators);

    }


    /**
     * Wires this dialog to the descriptor, so that the controls edit the descriptor.
     *
     * @param spec The descriptor
     */
    public Subscription bindToDescriptor(PropertyDescriptorSpec spec, ObservableList<PropertyDescriptorSpec> allDescriptors) {

        backingDescriptor.setValue(spec);
        backingDescriptorList.setValue(allDescriptors);
        return Subscription.multi(
            rewireInit(spec.nameProperty(), this.nameProperty(), this::setName),
            rewireInit(spec.typeIdProperty(), this.typeIdProperty(), this::setTypeId),
            rewireInit(spec.valueProperty(), this.valueProperty(), this::setValue),
            rewireInit(spec.descriptionProperty(), this.descriptionProperty(), this::setDescription)
        );
    }


    // Validators for attributes common to all properties
    private void registerBasicValidators() {
        Validator<String> noWhitespaceName
            = Validator.createRegexValidator("Name cannot contain whitespace", "\\S*+", Severity.ERROR);
        Validator<String> emptyName = Validator.createEmptyValidator("Name required");
        Validator<String> uniqueName = (c, val) -> {
            long sameNameDescriptors = backingDescriptorList.getOrElse(FXCollections.emptyObservableList())
                                                            .stream()
                                                            .map(PropertyDescriptorSpec::getName)
                                                            .filter(getName()::equals)
                                                            .count();

            return new ValidationResult().addErrorIf(c, "The name must be unique", sameNameDescriptors > 1);
        };

        validationSupport.registerValidator(nameField, Validator.combine(noWhitespaceName, emptyName, uniqueName));

        Validator<String> noWhitespaceDescription
                = Validator.createRegexValidator("Message cannot be whitespace", "(\\s*+\\S.*)?", Severity.ERROR);
        Validator<String> emptyDescription = Validator.createEmptyValidator("Message required");
        validationSupport.registerValidator(descriptionField, Validator.combine(noWhitespaceDescription, emptyDescription));
    }


    private void registerTypeDependentValidators(PropertyTypeId typeId) {
        Validator<String> valueValidator = (c, val) ->
                ValidationResult.fromErrorIf(valueField, "The value couldn't be parsed",
                                             Try.tryGet(() -> getValueParser(typeId).fromString(getValue())).isFailure());


        validationSupport.registerValidator(valueField, valueValidator);
    }


    private PropertySerializer<?> getValueParser(PropertyTypeId typeId) {
        return typeId.getBuilderUtils().getXmlMapper();
    }


    public String getName() {
        return nameField.getText();
    }


    public void setName(String name) {
        nameField.setText(name);
    }


    public Property<String> nameProperty() {
        return nameField.textProperty();
    }


    public String getDescription() {
        return descriptionField.getText();
    }


    public void setDescription(String description) {
        descriptionField.setText(description);
    }


    public Property<String> descriptionProperty() {
        return descriptionField.textProperty();
    }


    public PropertyTypeId getTypeId() {
        return typeId.getValue();
    }


    public void setTypeId(PropertyTypeId typeId) {
        typeChoiceBox.getSelectionModel().select(typeId);
    }


    public Var<PropertyTypeId> typeIdProperty() {
        return typeId;
    }


    public String getValue() {
        return valueField.getText();
    }


    public void setValue(String value) {
        valueField.setText(value);
    }


    public Property<String> valueProperty() {
        return valueField.textProperty();
    }

    @Override
    public DesignerRoot getDesignerRoot() {
        return root;
    }
}
