/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers;

import static net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers.StringMatchAlgo.PERFECT_SCORE;
import static net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers.StringMatchAlgo.WORST_SCORE;

import java.util.Locale;

import javafx.scene.text.TextFlow;


/**
 * This works ok for single camel-case words, but
 *
 * @author Clément Fournier
 * @since 7.0.0
 */
public final class CamelCaseMatcher {


    private CamelCaseMatcher() {

    }

    /**
     * Computes a match result with its score for the candidate and query.
     *
     * @param candidate           Candidate string
     * @param query               Query
     * @param fromIndex           Index in the candidate where to start the match
     * @param matchOnlyWordStarts Whether to only match word starts. This is a more unfair strategy
     *                            that can be used to break ties.
     */
    private static <T> MatchResult<T> computeMatchingSegments(T data, String candidate, String query, int fromIndex, boolean matchOnlyWordStarts) {
        if (candidate.equalsIgnoreCase(query)) {
            // perfect match
            return perfectMatch(data, candidate, query);
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

        int candIdx = fromIndex;  // current index in the candidate
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

                    if (matchOnlyWordStarts && !isStartOfWord && !isWordStart(candidate, candIdx, fromIndex)) {
                        // not the start of a word, don't record it as a match
                        candIdx++;
                        continue;
                    }

                    // set match start to current
                    curMatchStart = candIdx;

                    if (isWordStart(candidate, candIdx, fromIndex)) {
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

                    // matching at the very beginning of the candidate is heavily prioritized
                    int multiplier =
                        isStartOfWord && curMatchStart == 0 ? 8
                                                            : isStartOfWord ? 4 : 2;

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
                        flow.getChildren().add(StringMatchAlgo.makeNormalText(before));
                    }

                    flow.getChildren().add(StringMatchAlgo.makeHighlightedText(match));

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
                flow.getChildren().add(StringMatchAlgo.makeNormalText(before));
            }

            flow.getChildren().add(StringMatchAlgo.makeHighlightedText(match));

            lastMatchEnd = candIdx; // shift
        }

        // add the rest of the candidate
        String rest = candidate.substring(lastMatchEnd);
        if (!rest.isEmpty()) {
            flow.getChildren().add(StringMatchAlgo.makeNormalText(rest));
        }

        int remainingChars = query.length() - queryIdx;

        if (remainingChars > 0) {
            // some chars were not found, penalize that
            //            score -= remainingChars * 5;
        }

        final int finalScore = score;

        return new MatchResult<>(finalScore, data, candidate, query, flow);
    }

    private static boolean isWordStart(String pascalCased, int idx, int fromIndex) {
        if (idx == fromIndex || idx == 0) {
            return true;
        }
        char c = pascalCased.charAt(idx);
        char prev = pascalCased.charAt(idx - 1);
        return Character.isUpperCase(c) && Character.isLowerCase(prev)
            || Character.isAlphabetic(c) && !Character.isAlphabetic(prev);
    }

    private static <T> MatchResult<T> impossibleMatch(T data, String candidate, String query) {
        return new MatchResult<>(WORST_SCORE, data, candidate, query, new TextFlow(StringMatchAlgo.makeNormalText(candidate)));
    }

    private static <T> MatchResult<T> perfectMatch(T data, String candidate, String query) {
        return new MatchResult<>(PERFECT_SCORE, data, candidate, query, new TextFlow(StringMatchAlgo.makeHighlightedText(candidate)));
    }

    /**
     * Breaks some ties, by only matching the input words.
     */
    public static <T> MatchSelector<T> onlyWordStarts() {
        return raw -> raw.map(prev -> {
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

            MatchResult<T> refined = computeMatchingSegments(prev.getData(), prev.getStringMatch(), prev.getQuery(), 0, true);
            // keep the best
            return refined.getScore() > prev.getScore() ? refined : prev;
        });
    }


    /**
     * Scans once left-to-right from the start, picking up on any character
     * in scan order.
     *
     * <p>Enough when the candidate is a single word, but still scans only
     * once so it may miss some opportunities. {@link #onlyWordStarts()} can
     * be used to break ties (they look stupid with this matcher).
     */
    public static <T> MatchSelector<T> sparseCamelMatcher() {
        return raw -> raw.map(prev -> {
            MatchResult<T> refined = computeMatchingSegments(prev.getData(), prev.getStringMatch(), prev.getQuery(), 0, false);
            // keep the best
            return refined.getScore() > prev.getScore() ? refined : prev;
        });
    }

    /**
     * Scans several times from left to right, once for each of the possible
     * match starts, and keeps the best result. This IMO gives the best results,
     * especially when the candidate may be composed of several words.
     */
    public static <T> MatchSelector<T> allQueryStarts() {
        return raw -> raw.map(prev -> {
            if (prev.getScore() == PERFECT_SCORE) {
                return prev;
            }

            String query = prev.getQuery();
            String cand = prev.getStringMatch();
            String lowerCand = cand.toLowerCase(Locale.ROOT);
            char first = Character.toLowerCase(query.charAt(0));
            int i = lowerCand.indexOf(first);

            if (i < 0) {
                // impossible match
                // the algo scans left to right and begins giving out points on the first
                // occurrence of the first char of the query
                // we can weed this case immediately
                return impossibleMatch(prev.getData(), cand, query);
            }

            MatchResult<T> best = prev;
            while (i >= 0) {
                MatchResult<T> attempt = computeMatchingSegments(prev.getData(), cand, query, i, false);
                best = attempt.getScore() > best.getScore() ? attempt : best;

                i = lowerCand.indexOf(first, i + 1);
            }


            return best;
        });
    }

}
