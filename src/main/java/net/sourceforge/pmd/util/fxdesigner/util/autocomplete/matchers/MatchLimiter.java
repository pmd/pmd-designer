/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableInt;

import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.MatchResult;

/**
 * Strategy to help a {@link StringMatchAlgo} to make more precise guesses.
 */
@FunctionalInterface
public interface MatchLimiter<T> {

    Stream<MatchResult<T>> selectBest(Stream<MatchResult<T>> raw);


    default MatchLimiter<T> andThen(MatchLimiter<T> next) {
        return base -> next.selectBest(this.selectBest(base));
    }

    /**
     * Limits results to a fixed maximum size.
     */
    static <T> MatchLimiter<T> limitToBest(int limit) {
        return raw -> raw.sorted(Comparator.<MatchResult<?>>naturalOrder().reversed())
                         .limit(limit);
    }


    /**
     * Selects all the results that matched the highest, preserving all
     * tied best results.
     */
    static <T> MatchLimiter<T> selectBestTies() {
        return raw -> {

            MutableInt bestScore = new MutableInt(Integer.MIN_VALUE);

            List<MatchResult<T>> bestTies = new ArrayList<>();

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
        };
    }


    /**
     * Selects all the results that matched the highest, preserving all
     * tied best results.
     */
    static <T> MatchLimiter<T> selectNthDegreeBestTies(final int level) {
        if (level <= 0) {
            throw new IllegalArgumentException();
        }
        return raw -> {

            MutableInt bestScore = new MutableInt(Integer.MIN_VALUE);

            // 0 is the highest, last is the lowest pass
            List<Integer> thresholds = Stream.generate(() -> Integer.MIN_VALUE).limit(level).collect(Collectors.toList());

            List<MatchResult<T>> bestTies = new ArrayList<>();

            raw.forEach(result -> {
                if (result.getScore() > bestScore.getValue()) {
                    int last = thresholds.get(0);
                    thresholds.set(0, bestScore.getValue());
                    for (int i = 1; i < thresholds.size(); i++) {
                        int tmp = thresholds.get(i);
                        thresholds.set(i, last);
                        last = tmp;
                    }
                    bestScore.setValue(result.getScore());
                    bestTies.removeIf(r -> r.getScore() < thresholds.get(thresholds.size() - 1));
                    bestTies.add(result);
                } else if (result.getScore() >= thresholds.get(thresholds.size() - 1)) {
                    bestTies.add(result);
                }
            });


            return bestTies.stream();
        };
    }


}
