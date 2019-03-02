package net.sourceforge.pmd.util.fxdesigner.util.reactfx;

import java.time.Duration;

import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.value.Val;
import org.reactfx.value.ValBase;

/**
 * @author Clément Fournier
 */
public final class ReactfxUtil {

    private ReactfxUtil() {

    }

    /**
     * Converts an event stream to a val.
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
