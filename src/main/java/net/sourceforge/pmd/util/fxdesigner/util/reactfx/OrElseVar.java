/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.reactfx;

import org.reactfx.Subscription;
import org.reactfx.value.Val;
import org.reactfx.value.ValBase;
import org.reactfx.value.Var;

import javafx.beans.value.ObservableValue;

/**
 * A Var that when null, takes its default value from another Val.
 *
 * @param <T> Type of values
 */
public class OrElseVar<T> extends ValBase<T> implements Var<T> {

    private final Var<T> base = Var.newSimpleVar(null);
    private final Val<T> myOrElse;

    public OrElseVar(Val<? extends T> defaultValue) {
        myOrElse = Val.orElse(base, defaultValue);
    }


    @Override
    public void bind(ObservableValue<? extends T> observable) {
        base.bind(observable);
    }

    @Override
    public void unbind() {
        base.unbind();
    }

    @Override
    public boolean isBound() {
        return base.isBound();
    }

    @Override
    public void setValue(T value) {
        base.setValue(value);
    }

    @Override
    protected Subscription connect() {
        return myOrElse.observeInvalidations(it -> invalidate());
    }

    @Override
    protected T computeValue() {
        return myOrElse.getValue();
    }

}
