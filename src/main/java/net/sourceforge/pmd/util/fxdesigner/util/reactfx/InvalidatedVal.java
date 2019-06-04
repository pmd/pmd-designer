/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.reactfx;

import java.util.function.Function;

import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;
import org.reactfx.value.ValBase;

public class InvalidatedVal<T> extends ValBase<T> {


    private final Val<T> base;
    private final Function<T, EventStream<?>> otherTicks;

    public InvalidatedVal(Val<T> base, Function<T, EventStream<?>> otherTicks) {
        this.base = base;
        this.otherTicks = otherTicks;
    }


    @Override
    protected Subscription connect() {
        return ReactfxUtil.modificationTicks(
            LiveList.wrapVal(base),
            otherTicks
        ).subscribe(it -> invalidate());
    }

    @Override
    protected T computeValue() {
        return base.getValue();
    }
}
