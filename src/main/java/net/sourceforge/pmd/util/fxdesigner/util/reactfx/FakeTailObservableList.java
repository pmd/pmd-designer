/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.reactfx;

import org.reactfx.Subscription;
import org.reactfx.collection.LiveList;
import org.reactfx.collection.UnmodifiableByDefaultLiveList;

import javafx.collections.ObservableList;

/**
 * An observable list that has all the elements of the base list + 1
 * special element at the end.
 *
 * @param <E>
 */
public final class FakeTailObservableList<E> extends BaseObservableListDelegate<E> implements UnmodifiableByDefaultLiveList<E> {

    private final E tailElement;

    public FakeTailObservableList(ObservableList<E> base, E tailElement) {
        super(base);
        this.tailElement = tailElement;
    }

    @Override
    protected Subscription observeInputs() {
        return LiveList.observeQuasiChanges(base, this::notifyObservers);
    }

    @Override
    public int size() {
        return base.size() + 1;
    }

    @Override
    public E get(int index) {
        if (index < 0 || index > base.size()) {
            throw new IndexOutOfBoundsException("Index out of range: " + index);
        }

        return index < base.size() ? base.get(index) : tailElement;
    }

}
