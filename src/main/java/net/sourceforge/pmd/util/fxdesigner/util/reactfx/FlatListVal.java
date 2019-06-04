/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.reactfx;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.reactfx.Subscription;
import org.reactfx.collection.LiveList;
import org.reactfx.collection.LiveListBase;
import org.reactfx.collection.QuasiListModification;
import org.reactfx.value.Val;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 * Turns a {@code Val<LiveList<E>>} into a {@code LiveList<E>} that is
 * an empty, unmodifiable list when the base Val is null, and otherwise
 * delegates everything (including mutation methods) to the underlying value.
 *
 * <p>When the value changes a list change that deletes the list is emitted.
 *
 * @param <E>
 */
public class FlatListVal<E> extends LiveListBase<E> implements DefaultMutableLiveList<E> {

    private final Val<? extends ObservableList<E>> base;

    public FlatListVal(Val<? extends ObservableList<E>> base) {
        this.base = Val.orElseConst(base, FXCollections.emptyObservableList());
    }


    @Override
    protected Subscription observeInputs() {
        return ReactfxUtil.subscribeDisposable(
            base,
            lst ->
                LiveList.observeQuasiChanges(lst, this::notifyObservers)
                        .and(() -> this.notifyObservers(() -> Collections.singletonList(QuasiListModification.create(0, new ArrayList<>(lst), 0))))
        );
    }

    @Override
    public int size() {
        return base.getValue().size();
    }

    @Override
    public boolean add(E e) {
        return base.getValue().add(e);
    }

    @Override
    public boolean remove(Object o) {
        return base.getValue().remove(o);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return base.getValue().addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        return base.getValue().addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return base.getValue().removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return base.getValue().retainAll(c);
    }

    @Override
    public void clear() {
        base.getValue().clear();
    }

    @Override
    public E get(int index) {
        return base.getValue().get(index);
    }

    @Override
    public E set(int index, E element) {
        return base.getValue().set(index, element);
    }

    @Override
    public void add(int index, E element) {
        base.getValue().add(index, element);
    }

    @Override
    public E remove(int index) {
        return base.getValue().remove(index);
    }

    @Override
    public boolean setAll(Collection<? extends E> col) {
        return base.getValue().setAll(col);
    }

    @Override
    public void remove(int from, int to) {
        base.getValue().remove(from, to);
    }
}
