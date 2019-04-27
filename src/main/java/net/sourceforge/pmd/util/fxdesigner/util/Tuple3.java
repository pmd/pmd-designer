/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import java.util.Objects;

/**
 * @author Clément Fournier
 */
public class Tuple3<A, B, C> {

    public final A first;
    public final B second;
    public final C third;

    public Tuple3(A a, B b, C c) {
        this.first = a;
        this.second = b;
        this.third = c;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Tuple3<?, ?, ?> tuple3 = (Tuple3<?, ?, ?>) o;
        return Objects.equals(first, tuple3.first)
            && Objects.equals(second, tuple3.second)
            && Objects.equals(third, tuple3.third);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second, third);
    }

    @Override
    public String toString() {
        return "Tuple3{"
            + "a=" + first
            + ", b=" + second
            + ", c=" + third
            + '}';
    }
}
