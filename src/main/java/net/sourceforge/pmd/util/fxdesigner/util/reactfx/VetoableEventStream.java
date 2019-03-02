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
        Predicate<I> shouldWait,
        Predicate<I> cancelSignal,
        Function<Runnable, Timer> timerFactory) {

        this.input = input;
        this.vetoableReduction = vetoableReduction;
        this.isVetoable = shouldWait;
        this.isVetoSignal = cancelSignal;
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
    protected final Subscription observeInputs() {
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


    public static <I> AwaitingEventStream<I> vetoableFrom(EventStream<I> input,
                                                          BiFunction<I, I, I> vetoableReduction,
                                                          Predicate<I> shouldWait,
                                                          Predicate<I> cancelSignal,
                                                          Function<Runnable, Timer> timerFactory) {
        return new VetoableEventStream<>(input, vetoableReduction, shouldWait, cancelSignal, timerFactory);
    }


    public static <I> AwaitingEventStream<I> vetoableFrom(EventStream<I> input,
                                                          BiFunction<I, I, I> vetoableReduction,
                                                          Predicate<I> shouldWait,
                                                          Predicate<I> cancelSignal,
                                                          Duration vetoPeriod) {
        Function<Runnable, Timer> timerFactory = action -> FxTimer.create(vetoPeriod, action);
        return vetoableFrom(input, vetoableReduction, shouldWait, cancelSignal, timerFactory);
    }


    public static AwaitingEventStream<Boolean> vetoableYes(EventStream<Boolean> input, Duration vetoPeriod) {
        return vetoableFrom(input, (a, b) -> b, b -> b, b -> !b, vetoPeriod);
    }
}
