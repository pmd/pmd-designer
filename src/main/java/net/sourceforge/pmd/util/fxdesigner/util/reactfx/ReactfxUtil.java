/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.reactfx;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactfx.EventSource;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;
import org.reactfx.value.ValBase;
import org.reactfx.value.Var;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

/**
 * Extensions to ReactFX Val and EventStreams. Some can be deemed as too
 * general for this project: I'd like them to be moved to the reactfx main
 * project -> but it's unmaintained.
 *
 * @author Clément Fournier
 */
public final class ReactfxUtil {

    private ReactfxUtil() {

    }

    /**
     * Converts an event stream to a val, that always holds the latest
     * emitted value of the stream.
     */
    public static <T> Val<T> latestValue(EventStream<T> values) {
        return new ValBase<T>() {
            private T currentVal;

            @Override
            protected Subscription connect() {
                return values.subscribe(t -> {
                    currentVal = t;
                    invalidate();
                });
            }

            @Override
            protected T computeValue() {
                return currentVal;
            }
        };
    }

    /**
     * Returns a val that reflects "true" values of the input val only after the [vetoPeriod], and
     * only if they're not vetoed by a "false" value emitted during the veto period. "false" values
     * are reflected immediately.
     */
    public static Val<Boolean> vetoableYes(Val<Boolean> base, Duration vetoPeriod) {
        return latestValue(VetoableEventStream.vetoableYes(base.values(), vetoPeriod)).orElseConst(false);
    }

    /** Like the other overload, using the setter of the ui property. */
    public static <T> void rewireInit(Property<T> underlying, Property<T> ui) {
        rewireInit(underlying, ui, ui::setValue);
    }

    /**
     * Binds the underlying property to a source of values (UI property). The UI
     * property is also initialised using a setter.
     *
     * @param underlying The underlying property
     * @param ui         The property exposed to the user (the one in this wizard)
     * @param setter     Setter to initialise the UI value
     * @param <T>        Type of values
     */
    public static <T> void rewireInit(Property<T> underlying, ObservableValue<? extends T> ui, Consumer<? super T> setter) {
        setter.accept(underlying.getValue());
        rewire(underlying, ui);
    }

    /** Like rewireInit, with no initialisation. */
    public static <T> void rewire(Property<T> underlying, ObservableValue<? extends T> source) {
        underlying.unbind();
        underlying.bind(source); // Bindings are garbage collected after the popup dies
    }

    public static Var<Boolean> booleanVar(BooleanProperty p) {
        return Var.mapBidirectional(p, Boolean::booleanValue, Function.identity());
    }

    // Creating a real function Val<LiveList<T>> => LiveList<T> or LiveList<Val<T>> => LiveList<T> would
    // allow implementing LiveList.flatMap, which is a long-standing feature request in ReactFX
    // These utilities are very inefficient, but sufficient for our use case...
    public static <T> Val<LiveList<T>> flatMapChanges(ObservableList<? extends ObservableValue<T>> listOfObservables) {

        // every time an element changes an invalidation stream
        EventStream<?> invalidations =
            LiveList.map(listOfObservables, EventStreams::valuesOf)
                    .reduce(EventStreams::merge)
                    .values()
                    .filter(Objects::nonNull)
                    .flatMap(Function.identity());

        return Val.create(() -> LiveList.map(listOfObservables, ObservableValue::getValue), invalidations);
    }

    public static <T, U> Val<U> reduceWElts(ObservableList<? extends ObservableValue<T>> list, U zero, BiFunction<U, T, U> mapper) {
        return flatMapChanges(list).map(l -> l.stream().reduce(zero, mapper, (u, v) -> v));
    }

    public static <T> Val<Integer> countMatching(ObservableList<? extends ObservableValue<T>> list, Predicate<? super T> predicate) {
        return reduceWElts(list, 0, (cur, t) -> predicate.test(t) ? cur + 1 : cur);
    }

    public static Val<Integer> countNotMatching(ObservableList<? extends ObservableValue<Boolean>> list) {
        return countMatching(list, b -> !b);
    }

    /**
     * Like reduce if possible, but can be used if the events to reduce are emitted in extremely close
     * succession, so close that some unrelated events may be mixed up. This reduces each new event
     * with a related event in the pending notification chain instead of just considering the last one
     * as a possible reduction target.
     */
    public static <T> EventStream<T> reduceEntangledIfPossible(EventStream<T> input, BiPredicate<T, T> canReduce, BinaryOperator<T> reduction, Duration duration) {
        EventSource<T> source = new EventSource<>();


        input.reduceSuccessions(
            () -> new ArrayList<>(),
            (List<T> pending, T t) -> {

                for (int i = 0; i < pending.size(); i++) {
                    if (canReduce.test(pending.get(i), t)) {
                        pending.set(i, reduction.apply(pending.get(i), t));
                        return pending;
                    }
                }
                pending.add(t);

                return pending;
            },
            duration
        )
             .subscribe(pending -> {
                 for (T t : pending) {
                     source.push(t);
                 }
             });

        return source;
    }
}
