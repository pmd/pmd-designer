/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app.services;

import java.util.Map;

import org.reactfx.value.SuspendableVar;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.document.TextDocument;
import net.sourceforge.pmd.util.fxdesigner.SourceEditorController;
import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.model.ParseAbortedException;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;


/**
 * Manages a compilation unit for {@link SourceEditorController}.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public interface ASTManager extends ApplicationComponent, SettingsOwner {


    SuspendableVar<String> sourceCodeProperty();

    SuspendableVar<TextDocument> sourceDocumentProperty();


    String getSourceCode();

    TextDocument getSourceDocument();


    void setSourceCode(String sourceCode);


    Val<LanguageVersion> languageVersionProperty();


    Val<Node> compilationUnitProperty();


    Val<ClassLoader> classLoaderProperty();


    Val<ParseAbortedException> currentExceptionProperty();


    Var<Map<String, String>> ruleProperties();

}
