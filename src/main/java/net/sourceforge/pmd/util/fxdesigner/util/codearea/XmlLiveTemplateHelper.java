/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.codearea;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringEscapeUtils;
import org.fxmisc.richtext.CodeArea;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;

import net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.HighlightClasses;

import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;


/**
 * @author Clément Fournier
 * @since 6.0.0
 */
public class XmlLiveTemplateHelper {


    private final CodeArea codeArea;


    public XmlLiveTemplateHelper(CodeArea area) {
        this.codeArea = area;
    }


    /**
     * Makes a new subscription builder.
     *
     * @param val            Value to which we should subscribe
     * @param replacePattern pattern used to find the text to replace
     */
    public <T> ReplacementSubscriptionBuilder<T> replace(ObservableValue<T> val, Pattern replacePattern) {
        return this.new ReplacementSubscriptionBuilder<>(val, replacePattern);
    }


    public class OptionalGroup<E> {
        private final Pattern insertionPoint;

        private ObservableList<E> elements;


        public OptionalGroup(Pattern insertionPoint) {
            this.insertionPoint = insertionPoint;
        }


        public void setElements(ObservableList<E> elts) {
            elements = elts;
            elts.addListener((ListChangeListener<? super E>) e -> {
                if (e.wasAdded()) {
                    int from = e.getFrom();
                    int to = e.getTo();

                }
            });
        }


        private int getInsertionLine() {
            Matcher matcher = insertionPoint.matcher(codeArea.getText());
            if (matcher.find()) {
                return matcher.end();
            }
            return -1;
        }


    }

    // TODO
    public class OptionalElement {

        private final Supplier<String> update;
        private boolean isInserted;
        private String currentValue;
        private Map<ObservableValue<?>, Integer> valueToGroup;
        private String indent;
        private boolean newLine;


        private OptionalElement(Supplier<String> update) {
            this.update = update;
        }


        /** Insert the element in the result text. */
        public synchronized void insert(Pattern insertionPoint, int indent, boolean newline) {
            if (isInserted) {
                return;
            }

            this.indent = String.join("", Collections.nCopies(indent, " "));
            this.newLine = newline;

            Matcher m = insertionPoint.matcher(codeArea.getText());
            if (m.find()) {
                String up = update.get();
                codeArea.insertText(m.end(), up);
                currentValue = up;
                isInserted = true;
            }
        }


        private String getUpdate() {
            return indent + update.get() + (newLine ? "\n" : "");
        }


        /** Removes the element. */
        public synchronized void removeElement() {
            if (!isInserted) {
                return;
            }
            Matcher matcher = getFullMatchPattern().matcher(codeArea.getText());
            if (matcher.find()) {
                codeArea.replaceText(matcher.start(), matcher.end(), "");
                isInserted = false;
            }
        }


        /**
         * Returns a pattern that matches exactly {@link #currentValue}.
         * Can be overridden to provide more precise implementations.
         */
        protected Pattern getFullMatchPattern() {
            return Pattern.compile(Pattern.quote(currentValue));
        }


        /** Updates the text by replacing a group with the new value. */
        public synchronized void updateElement(int groupNum) {
            if (!isInserted) {
                return;
            }
            Matcher matcher = getFullMatchPattern().matcher(codeArea.getText());
            if (matcher.find()) {
                String up = getUpdate(); // make style
                codeArea.replaceText(matcher.start(), matcher.end(), up);
                currentValue = up;
            }
        }
    }


    /**
     * Creates a subscription that reacts to the changes in one observable value (eg one field of the form)
     * by replacing part of the text in the codearea with the new value.
     *
     * @param <T> Type of the value
     */
    public class ReplacementSubscriptionBuilder<T> {

        private final ObservableValue<T> value;
        private final Pattern replacePattern;
        private Function<T, String> converter = Object::toString;
        private boolean isEscaped = true;
        private Collection<String> style = Collections.emptySet();


        /**
         * New subscription builder.
         *
         * @param val     Value to which we should subscribe
         * @param pattern Regex pattern used to find the text to replace
         */
        ReplacementSubscriptionBuilder(ObservableValue<T> val, Pattern pattern) {
            this.value = val;
            this.replacePattern = pattern;
        }


        public ReplacementSubscriptionBuilder<T> converter(Function<T, String> converter) {
            this.converter = converter;
            return this;
        }


        public ReplacementSubscriptionBuilder<T> asCdata() {
            this.isEscaped = false;
            this.style = HighlightClasses.XML_CDATA_CONTENT.css;
            return this;
        }


        public ReplacementSubscriptionBuilder<T> asAttribute() {
            this.isEscaped = true;
            this.style = HighlightClasses.STRING.css;
            return this;
        }


        public ReplacementSubscriptionBuilder<T> escaped(boolean b) {
            this.isEscaped = b;
            return this;
        }


        public ReplacementSubscriptionBuilder<T> style(Collection<String> sty) {
            this.style = sty;
            return this;
        }


        public Subscription build() {
            return EventStreams.changesOf(value)
                               .successionEnds(Duration.ofMillis(50))
                               .subscribe(c -> replaceText(replacePattern, converter.apply(c.getNewValue()), style, isEscaped));
        }


        // End of the first group is the start position of the replaced text
        // Beginning of the last group is the end position of the replaced text
        private void replaceText(Pattern pattern, String newText, Collection<String> style, boolean isEscaped) {
            Matcher matcher = pattern.matcher(codeArea.getText());
            if (matcher.find()) {
                int startReplace = matcher.end(1);
                int endReplace = matcher.start(matcher.groupCount());
                codeArea.replace(startReplace,
                                 endReplace,
                                 isEscaped ? StringEscapeUtils.escapeXml10(newText) : newText,
                                 style);
            }

        }

    }

}
