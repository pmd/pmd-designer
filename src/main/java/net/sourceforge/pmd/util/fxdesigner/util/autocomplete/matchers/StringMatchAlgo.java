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
public interface StringMatchAlgo {

    <T> Stream<MatchResult<T>> filterResults(List<T> candidates, Function<T, String> matchExtractor, String query, MatchLimiter limiter);


}
