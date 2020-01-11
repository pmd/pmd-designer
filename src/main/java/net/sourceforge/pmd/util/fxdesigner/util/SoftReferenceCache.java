/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import java.lang.ref.SoftReference;
import java.util.function.Consumer;


/**
 * Caches a value with a soft reference.
 *
 * @author Clément Fournier
 * @since 6.1.0
 */
public class SoftReferenceCache<T> {

    private final UnsafeSupplier<T> valueSupplier;
    private SoftReference<T> ref;


    public SoftReferenceCache(UnsafeSupplier<T> supplier) {
        this.valueSupplier = supplier;
    }

    public boolean hasValue() {
        return ref != null && ref.get() != null;
    }

    /**
     * Gets the value of this cache. Uses the supplier function
     * in cache of cache miss.
     *
     * @return The value
     */
    public T get() {
        if (ref == null || ref.get() == null) {
            try {
                ref = new SoftReference<>(valueSupplier.get());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return ref.get();
    }

    public void apply(Consumer<T> f) {
        f.accept(get());
    }


    /**
     * Supplier which can throw exceptions.
     *
     * @param <T> Type of value
     */
    @FunctionalInterface
    public interface UnsafeSupplier<T> {
        T get() throws Exception;
    }

}
