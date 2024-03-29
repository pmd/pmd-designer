/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model;


import org.reactfx.EventStream;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.properties.NumericConstraints;
import net.sourceforge.pmd.properties.PropertyBuilder;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import net.sourceforge.pmd.properties.internal.PropertyTypeId;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;


/**
 * Stores enough data to build a property descriptor, can be displayed within table views.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public class PropertyDescriptorSpec implements SettingsOwner {

    private static final String DEFAULT_STRING = "TODO";

    private final Val<Boolean> isNumerical;
    private final Val<Boolean> isMultivalue;

    private final Var<PropertyTypeId> typeId = Var.newSimpleVar(PropertyTypeId.STRING);
    private final Var<String> name = Var.newSimpleVar(DEFAULT_STRING);
    private final Var<String> value = Var.newSimpleVar(DEFAULT_STRING);
    private final Var<String> description = Var.newSimpleVar(DEFAULT_STRING);


    public PropertyDescriptorSpec() {
        isNumerical = typeId.map(this::isPropertyNumeric);
        isMultivalue = typeId.map(this::isPropertyMultivalue);
    }

    private Boolean isPropertyMultivalue(PropertyTypeId propertyTypeId) {
        switch (propertyTypeId) {
        case DOUBLE_LIST:
        case LONG_LIST:
        case STRING_LIST:
        case INTEGER_LIST:
        case CHARACTER_LIST:
            return true;
        default:
            return false;
        }
    }

    private Boolean isPropertyNumeric(PropertyTypeId propertyTypeId) {
        switch (propertyTypeId) {
        case LONG:
        case DOUBLE:
        case INTEGER:
        case LONG_LIST:
        case DOUBLE_LIST:
        case INTEGER_LIST:
            return true;
        default:
            return false;
        }
    }


    public Boolean getIsNumerical() {
        return isNumerical.getValue();
    }


    public Val<Boolean> isNumericalProperty() {
        return isNumerical;
    }


    public Boolean getIsMultivalue() {
        return isMultivalue.getValue();
    }


    public Val<Boolean> isMultivalueProperty() {
        return isMultivalue;
    }


    @PersistentProperty
    public String getDescription() {
        return description.getValue();
    }


    public void setDescription(String description) {
        this.description.setValue(description);
    }


    public Var<String> descriptionProperty() {
        return description;
    }


    @PersistentProperty
    public PropertyTypeId getTypeId() {
        return typeId.getValue();
    }


    public void setTypeId(PropertyTypeId typeId) {
        this.typeId.setValue(typeId);
    }


    public Var<PropertyTypeId> typeIdProperty() {
        return typeId;
    }


    @PersistentProperty
    public String getName() {
        return name.getValue();
    }


    public void setName(String name) {
        this.name.setValue(name);
    }


    public Var<String> nameProperty() {
        return name;
    }


    @PersistentProperty
    public String getValue() {
        return value.getValue();
    }


    public void setValue(String value) {
        this.value.setValue(value);
    }


    public Var<String> valueProperty() {
        return value;
    }


    /**
     * Returns an xml string of this property definition.
     *
     * @return An xml string
     */
    public String toXml() {
        return String.format("<property name=\"%s\" type=\"%s\" value=\"%s\" description=\"%s\"/>",
                             getName(), getTypeId().getStringId(), getValue(), getDescription());
    }


    @Override
    public String toString() {
        return toXml();
    }

    public PropertyDescriptorSpec deepCopy() {
        PropertyDescriptorSpec spec = new PropertyDescriptorSpec();
        spec.setName(getName());
        spec.setValue(getValue());
        spec.setDescription(getDescription());
        spec.setTypeId(getTypeId());
        return spec;
    }


    /**
     * Builds the descriptor. May throw IllegalArgumentException.
     *
     * @return the descriptor if it can be built
     */
    public PropertyDescriptor<?> build() {
        PropertyBuilder propertyBuilder = getTypeId().getBuilderUtils().newBuilder(getName());
        Object defaultValue = getTypeId().getBuilderUtils().getXmlMapper().fromString(getValue());
        propertyBuilder.desc(getDescription());
        propertyBuilder.defaultValue(defaultValue);
        if (isPropertyNumeric(getTypeId())) {
            propertyBuilder.require(NumericConstraints.inRange(-2_000_000, +2_000_000));
        }
        return propertyBuilder.build();
    }


    Object parseValue() {
        return getTypeId().getBuilderUtils().getXmlMapper().fromString(getValue());
    }


    /**
     * Removes bindings from this property spec.
     */
    public void unbind() {
        typeIdProperty().unbind();
        nameProperty().unbind();
        descriptionProperty().unbind();
        valueProperty().unbind();
    }

    /**
     * Pushes an event every time the rule owning this property needs to be re-evaluated.
     */
    public EventStream<?> modificationTicks() {
        return nameProperty().values()
                             .or(valueProperty().values())
                             .or(typeIdProperty().values());
    }

}
