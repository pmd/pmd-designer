/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author Clément Fournier
 */
public final class DataHolder {

    private final Map<DataKey<?>, Object> data;

    private DataHolder(Map<DataKey<?>, Object> data) {
        this.data = data;
    }

    public DataHolder() {
        this(Collections.emptyMap());
    }


    @SuppressWarnings("unchecked")
    public <T> T getData(DataKey<T> key) {
        return (T) data.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T computeIfAbsent(DataKey<T> key, Supplier<T> value) {
        return (T) data.computeIfAbsent(key, dataKey -> value.get());
    }

    public <T> DataHolder withData(DataKey<T> key, T value) {
        Map<DataKey<?>, Object> newMap = new HashMap<>(data);
        newMap.put(key, value);
        return new DataHolder(newMap);
    }

    public boolean hasData(DataKey<?> key) {
        return data.containsKey(key);
    }

    @Override
    public String toString() {
        return data.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataHolder that = (DataHolder) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }

    /**
     * Uses instance equality.
     *
     * @param <T> Type of data
     */
    public static final class DataKey<T> {

        private final String name;


        public DataKey(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
