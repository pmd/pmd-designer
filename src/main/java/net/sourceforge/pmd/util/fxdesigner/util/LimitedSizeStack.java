/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import java.util.ArrayDeque;

/**
 * Stack with a limited size, without duplicates, without null value. Used to store recent files.
 *
 * @param <E> Element type
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public class LimitedSizeStack<E> extends ArrayDeque<E> {

    private final int maxSize;


    public LimitedSizeStack(int maxSize) {
        this.maxSize = maxSize;
    }


    @Override
    public void push(E item) {
        if (item == null) {
            return;
        }

        this.remove(item);

        super.push(item);

        if (size() > maxSize) {
            this.removeLast();
        }
    }
}
