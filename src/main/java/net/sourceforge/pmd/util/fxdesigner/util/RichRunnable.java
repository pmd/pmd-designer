/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

@FunctionalInterface
public interface RichRunnable extends Runnable {

    default RichRunnable andThen(Runnable r) {
        return () -> {
            this.run();
            r.run();
        };
    }

}
