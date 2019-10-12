/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting;

import static net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.HighlightClasses.BOOLEAN;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.HighlightClasses.BRACE;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.HighlightClasses.BRACKET;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.HighlightClasses.IDENTIFIER;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.HighlightClasses.KEYWORD;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.HighlightClasses.MULTIL_COMMENT;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.HighlightClasses.NUMBER;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.HighlightClasses.PAREN;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.HighlightClasses.SEMICOLON;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.HighlightClasses.SINGLEL_COMMENT;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.HighlightClasses.STRING;

import java.util.regex.Pattern;

import net.sourceforge.pmd.util.fxdesigner.util.codearea.SimpleRegexSyntaxHighlighter;

public class ModelicaSyntaxHighlighter extends SimpleRegexSyntaxHighlighter {

    private static final String[] KEYWORDS = {
        "import", "within", "encapsulated", "partial", "final",
        "class", "model", "operator", "record", "block", "expandable",
        "connector", "type", "package", "pure", "impure", "function",
        "extends", "end", "enumeration", "public", "protected", "external",
        "redeclare", "inner", "outer", "replaceable", "constrainedby",
        "flow", "stream", "discrete", "parameter", "constant", "input",
        "output", "der", "connect", "if", "each", "initial", "equation",
        "algorithm", "annotation", "break", "return", "then", "elseif",
        "else", "for", "loop", "in", "while", "when", "elsewhen", "or",
        "and", "not", "true", "false",
    };

    // based on Java highlighter
    private static final RegexHighlightGrammar GRAMMAR
        = grammarBuilder(SINGLEL_COMMENT.css, "//[^\n]*")
        .or(MULTIL_COMMENT.css, "/\\*.*?\\*/")
        .or(PAREN.css, "[()]")
        .or(NUMBER.css, asWord("\\d[_\\d]*+(\\.\\d(_?\\d)*+)?[fdlFDL]?"))
        .or(BRACE.css, "[{}]")
        .or(BRACKET.css, "[\\[]]")
        .or(SEMICOLON.css, ";")
        .or(KEYWORD.css, alternation(KEYWORDS))
        .or(STRING.css, "\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"")
        .or(BOOLEAN.css, asWord("true|false"))
        .or(IDENTIFIER.css, asWord("[\\w_$]+"))
        .create(Pattern.DOTALL);

    public ModelicaSyntaxHighlighter() {
        super("modelica", GRAMMAR);
    }
}
