/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.beans.converters;

import java.lang.reflect.Type;
import java.util.Objects;

import org.apache.commons.lang3.reflect.Typed;

public class TypedObject<T> implements Typed<T> {

    private final T object;
    private final Type type;

    public TypedObject(T object, Type type) {
        this.object = object;
        this.type = type;
    }


    @Override
    public Type getType() {
        return type;
    }

    public T getObject() {
        return object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TypedObject<?> that = (TypedObject<?>) o;
        return Objects.equals(object, that.object)
            && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object, type);
    }

    @Override
    public String toString() {
        return "object of type " + type + " : " + object;
    }
}
