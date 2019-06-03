/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.reactfx;

import org.reactfx.EventStream;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

public final class ObservablePair<K, V> {


    private final Var<K> first = Var.newSimpleVar(null);
    private final Var<V> second = Var.newSimpleVar(null);

    public ObservablePair(K k, V v) {
        first.setValue(k);
        second.setValue(v);
    }

    public ObservablePair(Val<K> k, V v) {
        first.bind(k);
        second.setValue(v);
    }

    public K getFirst() {
        return first.getValue();
    }

    public Var<K> firstProperty() {
        return first;
    }

    public void setFirst(K first) {
        this.first.setValue(first);
    }

    public V getSecond() {
        return second.getValue();
    }

    public Var<V> secondProperty() {
        return second;
    }

    public void setSecond(V second) {
        this.second.setValue(second);
    }

    public EventStream<?> modificationTicks() {
        return second.values().distinct().or(first.values().distinct());
    }
}
