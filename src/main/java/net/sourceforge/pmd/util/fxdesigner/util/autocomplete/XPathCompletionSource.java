/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.autocomplete;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers.CamelCaseMatcher;
import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers.MatchResult;
import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers.MatchSelector;
import net.sourceforge.pmd.util.fxdesigner.util.autocomplete.matchers.StringMatchUtil;


/**
 * Language specific tool to suggest auto-completion results.
 */
public final class XPathCompletionSource implements CompletionResultSource {

    private static final Comparator<? extends MatchResult<?>> DISPLAY_ORDER =
        Comparator.<MatchResult<?>>naturalOrder()
            .reversed()
            // shorter results are displayed first when there's a tie
            .thenComparing(MatchResult::getStringMatch, Comparator.comparing(String::length));
    // if we don't cache them the classpath exploration is done on each character typed
    private static final Map<Language, XPathCompletionSource> BY_LANGUAGE = new HashMap<>();
    private final NodeNameFinder myNameFinder;

    private XPathCompletionSource(NodeNameFinder nodeNameFinder) {
        this.myNameFinder = nodeNameFinder;


    }

    private MatchSelector<String> getLimiter(int limit) {
        MatchSelector<String> limited = MatchSelector.limitToBest(limit);
        return CamelCaseMatcher.<String>allQueryStarts().andThen(limited)
                                                        .andThen(CamelCaseMatcher.onlyWordStarts())
                                                        .andThen(limited);

    }

    /**
     * Returns a stream of pre-built TextFlows sorted by relevance.
     * The stream will contain at most "limit" elements.
     */
    @Override
    public Stream<MatchResult<String>> getSortedMatches(String input, int limit) {

        return StringMatchUtil.filterResults(
            myNameFinder.getNodeNames(),
            Function.identity(),
            input,
            getLimiter(limit)
        ).sorted(displayOrder());
    }

    /**
     * Gets a suggestion tool suited to the given language.
     */
    public static XPathCompletionSource forLanguage(Language language) {
        return BY_LANGUAGE.computeIfAbsent(language, l -> new XPathCompletionSource(NodeNameFinder.forLanguage(l)));
    }


    @SuppressWarnings("unchecked")
    private static <T> Comparator<MatchResult<T>> displayOrder() {
        return (Comparator<MatchResult<T>>) DISPLAY_ORDER;
    }
}
