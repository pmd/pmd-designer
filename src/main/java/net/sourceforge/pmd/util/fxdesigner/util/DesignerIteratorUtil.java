/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;


/**
 * @author Clément Fournier
 * @since 6.11.0
 */
public final class DesignerIteratorUtil {

    // TODO move that into PMD core with Java 8


    private DesignerIteratorUtil() {

    }

    public static <T> Stream<T> takeWhile(Stream<T> stream, Predicate<? super T> predicate) {
        return StreamSupport.stream(takeWhile(stream.spliterator(), predicate), false);
    }


    private static <T> Spliterator<T> takeWhile(Spliterator<T> splitr, Predicate<? super T> predicate) {
        return new Spliterators.AbstractSpliterator<T>(splitr.estimateSize(), 0) {
            boolean stillGoing = true;

            @Override
            public boolean tryAdvance(Consumer<? super T> consumer) {
                if (stillGoing) {
                    boolean hadNext = splitr.tryAdvance(elem -> {
                        if (predicate.test(elem)) {
                            consumer.accept(elem);
                        } else {
                            stillGoing = false;
                        }
                    });
                    return hadNext && stillGoing;
                }
                return false;
            }
        };
    }

    public static <T> T last(Iterator<T> ts) {
        T t = null;
        while (ts.hasNext()) {
            t = ts.next();
        }
        return t;
    }

    public static <T> boolean any(Iterator<? extends T> it, Predicate<? super T> predicate) {
        for (T t : toIterable(it)) {
            if (predicate.test(t)) {
                return true;
            }
        }
        return false;
    }

    public static <T> Stream<T> toStream(Iterator<T> it) {
        return StreamSupport.stream(toIterable(it).spliterator(), false);
    }


    public static <T> Iterator<T> reverse(Iterator<T> it) {
        List<T> tmp = toList(it);
        Collections.reverse(tmp);
        return tmp.iterator();
    }


    public static <T> List<T> toList(Iterator<T> it) {
        List<T> list = new ArrayList<>();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list;
    }


    public static <T> Iterable<T> toIterable(final Iterator<T> it) {
        return () -> it;
    }


    /** Counts the items in this iterator, exhausting it. */
    public static int count(Iterator<?> it) {
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        return count;
    }


    public static <T> Iterable<T> asReversed(final List<T> lst) {

        return () -> new Iterator<T>() {

            ListIterator<T> li = lst.listIterator(lst.size());


            @Override
            public boolean hasNext() {
                return li.hasPrevious();
            }


            @Override
            public T next() {
                return li.previous();
            }


            @Override
            public void remove() {
                li.remove();
            }
        };
    }

    /**
     * Gets an iterator with a successor fun.
     *
     * @param seed         Seed item
     * @param hasSuccessor Tests whether the seed / the last item output has a successor
     * @param successorFun Successor function
     * @param includeSeed  Whether to include the seed as the first item of the iterator
     * @param <T>          Type of values
     *
     * @return An iterator
     */
    public static <T> Iterator<T> iteratorFrom(T seed, Predicate<T> hasSuccessor, Function<T, T> successorFun,
                                               boolean includeSeed) {

        return new Iterator<T>() {

            private T current = seed;
            private boolean myIncludeCurrent = includeSeed; // include the current item iff it's the first and includeFirst


            @Override
            public boolean hasNext() {
                return myIncludeCurrent || hasSuccessor.test(current);
            }


            @Override
            public T next() {

                if (myIncludeCurrent) {
                    myIncludeCurrent = false;
                } else {
                    current = successorFun.apply(current);
                }

                return current;
            }
        };
    }

    // TODO move to IteratorUtil
    static <T> Stream<T> enumerationAsStream(Enumeration<T> e) {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                new Iterator<T>() {
                    @Override
                    public T next() {
                        return e.nextElement();
                    }


                    @Override
                    public boolean hasNext() {
                        return e.hasMoreElements();
                    }
                },
                Spliterator.ORDERED), false);
    }
}
