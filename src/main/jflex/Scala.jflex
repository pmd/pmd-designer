package net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting;

import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.commons.lang3.mutable.MutableInt;

import net.sourceforge.pmd.util.fxdesigner.util.codearea.LexerBasedHighlighter.JflexLexer;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.HighlightClasses.*;

%%


%class      ScalaLexer
%implements JflexLexer
%function   nextSpan
%type	    HighlightClasses
%unicode

%{


    private class InterpolationLevel extends MutableInt {

          private final int state;
          InterpolationLevel() {
              this.state = yystate();
          }

          int getState() {
              return state;
          }
    }

    //to get id after $ in interpolated String
    private boolean haveIdInString = false;
    private boolean haveIdInMultilineString = false;
    // Currently opened interpolated Strings. Each int represents the number of the opened left structural braces in the String
    private Deque<InterpolationLevel> nestedString = new ArrayDeque<>();

    private boolean isInterpolatedStringState() {
        return shouldProcessBracesForInterpolated() ||
               haveIdInString ||
               haveIdInMultilineString ||
               yystate() == INSIDE_INTERPOLATED_STRING ||
               yystate() == INSIDE_MULTI_LINE_INTERPOLATED_STRING;
    }

    private boolean shouldProcessBracesForInterpolated() {
      return !nestedString.isEmpty();
    }

    private HighlightClasses processOutsideString() {
      if (shouldProcessBracesForInterpolated()) nestedString.pop();
      yybegin(COMMON_STATE);
      return process(STRING);
    }

    private HighlightClasses process(HighlightClasses type){
      if ((type == IDENTIFIER || type == THIS)) {
        if (haveIdInString) {
          haveIdInString = false;
          yybegin(INSIDE_INTERPOLATED_STRING);
        } else if (haveIdInMultilineString) {
          haveIdInMultilineString = false;
          yybegin(INSIDE_MULTI_LINE_INTERPOLATED_STRING);
        }
      }

      if (yystate() == YYINITIAL && type != WHITESPACE && yycharat(0) != '{' && yycharat(0) != '(') {
        yybegin(COMMON_STATE);
      }

      return type;
    }

    private HighlightClasses processInsideString(boolean isInsideMultiline) {
        boolean isEscape = yycharat(1) == '$';
        if (!isEscape) {
            if (isInsideMultiline) {
                haveIdInMultilineString = true;
            } else {
                haveIdInString = true;
            }
            yybegin(INJ_COMMON_STATE);
        }

        yypushback(yylength() - 1 - (isEscape ? 1 : 0));
        return process(isEscape ? STRING : INTERPOLATED_IDENTIFIER);
    }

%}


%state  COMMON_STATE
%xstate WAIT_FOR_INTERPOLATED_STRING
%xstate INSIDE_INTERPOLATED_STRING
%xstate INSIDE_MULTI_LINE_INTERPOLATED_STRING
%xstate INJ_COMMON_STATE

keyword =  "case" | "catch" | "def" | "do" | "else" | "extends" | "finally" | "for" |
           "if" | "match" | "new" | "requires" | "return" |
           "super" | "throw" | "try" | "val" | "var" | "while" | "with" | "yield" | "trait" | "class" | "enum" |
           "object" | "package" | "import"

modifier = "abstract" | "final" | "forSome" | "implicit" | "lazy" | "override" | "private" | "protected" | "sealed"

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////      integers and floats     /////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

longLiteral =    {integerLiteral} [Ll]
integerLiteral = {decimalNumeral} | {hexNumeral} | {octalNumeral}
decimalNumeral = 0 | [1-9] {digitOrUnderscore}*
hexNumeral =     0 [Xx]  [_0-9A-Fa-f]+
octalNumeral =   0 [_0-7]+
digitOrUnderscore = [_0-9]

doubleLiteral   = ({floatingDecimalNumber} [Dd]?) | ({fractionPart} [Dd])
floatingLiteral = ({floatingDecimalNumber} | {fractionPart}) [Ff]

floatingDecimalNumber = {digits} "." {digits}? {exponentPart}?
          | "." {fractionPart}
          | {digits} {exponentPart}

digits = [0-9] {digitOrUnderscore}*
exponentPart = [Ee] [+-]? {digits}
fractionPart = {digits} {exponentPart}?

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////      identifiers      ////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

identifier = {plainid} | {backtick_ident}
backtick_ident =  "`" {stringLiteralExtra} "`"

special = \u0021 | \u0023
          | [\u0025-\u0026]
          | [\u002A-\u002B]
          | \u002D | \u005E
          | \u003A
          | [\u003C-\u0040]
          | \u007E
          | \u005C | \u002F | [:unicode_math_symbol:] | [:unicode_other_symbol:] | \u2694


// Vertical line
op = \u007C ({special} | \u007C)+
     | {special} ({special} | \u007C)*
octalDigit = [0-7]

idrest1 = [:jletter:]? [:jletterdigit:]* ("_" {op})?
idrest = [:jletter:]? [:jletterdigit:]* ("_" {op} | "_" {idrest1} )?
varid = [:jletter:] {idrest}

plainid = {varid} | {op}


charEscapeSeq = \\[^\r\n]
charExtra = !( ![^`] | \R)             //This is for `type` identifiers
stringElementExtra = {charExtra} | {charEscapeSeq}
stringLiteralExtra = {stringElementExtra}*
symbolLiteral = "\'" {plainid}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////// Comments ////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

END_OF_LINE_COMMENT="/""/"[^\r\n]*
SH_COMMENT="#!" [^]* "!#" | "::#!" [^]* "::!#"

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////// String & chars //////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

hexDigit = [0-9A-Fa-f]
escapeSequence=\\[^\r\n]
unicodeEscape=!(!(\\u{hexDigit}{hexDigit}{hexDigit}{hexDigit}) | \\u000A)
octalEscape=\\{octalDigit} {octalDigit}? {octalDigit}?
charLiteral="'"([^\\\'\r\n]|{escapeSequence}|{unicodeEscape}|{octalEscape})("'"|\\) | \'\\u000A\' | "'''"

STRING_BEGIN = \"([^\\\"\r\n]|{escapeSequence})*
STRING_LITERAL={STRING_BEGIN} \"
MULTI_LINE_STRING = \"\"\" ( (\"(\")?)? [^\"] )* \"\"\" (\")* // Multi-line string

////////String Interpolation////////
INTERPOLATED_STRING_ID = {varid}

INTERPOLATED_STRING_BEGIN = \"([^\\\"\r\n\$]|{escapeSequence})*
INTERPOLATED_STRING_PART = ([^\\\"\r\n\$]|{escapeSequence})+

INTERPOLATED_MULTI_LINE_STRING_BEGIN = \"\"\" ( (\"(\")?)? [^\"\$] )*
INTERPOLATED_MULTI_LINE_STRING_PART = ( (\"(\")?)? [^\"\$] )+
WRONG_STRING = {STRING_BEGIN}


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////// Common symbols //////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

WhiteSpace = [ \t\f]
NewLines = \R (\R | {WhiteSpace})*

XML_BEGIN = "<" ("_" | [:jletter:]) | "<!--" | "<?" ("_" | [:jletter:]) | "<![CDATA["

%%


<YYINITIAL>{

{XML_BEGIN}                             {   yybegin(COMMON_STATE);
                                            yypushback(yylength());
                                            return PUNCTUATION;
                                        }
}

{END_OF_LINE_COMMENT}                   { return process(SINGLEL_COMMENT); }
{SH_COMMENT}                            { return process(SINGLEL_COMMENT); }
"/*" ~ "*/"                             { return process(MULTIL_COMMENT); }

{INTERPOLATED_STRING_ID} / ({INTERPOLATED_STRING_BEGIN} | {INTERPOLATED_MULTI_LINE_STRING_BEGIN}) {
  yybegin(WAIT_FOR_INTERPOLATED_STRING);
  if (yytext().endsWith("\"\"")) yypushback(2);
  return process(haveIdInString || haveIdInMultilineString ? IDENTIFIER : INTERPOLATED_IDENTIFIER);
}

<WAIT_FOR_INTERPOLATED_STRING> {
  {INTERPOLATED_STRING_BEGIN}            { yybegin(INSIDE_INTERPOLATED_STRING); nestedString.push(new InterpolationLevel()); return process(STRING); }
  {INTERPOLATED_MULTI_LINE_STRING_BEGIN} { yybegin(INSIDE_MULTI_LINE_INTERPOLATED_STRING); nestedString.push(new InterpolationLevel()); return process(STRING); }
}

<INJ_COMMON_STATE> {identifier} {
      int length = yylength();
      int number = length;
      for (int i = 1; i < length; i++) {
        if (yycharat(i) == '$') {
          number = i;
          break;
        }
      }

      yypushback(length - number);
      boolean isThis = "this".contentEquals(yytext());
      return process(isThis ? THIS : IDENTIFIER);
}

<INJ_COMMON_STATE> [^]           { return process(BAD_CHAR); }

<INSIDE_INTERPOLATED_STRING> {
  "$$"                                  { return process(STRING); }
  {INTERPOLATED_STRING_PART}            { return process(STRING); }
  "$"{identifier}                       { return processInsideString(false); }
  \"                                    { return processOutsideString(); }
  "$" / "{"                             { yybegin(COMMON_STATE); return process(OPERATOR); }
  \R                                    { yybegin(COMMON_STATE); return process(BAD_CHAR); }
  [^]                                   { return process(BAD_CHAR); }
}

<INSIDE_MULTI_LINE_INTERPOLATED_STRING> {
  "$$"                                  { return process(STRING); }
  (\"\") / "$"                          { return process(STRING); }
  {INTERPOLATED_MULTI_LINE_STRING_PART} { return process(STRING); }
  "$"{identifier}                       { return processInsideString(true); }
  \"\"\" (\")+                          { yypushback(yylength() - 1); return process(STRING); }
  \"\"\"                                { return processOutsideString(); }
  "$" / "{"                             { yybegin(COMMON_STATE); return process(OPERATOR); }
  \" / [^\"]                            { return process(STRING); }
  [^]                                   { return process(BAD_CHAR); }
}



{STRING_LITERAL}                         { return process(STRING);  }
{MULTI_LINE_STRING}                      { return process(STRING);  }
{WRONG_STRING}                           { return process(BAD_CHAR);  }
{symbolLiteral}                          { return process(SYMBOL);  }
{charLiteral}                            { return process(CHAR);  }


"{"                                     {   if (shouldProcessBracesForInterpolated()) { nestedString.getFirst().increment(); } return process(PUNCTUATION); }
"}"                                     {   if (shouldProcessBracesForInterpolated()) {
                                              InterpolationLevel level = nestedString.getFirst();
                                              level.decrement();

                                              if (level.getValue() == 0) {
                                                yybegin(level.getState());
                                              }
                                            }
                                            return process(PUNCTUATION);
                                        }

"("                                     {   return process(PUNCTUATION); }
")"                                     {   return process(PUNCTUATION); }



{keyword}                               {   return process(KEYWORD); }
{modifier}                              {   return process(MODIFIER); }
"false" | "true"                        {   return process(BOOLEAN); }
"null"                                  {   return process(NULL); }
"this"                                  {   return process(THIS); }

  "*" | "?" | ":"
//| "<:" | ">:" | "<%" | "#"
| "&" | "|" | "+" | "-" | "~"
| "!" | "." | ";" | "," | "[" | "]"     {   return process(PUNCTUATION); }

"=" | "=>" | "\\u21D2" | "\u21D2"
"_" | "<-" | "\\u2190" | "\u2190"       {   return process(OPERATOR); }

"@" {identifier} ("." {identifier})*    {   return process(ANNOTATION); }

[:uppercase:]{idrest}                   {   return process(CLASS_IDENTIFIER); }
{varid}                                 {   return process(IDENTIFIER); }
{op} | {backtick_ident}                 {   return process(OPERATOR); }
{identifier}                            {   return process(IDENTIFIER); }
{integerLiteral} / "." {identifier}     {   return process(NUMBER);  }
  {doubleLiteral}
| {floatingLiteral}
| {longLiteral}
| {integerLiteral}                      {   return process(NUMBER);  }

{WhiteSpace}                            {   yybegin(YYINITIAL); return process(WHITESPACE);  }
{NewLines}                              {   yybegin(YYINITIAL); return process(WHITESPACE); }

.                                       {   return process(TEXT); }
