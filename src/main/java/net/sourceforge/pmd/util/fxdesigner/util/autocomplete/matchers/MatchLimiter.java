/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers;

import java.util.stream.Stream;

import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.MatchResult;

/**
 * Strategy to help a {@link StringMatchAlgo} to make more precise guesses.
 */
@FunctionalInterface
public interface MatchLimiter {

    <T> Stream<MatchResult<T>> selectBest(Stream<MatchResult<T>> raw);


    /**
     * Limits results to a fixed maximum size.
     */
    static MatchLimiter limited(int limit) {
        return new MatchLimiter() {
            @Override
            public <T> Stream<MatchResult<T>> selectBest(Stream<MatchResult<T>> raw) {
                return raw.limit(limit);
            }
        };
    }

}
