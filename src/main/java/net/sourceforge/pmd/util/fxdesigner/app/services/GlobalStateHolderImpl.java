/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app.services;

import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.util.LanguageRegistryUtil;

/**
 * @author Clément Fournier
 */
public class GlobalStateHolderImpl implements GlobalStateHolder {

    private final Var<Node> globalCompilationUnit = Var.newSimpleVar(null);
    private final Var<LanguageVersion> globalLanguageVersion = Var.newSimpleVar(LanguageRegistryUtil.defaultLanguageVersion());


    @Override
    public Var<Node> writableGlobalCompilationUnitProperty() {
        return globalCompilationUnit;
    }

    @Override
    public Var<LanguageVersion> writeableGlobalLanguageVersionProperty() {
        return globalLanguageVersion;
    }
}
