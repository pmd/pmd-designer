/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.beans.converters;

import java.lang.reflect.Type;

public class PropertyValue extends TypedObject<Object> {

    private final String propertyName;
    private final String ownerName;

    public PropertyValue(String propertyName, String ownerName, Object object, Type type) {
        super(object, type);
        this.propertyName = propertyName;
        this.ownerName = ownerName;
    }


    @Override
    public String toString() {
        return "property " + propertyName + " of type " + getType() + " for " + ownerName;
    }
}
