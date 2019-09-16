/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.lang.AbstractLanguageVersionHandler;
import net.sourceforge.pmd.lang.BaseLanguageModule;
import net.sourceforge.pmd.lang.Parser;
import net.sourceforge.pmd.lang.ParserOptions;
import net.sourceforge.pmd.lang.TokenManager;
import net.sourceforge.pmd.lang.ast.AbstractNode;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.ParseException;
import net.sourceforge.pmd.lang.ast.RootNode;
import net.sourceforge.pmd.lang.ast.SourceCodePositioner;
import net.sourceforge.pmd.lang.rule.AbstractRuleViolationFactory;
import net.sourceforge.pmd.lang.rule.ParametricRuleViolation;
import net.sourceforge.pmd.lang.rule.RuleViolationFactory;

/**
 * Default language module used when none is on the classpath.
 */
public final class PlainTextLanguage extends BaseLanguageModule {

    public PlainTextLanguage() {
        super("Plain text", "Plain text", "text", null);
        addVersion("default", new TextLvh(), true);
    }

    @Override
    public List<String> getExtensions() {
        return super.getExtensions();
    }

    private static class TextLvh extends AbstractLanguageVersionHandler {

        private static final RuleViolationFactory RV_FACTORY = new AbstractRuleViolationFactory() {
            @Override
            protected RuleViolation createRuleViolation(Rule rule, RuleContext ruleContext, Node node, String s) {
                return new ParametricRuleViolation<>(rule, ruleContext, node, s);
            }

            @Override
            protected RuleViolation createRuleViolation(Rule rule, RuleContext ruleContext, Node node, String s, int i, int i1) {
                return new ParametricRuleViolation<>(rule, ruleContext, node, s);
            }
        };

        @Override
        public RuleViolationFactory getRuleViolationFactory() {
            return RV_FACTORY;
        }

        @Override
        public Parser getParser(ParserOptions parserOptions) {
            return new Parser() {
                @Override
                public ParserOptions getParserOptions() {
                    return parserOptions;
                }

                @Override
                public TokenManager getTokenManager(String s, Reader reader) {
                    return null;
                }

                @Override
                public boolean canParse() {
                    return true;
                }

                @Override
                public Node parse(String s, Reader reader) throws ParseException {
                    try {
                        return new PlainTextFile(IOUtils.toString(reader));
                    } catch (IOException e) {
                        throw new ParseException(e);
                    }
                }

                @Override
                public Map<Integer, String> getSuppressMap() {
                    return Collections.emptyMap();
                }
            };
        }
    }

    private static class PlainTextFile extends AbstractNode implements RootNode {

        PlainTextFile(String fileText) {
            super(0);
            SourceCodePositioner positioner = new SourceCodePositioner(fileText);
            this.beginLine = 1;
            this.beginColumn = 1;
            this.endLine = positioner.getLastLine();
            this.endColumn = positioner.getLastLineColumn();
        }
    }
}
