/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app.services;

import org.reactfx.value.Val;

import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.ast.Node;

/**
 * Logs events. Stores the whole log in case no view was open.
 *
 * @author Clément Fournier
 * @since 6.13.0
 */
public interface GlobalStateHolder {

    /**
     * Returns the compilation unit of the main editor. Empty if the source
     * is unparsable.
     */
    Val<Node> globalCompilationUnitProperty();


    /**
     * Returns the language version selected on the app. Never empty.
     */
    Val<LanguageVersion> globalLanguageVersionProperty();


    default LanguageVersion getGlobalLanguageVersion() {
        return globalLanguageVersionProperty().getValue();
    }


}
