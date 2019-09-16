/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.reactfx;

import java.util.Objects;
import java.util.function.Function;

import org.reactfx.EventStream;
import org.reactfx.EventStreamBase;
import org.reactfx.Subscription;
import org.reactfx.util.Timer;

/**
 * An event stream that prunes distinct events only if they occur in during
 * some user specified period. E.g.
 *
 * <pre>{@code
 * input:           ---------A--A-------A--B--B------
 * timer (3 units): ---------|>>>|>>>|--|>>|>>|>>>|--
 * output:          ---------A----------A--B---------
 * }</pre>
 *
 * Ie, once the timer expires the latest event can be reemitted again.
 *
 * @author Clément Fournier
 */
public final class DistinctBetweenStream<I> extends EventStreamBase<I> {

    private static final Object NONE = new Object();
    private final EventStream<I> input;
    private final Timer timer;
    private Object previous = NONE;

    private DistinctBetweenStream(EventStream<I> input, Function<Runnable, Timer> timerFactory) {
        this.input = input;
        this.timer = timerFactory.apply(() -> previous = NONE);
    }

    @Override
    protected Subscription observeInputs() {
        return input.subscribe(value -> {
            Object prevToCompare = previous;
            previous = value;
            timer.restart();
            if (!Objects.equals(value, prevToCompare)) {
                emit(value);
            }
        });
    }

    static <I> DistinctBetweenStream<I> distinctBetween(EventStream<I> input, Function<Runnable, Timer> timerFactory) {
        return new DistinctBetweenStream<I>(input, timerFactory);
    }

}
