/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.reactfx;

import java.util.Arrays;

import org.reactfx.collection.LiveList;

public interface DefaultMutableLiveList<O> extends LiveList<O> {


    @Override
    default boolean addAll(O... elements) {
        return addAll(Arrays.asList(elements));
    }


    @Override
    default boolean setAll(O... elements) {
        return setAll(Arrays.asList(elements));
    }


    @Override
    default boolean removeAll(O... elements) {
        return removeAll(Arrays.asList(elements));
    }


    @Override
    default boolean retainAll(O... elements) {
        return retainAll(Arrays.asList(elements));
    }

}
