/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;

import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.RuleViolation;
import net.sourceforge.pmd.lang.AbstractLanguageVersionHandler;
import net.sourceforge.pmd.lang.BaseLanguageModule;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.Parser;
import net.sourceforge.pmd.lang.ParserOptions;
import net.sourceforge.pmd.lang.TokenManager;
import net.sourceforge.pmd.lang.ast.AbstractNode;
import net.sourceforge.pmd.lang.ast.AstProcessingStage;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.ParseException;
import net.sourceforge.pmd.lang.ast.RootNode;
import net.sourceforge.pmd.lang.ast.SourceCodePositioner;
import net.sourceforge.pmd.lang.rule.AbstractRuleChainVisitor;
import net.sourceforge.pmd.lang.rule.AbstractRuleViolationFactory;
import net.sourceforge.pmd.lang.rule.ParametricRuleViolation;
import net.sourceforge.pmd.lang.rule.RuleViolationFactory;

/**
 * Default language module used when none is on the classpath.
 */
public final class PlainTextLanguage extends BaseLanguageModule {

    public static final Language INSTANCE = new PlainTextLanguage();

    static final String TERSE_NAME = "text";

    private PlainTextLanguage() {
        super("Plain text", "Plain text", TERSE_NAME, RchainVisitor.class, "plain-text-file-goo-extension");
        addVersion("default", new TextLvh(), true);
    }

    @SuppressWarnings({"PMD.MissingOverride"})
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
        public List<? extends AstProcessingStage<?>> getProcessingStages() {
            return Collections.emptyList();
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
                public Node parse(String s, Reader reader) throws ParseException {
                    try {
                        return new PlainTextFile(IOUtils.toString(reader));
                    } catch (IOException e) {
                        throw new ParseException(e);
                    }
                }
            };
        }
    }

    public static class PlainTextFile extends AbstractNode implements RootNode {

        PlainTextFile(String fileText) {
            super(0);
            SourceCodePositioner positioner = new SourceCodePositioner(fileText);
            this.beginLine = 1;
            this.beginColumn = 1;
            this.endLine = positioner.getLastLine();
            this.endColumn = positioner.getLastLineColumn();
        }

        @Override
        public String getXPathNodeName() {
            return "TextFile";
        }

        @Override
        public String getImage() {
            return null;
        }

        @Override
        public void setImage(String image) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeChildAtIndex(int childIndex) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public String toString() {
            return "Plain text file (" + endLine + "lines)";
        }
    }

    public static class RchainVisitor extends AbstractRuleChainVisitor {

        @Override
        protected void visit(Rule rule, Node node, RuleContext ctx) {
            rule.apply(Collections.singletonList(node), ctx);
        }

        @Override
        protected void indexNodes(List<Node> nodes, RuleContext ctx) {
            // there's a single node...
        }
    }
}
