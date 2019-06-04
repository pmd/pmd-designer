/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.reactfx;

import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.function.Function;

import org.reactfx.Subscription;
import org.reactfx.collection.LiveList;
import org.reactfx.collection.LiveListBase;

import javafx.collections.ObservableList;

public class MutableMappedList<I, O> extends LiveListBase<O> implements DefaultMutableLiveList<O> {

    private final ObservableList<I> base;
    private final Function<I, O> forward;
    private final Function<O, I> backward;

    public MutableMappedList(ObservableList<I> base,
                             Function<I, O> forward,
                             Function<O, I> backward) {
        this.base = base;
        this.forward = forward;
        this.backward = backward;
    }

    @Override
    protected Subscription observeInputs() {
        return LiveList.map(base, forward).observeQuasiChanges(this::notifyObservers);
    }

    @Override
    public boolean setAll(Collection<? extends O> col) {
        base.clear();

        for (O o : col) {
            base.add(backward.apply(o));
        }
        return true;
    }

    @Override
    public void remove(int from, int to) {
        base.remove(from, to);
    }

    @Override
    public int size() {
        return base.size();
    }

    @Override
    public boolean add(O o) {
        return base.add(backward.apply(o));
    }

    @Override
    public boolean remove(Object o) {
        return base.remove(backward.apply((O) o));
    }

    @Override
    public boolean addAll(Collection<? extends O> c) {
        return base.addAll(c.stream().map(backward).collect(toList()));
    }

    @Override
    public boolean addAll(int index, Collection<? extends O> c) {
        return base.addAll(index, c.stream().map(backward).collect(toList()));
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return base.removeAll(c.stream().map(it -> (O) it).map(backward).collect(toList()));
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return base.retainAll(c.stream().map(it -> (O) it).map(backward).collect(toList()));
    }

    @Override
    public void clear() {
        base.clear();
    }

    @Override
    public O get(int index) {
        return forward.apply(base.get(index));
    }

    @Override
    public O set(int index, O element) {
        return forward.apply(base.set(index, backward.apply(element)));
    }

    @Override
    public void add(int index, O element) {
        base.add(index, backward.apply(element));
    }

    @Override
    public O remove(int index) {
        return forward.apply(base.remove(index));
    }
}
