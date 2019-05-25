/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.reactfx;

import java.util.Collection;

import org.reactfx.collection.LiveListBase;

import javafx.collections.ObservableList;

/**
 * An observable list that has all the elements of the base list + 1
 * special element at the end.
 *
 * @param <E>
 */
public abstract class BaseObservableListDelegate<E> extends LiveListBase<E> {


    protected final ObservableList<E> base;

    public BaseObservableListDelegate(ObservableList<E> base) {
        this.base = base;
    }


    @Override
    public int size() {
        return base.size();
    }

    @Override
    public boolean add(E e) {
        return base.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return base.remove(o);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return base.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        return base.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return base.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return base.retainAll(c);
    }

    @Override
    public void clear() {
        base.clear();
    }

    @Override
    public E get(int index) {
        return base.get(index);
    }

    @Override
    public E set(int index, E element) {
        return base.set(index, element);
    }

    @Override
    public void add(int index, E element) {
        base.add(index, element);
    }

    @Override
    public E remove(int index) {
        return base.remove(index);
    }

    @Override
    public boolean addAll(E... elements) {
        return base.addAll(elements);
    }

    @Override
    public boolean setAll(E... elements) {
        return base.setAll(elements);
    }

    @Override
    public boolean setAll(Collection<? extends E> col) {
        return base.setAll(col);
    }

    @Override
    public boolean removeAll(E... elements) {
        return base.removeAll(elements);
    }

    @Override
    public boolean retainAll(E... elements) {
        return base.retainAll(elements);
    }

    @Override
    public void remove(int from, int to) {
        base.remove(from, to);
    }
}
