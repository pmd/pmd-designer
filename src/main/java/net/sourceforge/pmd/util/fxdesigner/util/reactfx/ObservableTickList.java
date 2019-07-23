/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.reactfx;

import java.util.Collections;
import java.util.function.Function;

import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.collection.LiveList;
import org.reactfx.collection.UnmodifiableByDefaultLiveList;

import com.github.oowekyala.rxstring.ReactfxExtensions;
import javafx.collections.ObservableList;

/**
 * An observable list that fires a change tick given a function that
 * produces a stream of modifications.
 *
 * @param <E>
 */
public final class ObservableTickList<E> extends BaseObservableListDelegate<E> implements UnmodifiableByDefaultLiveList<E> {


    private final Function<? super E, ? extends EventStream<?>> ticks;

    public ObservableTickList(ObservableList<E> base, Function<? super E, ? extends EventStream<?>> ticks) {
        super(base);
        this.ticks = ticks;
    }

    @Override
    protected Subscription observeInputs() {
        return ReactfxExtensions.dynamic(base, (e, i) -> ticks.apply(e).subscribe(k -> this.notifyObservers(Collections::emptyList)))
                                .and(LiveList.observeQuasiChanges(base, this::notifyObservers));
    }

}
