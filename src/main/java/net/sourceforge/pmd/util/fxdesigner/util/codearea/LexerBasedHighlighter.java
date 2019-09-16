/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */


package net.sourceforge.pmd.util.fxdesigner.util.codearea;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.HighlightClasses;


/**
 * Language-specific engine for syntax highlighting.
 *
 * @author Clément Fournier
 * @since 6.19.0
 */
public abstract class LexerBasedHighlighter implements SyntaxHighlighter {

    private final String languageName;


    protected LexerBasedHighlighter(String languageName) {
        this.languageName = languageName;
    }

    protected abstract JflexLexer newLexer(String text);


    @Override
    public StyleSpans<Collection<String>> computeHighlighting(String text) {
        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();

        JflexLexer lexer = newLexer(text);
        try {
            HighlightClasses classes = lexer.nextSpan();
            while (classes != null) {
                HashSet<String> css = new HashSet<>(classes.css.size() + 2);
                css.addAll(classes.css);
                css.add("code");
                css.add(languageName);
                builder.add(css, lexer.yylength());

                classes = lexer.nextSpan();
            }
        } catch (IOException ignored) {
            throw new RuntimeException(ignored); // shouldn't occur
        }

        return builder.create();
    }

    @Override
    public final String getLanguageTerseName() {
        return languageName;
    }


    /** Generated lexers should implement this interface. */
    public interface JflexLexer {

        @Nullable
        HighlightClasses nextSpan() throws IOException;


        int yylength();

    }
}
