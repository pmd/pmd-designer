/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import net.sourceforge.pmd.lang.AbstractLanguageVersionHandler;
import net.sourceforge.pmd.lang.BaseLanguageModule;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.ast.AstInfo;
import net.sourceforge.pmd.lang.ast.Parser;
import net.sourceforge.pmd.lang.ast.Parser.ParserTask;
import net.sourceforge.pmd.lang.ast.RootNode;
import net.sourceforge.pmd.lang.ast.SourceCodePositioner;
import net.sourceforge.pmd.lang.ast.impl.AbstractNodeWithTextCoordinates;

/**
 * Default language module used when none is on the classpath.
 */
public final class PlainTextLanguage extends BaseLanguageModule {

    public static final Language INSTANCE = new PlainTextLanguage();

    static final String TERSE_NAME = "text";

    private PlainTextLanguage() {
        super("Plain text", "Plain text", TERSE_NAME, "plain-text-file-goo-extension");
        addVersion("default", new TextLvh(), true);
    }

    private static class TextLvh extends AbstractLanguageVersionHandler {

        @Override
        public Parser getParser() {
            return PlainTextFile::new;
        }
    }

    public static class PlainTextFile extends AbstractNodeWithTextCoordinates<PlainTextFile, PlainTextFile> implements RootNode {

        private final AstInfo<PlainTextFile> astInfo;

        PlainTextFile(ParserTask task) {
            this.astInfo = new AstInfo<>(task, this);
            SourceCodePositioner positioner = new SourceCodePositioner(task.getSourceText());
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
        public String toString() {
            return "Plain text file (" + getEndLine() + " lines)";
        }

        @Override
        public AstInfo<? extends RootNode> getAstInfo() {
            return astInfo;
        }
    }

}
