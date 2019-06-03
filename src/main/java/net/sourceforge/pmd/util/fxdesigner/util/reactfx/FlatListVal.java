/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.reactfx;

import java.util.Collections;
import java.util.List;

import org.reactfx.Subscription;
import org.reactfx.collection.LiveList;
import org.reactfx.collection.LiveListBase;
import org.reactfx.collection.QuasiListModification;
import org.reactfx.collection.UnmodifiableByDefaultLiveList;
import org.reactfx.value.Val;

import javafx.collections.ObservableList;

public class FlatListVal<E> extends LiveListBase<E> implements UnmodifiableByDefaultLiveList<E> {

    private final Val<? extends ObservableList<? extends E>> base;

    public FlatListVal(Val<? extends ObservableList<? extends E>> base) {
        this.base = base;
    }


    @Override
    protected Subscription observeInputs() {
        return ReactfxUtil.subscribeDisposable(
            base,
            lst ->
                LiveList.<E>observeQuasiChanges(lst, this::notifyObservers)
                    .and(() -> this.notifyObservers(() -> Collections.singletonList(QuasiListModification.create(0, lst, 0))))
        );
    }

    @Override
    public int size() {
        return base.map(List::size).getOrElse(0);
    }

    @Override
    public E get(int index) {
        return base.getOpt().map(it -> it.get(index)).orElseGet(() -> {
            throw new IndexOutOfBoundsException("Empty list");
        });
    }
}
