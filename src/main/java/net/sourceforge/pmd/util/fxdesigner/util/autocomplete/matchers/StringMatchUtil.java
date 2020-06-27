/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;


/**
 * Utility class.
 */
public final class StringMatchUtil {

    public static final int MIN_QUERY_LENGTH = 1;
    public static final int WORST_SCORE = Integer.MIN_VALUE;
    public static final int PERFECT_SCORE = Integer.MAX_VALUE;

    private StringMatchUtil() {

    }

    /**
     * Selects the best {@link MatchResult} given a list of candidates and a query.
     *
     * <p>The results are useless unless you provide the {@link MatchSelector} that
     * suits your use case (limiter).
     *
     * @param candidates     List of stuff to sort
     * @param matchExtractor Extracts the searchable text from an item
     * @param limiter        Selects the best candidates, may process them further to break ties
     * @param query          Text to search for
     */
    public static <T> Stream<MatchResult<T>> filterResults(List<? extends T> candidates,
                                                           Function<? super T, String> matchExtractor,
                                                           String query,
                                                           MatchSelector<T> limiter) {
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
        return DesignerUtil.makeStyledText(text, "text");
    }

}
