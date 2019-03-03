package net.sourceforge.pmd.util.fxdesigner.util.datakeys;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Clément Fournier
 */
public final class DataKey<T> {


    private final String name;
    private final T defaultValue;

    public DataKey(String name, T defaultValue) {
        this.name = name;
        this.defaultValue = defaultValue;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataKey<?> dataKey = (DataKey<?>) o;
        return Objects.equals(name, dataKey.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
