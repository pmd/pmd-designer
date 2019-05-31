/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.autocomplete;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;


/**
 * Selects the best match results given a list of candidates and a query.
 * We can abstract that later if we need it. E.g. we could provide more
 * informed guesses based on what nodes are frequently found in that position
 * in known XPath queries, or parse JJDoc output and suggest nodes that we
 * know can be children of the previous node.
 *
 * @author Clément Fournier
 * @since 7.0.0
 */
public class ResultSelectionStrategy {

    private static final int MIN_QUERY_LENGTH = 1;

    private static final Comparator<? extends MatchResult<?>> DISPLAY_ORDER =
        Comparator.<MatchResult<?>>naturalOrder()
            .reversed()
            // shorter results are displayed first when there's a tie
            .thenComparing(MatchResult::getStringMatch, Comparator.comparing(String::length));

    public <T> Stream<MatchResult<T>> filterResults(List<T> candidates, Function<T, String> candExtractor, String query, int limit) {
        if (query.length() < MIN_QUERY_LENGTH) {
            return Stream.empty();
        }

        return candidates.stream()
                         .map(cand -> computeMatchingSegments(cand, candExtractor.apply(cand), query, false))
                         .sorted(Comparator.<MatchResult<?>>naturalOrder().reversed())
                         .filter(it -> it.getScore() > 0)
                         .limit(limit)
                         // second pass is done only on those we know we'll keep
                         .map(prev -> {
                             // try to break ties between the top results, e.g.
                             //
                             // without second pass, we have a tie:
                             //      query       coit
                             //      candidate   ClassOrInterfaceType            : 32
                             //      candidate   ClassOrInterfaceBodyDeclaration : 32
                             //                  ^    ^ ^ ^
                             // with second pass:
                             //
                             //      query       coit
                             //      candidate   ClassOrInterfaceType            : 40 -> and indeed it's a better match
                             //                  ^    ^ ^        ^
                             //      candidate   ClassOrInterfaceDeclaration     : 32
                             //                  ^    ^ ^ ^

                             MatchResult<T> refined = computeMatchingSegments(prev.getData(), prev.getStringMatch(), query, true);
                             // keep the best
                             return refined.getScore() > prev.getScore() ? refined : prev;
                         })
                         .sorted(displayOrder());


    }

    public <T> Optional<MatchResult<T>> evaluateBestSingle(T data, String candidate, String query) {
        if (query == null || query.length() < MIN_QUERY_LENGTH || StringUtils.isEmpty(candidate)) {
            return Optional.empty();
        }

        return Optional.of(computeMatchingSegments(data, candidate, query, false))
                       .filter(it -> it.getScore() > 0)
                       .map(prev -> {
                           MatchResult<T> refined = computeMatchingSegments(data, prev.getStringMatch(), query, true);
                           // keep the best
                           return refined.getScore() > prev.getScore() ? refined : prev;
                       });
    }

    /**
     * Computes a match result with its score for the candidate and query.
     *
     * @param candidate           Candidate string
     * @param query               Query
     * @param matchOnlyWordStarts Whether to only match word starts. This is a more unfair strategy
     *                            that can be used to break ties.
     */
    private <T> MatchResult<T> computeMatchingSegments(T data, String candidate, String query, boolean matchOnlyWordStarts) {
        if (candidate.equalsIgnoreCase(query)) {
            // perfect match
            TextFlow flow = new TextFlow(makeHighlightedText(candidate));
            return new MatchResult<>(Integer.MAX_VALUE, data, candidate, flow);
        }

        // Performs a left-to-right scan of the candidate string,
        // trying to assign each of the chars of the query to a
        // location in the string (also left-to-right)

        // Score is computed a bit ad-hoc:
        // +2 for a lonely char
        // +10 for a character matching the start of a camelcase word (an uppercase char)
        // the longer the submatch, the higher the match counts
        // submatches occurring at the beginning of a word count more than in other places
        // chars from the query that remain at the end penalise the score

        // This algorithm is greedy and doesn't always select the best possible match result
        // The second pass is even more unfair and allows to break ties

        int candIdx = 0;  // current index in the candidate
        int queryIdx = 0; // current index in the query
        int score = 0;

        // these are reset when a submatch ends
        int lastMatchEnd = 0;
        int curMatchStart = -1;
        int matchLength = 0;
        boolean isStartOfWord = true; // whether the current submatch is at the start of a camelcase word

        TextFlow flow = new TextFlow(); // result

        while (candIdx < candidate.length() && queryIdx < query.length()) {

            char candChar = candidate.charAt(candIdx);
            char queryChar = query.charAt(queryIdx);

            if (Character.toLowerCase(candChar) == Character.toLowerCase(queryChar)) {
                // it's the same char

                matchLength++;

                if (curMatchStart == -1) {
                    // start of a match

                    if (matchOnlyWordStarts && !isStartOfWord && !isWordStart(candidate, candIdx)) {
                        // not the start of a word, don't record it as a match
                        candIdx++;
                        continue;
                    }

                    // set match start to current
                    curMatchStart = candIdx;

                    if (isWordStart(candidate, candIdx)) {
                        // start of a match on the start of a word
                        // e.g. query       coit
                        //      candidate   ClassOrInterfaceType
                        //                  ^    ^ ^ ^
                        //      score       34

                        isStartOfWord = true;
                        score += 10;
                    } else {
                        isStartOfWord = false;
                        score += 2;
                    }

                } else {
                    // match is running-on

                    // e.g. query       wur
                    //      candidate   Würstchen
                    //                  ^^^
                    //      candidate   BratWurst
                    //                      ^^^
                    //      score       38 = 4 + 8 + 16 + (start of word : 10)
                    //------------------
                    //      query       wur
                    //      candidate   Bratwurst
                    //                      ^^^
                    //      score       14 = 2 + 4 + 8
                    //------------------
                    //      query       wur
                    //      candidate   zweihundert
                    //                   ^   ^   ^
                    //      score       6 = 2 + 2 + 2

                    int multiplier = isStartOfWord ? 4 : 2;
                    score += matchLength * multiplier;
                }

                candIdx++;
                queryIdx++;

            } else {
                // the current chars don't match

                if (curMatchStart != -1) {
                    // end of a match
                    // assert matchLength > 0;

                    String before = candidate.substring(lastMatchEnd, curMatchStart);
                    String match = candidate.substring(curMatchStart, curMatchStart + matchLength);

                    if (before.length() > 0) {
                        flow.getChildren().add(makeNormalText(before));
                    }

                    flow.getChildren().add(makeHighlightedText(match));

                    lastMatchEnd = curMatchStart + matchLength;
                }

                candIdx++;
                // stay on same query index

                // reset match
                curMatchStart = -1;
                matchLength = 0;
                isStartOfWord = false;
            }
        }

        // end of loop

        if (curMatchStart != -1 && candIdx <= candidate.length()) {
            // the query ends inside a match, we must complete the current match

            String before = candidate.substring(lastMatchEnd, curMatchStart);
            String match = candidate.substring(curMatchStart, candIdx);

            if (before.length() > 0) {
                flow.getChildren().add(makeNormalText(before));
            }

            flow.getChildren().add(makeHighlightedText(match));

            lastMatchEnd = candIdx; // shift
        }

        // add the rest of the candidate
        String rest = candidate.substring(lastMatchEnd);
        if (!rest.isEmpty()) {
            flow.getChildren().add(makeNormalText(rest));
        }

        int remainingChars = query.length() - queryIdx;

        if (remainingChars > 0) {
            // some chars were not found, penalize that
            score -= remainingChars * 5;
        }

        final int finalScore = score;

        return new MatchResult<>(finalScore, data, candidate, flow);
    }


    private Text makeHighlightedText(String match) {
        Text matchLabel = makeNormalText(match);
        matchLabel.getStyleClass().add("autocomplete-match");
        return matchLabel;
    }

    private Text makeNormalText(String text) {
        Text matchLabel = new Text(text);
        matchLabel.getStyleClass().add("text");
        return matchLabel;
    }

    private boolean isWordStart(String pascalCased, int idx) {
        return idx == 0 || Character.isUpperCase(pascalCased.charAt(idx)) && Character.isLowerCase(pascalCased.charAt(
            idx - 1));
    }

    @SuppressWarnings("unchecked")
    private static <T> Comparator<MatchResult<T>> displayOrder() {
        return (Comparator<MatchResult<T>>) DISPLAY_ORDER;
    }

}
