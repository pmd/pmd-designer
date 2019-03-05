/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.reactfx;

import java.time.Duration;

import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.value.Val;
import org.reactfx.value.ValBase;

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
}
