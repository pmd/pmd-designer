/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableInt;

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


    /**
     * Selects all the results that matched the highest, preserving all
     * tied best results.
     */
    static MatchLimiter selectBestTies() {
        return new MatchLimiter() {
            @Override
            public <T> Stream<MatchResult<T>> selectBest(Stream<MatchResult<T>> raw) {

                MutableInt bestScore = new MutableInt(Integer.MIN_VALUE);

                Set<MatchResult<T>> bestTies = new HashSet<>();

                raw.forEach(it -> {
                    if (it.getScore() > bestScore.getValue()) {
                        bestScore.setValue(it.getScore());
                        bestTies.clear();
                        bestTies.add(it);
                    } else if (it.getScore() == bestScore.getValue()) {
                        bestTies.add(it);
                    }
                });


                return bestTies.stream();
            }
        };
    }


}
