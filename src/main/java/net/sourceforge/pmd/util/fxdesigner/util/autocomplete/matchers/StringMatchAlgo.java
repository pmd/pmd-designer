/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.MatchResult;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;


/**
 * Selects the best match results given a list of candidates and a query.
 *
 * The results are useless unless you provide the {@link MatchLimiter} that
 * suits your use case.
 *
 * We can abstract that later if we need it. E.g. we could provide more
 * informed guesses based on what nodes are frequently found in that position
 * in known XPath queries, or parse JJDoc output and suggest nodes that we
 * know can be children of the previous node.
 *
 * @author Clément Fournier
 * @since 7.0.0
 */
public class StringMatchAlgo {

    public static final int MIN_QUERY_LENGTH = 1;
    public static final int WORST_SCORE = Integer.MIN_VALUE;
    public static final int PERFECT_SCORE = Integer.MAX_VALUE;


    public static <T> Stream<MatchResult<T>> filterResults(List<T> candidates, Function<T, String> matchExtractor, String query, MatchLimiter<T> limiter) {
        if (query.length() < MIN_QUERY_LENGTH) {
            return Stream.empty();
        }

        Stream<MatchResult<T>> base = candidates.stream()
                                                .map(it -> {
                                                    String cand = matchExtractor.apply(it);
                                                    return new MatchResult<>(0, it, cand, query, new TextFlow(makeNormalText(cand)));
                                                });
        return limiter.selectBest(base);
    }


    static Text makeHighlightedText(String match) {
        Text matchLabel = makeNormalText(match);
        matchLabel.getStyleClass().add("autocomplete-match");
        return matchLabel;
    }


    static Text makeNormalText(String text) {
        Text matchLabel = new Text(text);
        matchLabel.getStyleClass().add("text");
        return matchLabel;
    }

}
