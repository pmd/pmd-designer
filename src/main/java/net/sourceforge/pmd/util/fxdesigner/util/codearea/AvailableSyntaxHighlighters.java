/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.codearea;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.fxmisc.richtext.model.StyleSpans;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.ApexSyntaxHighlighter;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.JavaSyntaxHighlighter;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.ModelicaSyntaxHighlighter;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.ScalaSyntaxHighlighter;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.XPathSyntaxHighlighter;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.XmlSyntaxHighlighter;


/**
 * Lists the available syntax highlighter engines by language.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public enum AvailableSyntaxHighlighters implements SyntaxHighlighter {
    JAVA("java", new JavaSyntaxHighlighter()),
    SCALA("scala", new ScalaSyntaxHighlighter()),
    // for the future
    // uses the same lexer, we'll update if kotlin is upgraded to full support one day
    KOTLIN("kotlin", new ScalaSyntaxHighlighter()),
    APEX("apex", new ApexSyntaxHighlighter()),
    XML("xml", new XmlSyntaxHighlighter()),
    XSL("xsl", new XmlSyntaxHighlighter()),
    WSDL("wsdl", new XmlSyntaxHighlighter()),
    POM("pom", new XmlSyntaxHighlighter()),
    XPATH("xpath", new XPathSyntaxHighlighter()),
    MODELICA("modelica", new ModelicaSyntaxHighlighter());


    private final String language;
    private final SyntaxHighlighter engine;


    AvailableSyntaxHighlighters(String languageTerseName, SyntaxHighlighter engine) {
        this.language = languageTerseName;
        this.engine = engine;
    }

    @Override
    public String getLanguageTerseName() {
        return engine.getLanguageTerseName();
    }

    @Override
    public StyleSpans<Collection<String>> computeHighlighting(String text) {
        return engine.computeHighlighting(text);
    }

    /**
     * Gets the highlighter for a language if available.
     *
     * @param language Language to look for
     *
     * @return A highlighter, if available
     */
    public static Optional<SyntaxHighlighter> getHighlighterForLanguage(Language language) {
        return Arrays.stream(AvailableSyntaxHighlighters.values())
                     .filter(e -> e.language.equals(language.getId()))
                     .findFirst()
                     .map(h -> h.engine);

    }
}
