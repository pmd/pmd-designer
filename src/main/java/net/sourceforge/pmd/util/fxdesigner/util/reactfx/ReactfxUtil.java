/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.reactfx;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactfx.EventSource;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.collection.LiveList;
import org.reactfx.util.FxTimer;
import org.reactfx.util.Timer;
import org.reactfx.value.Val;
import org.reactfx.value.ValBase;
import org.reactfx.value.Var;

import com.github.oowekyala.rxstring.ReactfxExtensions;
import com.github.oowekyala.rxstring.ReactfxExtensions.RebindSubscription;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;

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


    private static final RebindSubscription<?> EMPTY_SUB = new RebindSubscription<Object>() {
        @Override
        public RebindSubscription<Object> rebind(Object newItem) {
            return emptySub();
        }

        @Override
        public void unsubscribe() {
            // do nothing
        }
    };

    public static <T> RebindSubscription<T> emptySub() {
        return (RebindSubscription<T>) EMPTY_SUB;
    }


    public static <I, O> RebindSubscription<O> map(RebindSubscription<I> base, Function<O, I> f) {
        return RebindSubscription.make(base, o -> map(base.rebind(f.apply(o)), f));
    }

    public static Val<Boolean> isPresentProperty(Val<?> v) {
        return v.map(it -> true).orElseConst(false);
    }

    /**
     * Add a hook on the owner window. It's not possible to do this statically,
     * since at construction time the window might not be set.
     */
    public static <T> Subscription subscribeDisposable(ObservableValue<@Nullable ? extends T> node,
                                                       Function<@NonNull ? super T, Subscription> subscriber) {
        return ReactfxExtensions.dynamic(
            LiveList.wrapVal(node),
            (w, i) -> subscriber.apply(w)
        );
    }

    public static <E> EventStream<?> modificationTicks(ObservableList<? extends E> list, Function<? super E, ? extends EventStream<?>> tickProvider) {
        return new ObservableTickList<>(list, tickProvider).quasiChanges();
    }

    public static <T> Subscription subscribeDisposable(EventStream<T> stream, Function<T, Subscription> subscriber) {
        return subscribeDisposable(latestValue(stream), subscriber);
    }

    public static <T> Var<T> defaultedVar(Val<? extends T> defaultValue) {
        return new OrElseVar<>(defaultValue);
    }

    public static <T, S> LiveList<S> mapBothWays(ObservableList<T> base, Function<T, S> forward, Function<S, T> backward) {
        return new MutableMappedList<>(base, forward, backward);
    }

    //    public static <T extends Event> Subscription addEventHandler(Consumer<EventHandler<T>> addMethod, Consumer<EventHandler<T>> removeMethod,)

    public static <T extends Event> Subscription addEventHandler(Property<EventHandler<T>> addMethod, EventHandler<T> handler) {
        addMethod.setValue(handler);
        return () -> addMethod.setValue(null);
    }

    public static <T extends Event> Subscription addEventHandler(Node node, EventType<T> type, EventHandler<T> handler) {
        node.addEventHandler(type, handler);
        return () -> node.removeEventHandler(type, handler);
    }

    static Function<Runnable, Timer> defaultTimerFactory(Duration duration) {
        return action -> FxTimer.create(duration, action);
    }


    public static <I> EventStream<I> distinctBetween(EventStream<I> input, Duration duration) {
        return DistinctBetweenStream.distinctBetween(input, ReactfxUtil.defaultTimerFactory(duration));
    }

    public static <K, V> Val<Map<K, LiveList<V>>> groupBy(ObservableList<? extends V> base, Function<? super V, ? extends K> selector) {
        return new GroupByLiveList<>(base, selector);
    }

    public static <E> LiveList<E> flattenList(Val<? extends ObservableList<E>> base) {
        return new FlatListVal<>(base);
    }

    public static <K, V> Val<Map<K, V>> observableMapVal(ObservableMap<K, V> map) {
        return new ValBase<Map<K, V>>() {

            @Override
            protected Subscription connect() {
                MapChangeListener<K, V> listener = ch -> notifyObservers(new HashMap<>(map));
                map.addListener(listener);
                return () -> map.removeListener(listener);
            }

            @Override
            protected Map<K, V> computeValue() {
                return new HashMap<>(map);
            }
        };
    }

    public static <T> Val<T> withInvalidations(Val<T> base, Function<T, EventStream<?>> otherInvalidations) {
        return new InvalidatedVal<>(base, otherInvalidations);
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

    public static <T> Val<T> vetoableNull(Val<T> base, Duration duration) {
        return latestValue(VetoableEventStream.vetoableNull(base.values(), duration));
    }

    public static Var<Boolean> booleanVar(BooleanProperty p) {
        return Var.mapBidirectional(p, Boolean::booleanValue, Function.identity());
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
                source.push(t);
                return pending;
            },
            duration
        )
             .subscribe(pending -> {
                 for (T t : pending) {
                     source.push(t);
                 }
             });

        return source.distinct();
    }
}
