/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app.services;

import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;

/**
 * @author Clément Fournier
 */
public class GlobalStateHolderImpl implements GlobalStateHolder {

    private final Var<Node> globalCompilationUnit = Var.newSimpleVar(null);
    private final Var<LanguageVersion> globalLanguageVersion = Var.newSimpleVar(DesignerUtil.defaultLanguageVersion());

    @Override
    public Var<Node> globalCompilationUnitProperty() {
        return globalCompilationUnit;
    }

    @Override
    public Var<LanguageVersion> globalLanguageVersionProperty() {
        return globalLanguageVersion;
    }
}
