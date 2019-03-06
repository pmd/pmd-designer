/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.reactfx;

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.reactfx.AwaitingEventStream;
import org.reactfx.EventStream;
import org.reactfx.EventStreamBase;
import org.reactfx.Subscription;
import org.reactfx.util.FxTimer;
import org.reactfx.util.Timer;
import org.reactfx.value.Val;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;

/**
 * An event stream whose events can be vetoed during a certain period, after which they are
 * emitted.
 *
 * @author Clément Fournier
 */
public final class VetoableEventStream<I> extends EventStreamBase<I> implements AwaitingEventStream<I> {

    private final EventStream<I> input;
    private final BiFunction<I, I, I> vetoableReduction;
    private final Predicate<I> isVetoable;
    private final Predicate<I> isVetoSignal;

    private final Timer timer;

    private BooleanBinding pending = null;
    private I vetoable = null;

    private VetoableEventStream(
        EventStream<I> input,
        BiFunction<I, I, I> vetoableReduction,
        Predicate<I> isVetoable,
        Predicate<I> isVeto,
        Function<Runnable, Timer> timerFactory) {

        this.input = input;
        this.vetoableReduction = vetoableReduction;
        this.isVetoable = isVetoable;
        this.isVetoSignal = isVeto;
        this.timer = timerFactory.apply(this::handleTimeout);
    }

    @Override
    public ObservableBooleanValue pendingProperty() {
        if (pending == null) {
            pending = new BooleanBinding() {
                @Override
                protected boolean computeValue() {
                    return vetoable != null;
                }
            };
        }
        return pending;
    }

    @Override
    public boolean isPending() {
        return pending != null ? pending.get() : vetoable != null;
    }

    @Override
    protected Subscription observeInputs() {
        return input.subscribe(this::handleEvent);
    }

    private void handleEvent(I i) {
        if (vetoable != null) {
            if (isVetoSignal.test(i)) {
                timer.stop();
                vetoable = null;
                invalidatePending();
                emit(i);
            } else if (isVetoable.test(i)) {
                I reduced = vetoableReduction.apply(vetoable, i);
                vetoable = null;
                handleEvent(reduced); // test whether the reduced is vetoable
            }
        } else {
            if (isVetoable.test(i)) {
                vetoable = i;
                timer.restart();
                invalidatePending();
            } else {
                emit(i);
            }
        }
    }

    private void handleTimeout() {
        if (vetoable == null) {
            return;
        }
        I toEmit = vetoable;
        vetoable = null;
        emit(toEmit);
        invalidatePending();
    }

    private void invalidatePending() {
        if (pending != null) {
            pending.invalidate();
        }
    }


    /**
     * Low-level method to create a vetoable event stream.
     *
     * @param input             Input event stream
     * @param isVetoable        Predicate that accepts events that should wait for the veto period
     *                          before it's emitted.
     * @param isVeto            Predicate that accepts events that cancel a pending vetoable event
     * @param vetoableReduction Reduces two vetoable events, if a new vetoable event is recorded
     *                          while another one was already pending. The pending event is passed
     *                          as the first parameter. If the result of the reduction is vetoable,
     *                          then it's enqueued and treated as pending. Otherwise it's emitted as
     *                          a normal event.
     * @param timerFactory      Factory that produces a timer for the veto period of a vetoable
     */
    public static <I> AwaitingEventStream<I> vetoableFrom(EventStream<I> input,
                                                          Predicate<I> isVetoable,
                                                          Predicate<I> isVeto,
                                                          BiFunction<I, I, I> vetoableReduction,
                                                          Function<Runnable, Timer> timerFactory) {
        return new VetoableEventStream<>(input, vetoableReduction, isVetoable, isVeto, timerFactory);
    }


    public static <I> AwaitingEventStream<I> vetoableFrom(EventStream<I> input,
                                                          Predicate<I> isVetoable,
                                                          Predicate<I> isVeto,
                                                          BiFunction<I, I, I> vetoableReduction,
                                                          Duration vetoPeriod) {

        Function<Runnable, Timer> timerFactory = action -> FxTimer.create(vetoPeriod, action);
        return vetoableFrom(input, isVetoable, isVeto, vetoableReduction, timerFactory);
    }

    /**
     * @see ReactfxUtil#vetoableYes(Val, Duration)
     */
    public static AwaitingEventStream<Boolean> vetoableYes(EventStream<Boolean> input, Duration vetoPeriod) {
        return vetoableFrom(input, b -> b, b -> !b, (a, b) -> b, vetoPeriod);
    }
}
