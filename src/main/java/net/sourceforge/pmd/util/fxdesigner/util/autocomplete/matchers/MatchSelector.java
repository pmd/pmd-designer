/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.mutable.MutableInt;


/**
 * Strategy to filter {@link MatchResult}s.
 *
 * <p>Depending on the use case (multi-word input, short input, etc),
 * you compose a pipeline of {@link MatchSelector}s, which will filter
 * a stream of results one after the other.
 *
 * <p>Some selectors *produce* match results, those are provided as static
 * factories of {@link CamelCaseMatcher}. Others only filter results, they're
 * found here.
 *
 * <p>You may want to sort results after the pipeline is done, or not.
 *
 */
@FunctionalInterface
public interface MatchSelector<T> {

    Stream<MatchResult<T>> selectBest(Stream<MatchResult<T>> raw);


    default MatchSelector<T> andThen(MatchSelector<T> next) {
        return base -> next.selectBest(this.selectBest(base));
    }

    /**
     * Limits results to a the best maximum size.
     */
    static <T> MatchSelector<T> limitToBest(int limit) {
        return raw -> raw.sorted(Comparator.<MatchResult<?>>naturalOrder().reversed())
                         .limit(limit);
    }


    /**
     * Selects all the results that matched the highest score, preserving
     * all tied best results.
     */
    static <T> MatchSelector<T> selectBestTies() {
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

}
