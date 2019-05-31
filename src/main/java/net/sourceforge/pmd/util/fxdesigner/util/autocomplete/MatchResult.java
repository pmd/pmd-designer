/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.autocomplete;

import javafx.scene.text.TextFlow;


/**
 * Result of a match algorithm.
 *
 * @param <T> type of input to the algorithm
 *
 * @author Clément Fournier
 * @since 7.0.0
 */
public class MatchResult<T> implements Comparable<MatchResult<?>> {
    private final int score;
    private final T data;
    private final String suggestion;
    private final TextFlow textFlow;


    MatchResult(int score, T data, String suggestion, TextFlow textFlow) {
        this.score = score;
        this.data = data;
        this.suggestion = suggestion;
        this.textFlow = textFlow;
    }

    public T getData() {
        return data;
    }

    /** Suggested node name. */
    public String getStringMatch() {
        return suggestion;
    }


    /**
     * Formatted TextFlow with the match regions highlighted.
     */
    public TextFlow getTextFlow() {
        return textFlow;
    }


    /** Relevance score of this result. */
    int getScore() {
        return score;
    }


    @Override
    public int compareTo(MatchResult<?> o) {
        return Integer.compare(score, o.score);
    }
}
