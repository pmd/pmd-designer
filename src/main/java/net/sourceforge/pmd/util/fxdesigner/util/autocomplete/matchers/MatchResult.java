/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers;

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
    private final String query;


    public MatchResult(int score, T data, String suggestion, String query, TextFlow textFlow) {
        this.score = score;
        this.data = data;
        this.suggestion = suggestion;
        this.textFlow = textFlow;
        this.query = query;
    }

    /**
     * Data from which the candidate string was extracted.
     */
    public T getData() {
        return data;
    }

    /**
     * Candidate string that was matched against the query.
     */
    public String getStringMatch() {
        return suggestion;
    }

    /**
     * Query that was matched against the candidate string. This
     * is the user input.
     */
    public String getQuery() {
        return query;
    }


    /**
     * Formatted TextFlow with the match regions highlighted.
     */
    public TextFlow getTextFlow() {
        return textFlow;
    }


    /**
     * Relevance score of this result. This is largely implementation specific
     * and has no meaning unless comparing with results selected by the same implementation
     * that produced this match.
     */
    public int getScore() {
        return score;
    }


    @Override
    public int compareTo(MatchResult<?> o) {
        return Integer.compare(score, o.score);
    }
}
