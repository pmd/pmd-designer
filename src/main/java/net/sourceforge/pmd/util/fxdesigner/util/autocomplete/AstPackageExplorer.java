/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.autocomplete;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.util.fxdesigner.util.ResourceUtil;


/**
 * Finds XPath node names by looking into the classpath
 * directory corresponding to the AST of a language. This
 * is ok for Java, Apex, etc. but not e.g. for XML.
 */
class AstPackageExplorer implements NodeNameFinder {
    private final List<String> availableNodeNames;


    AstPackageExplorer(Language language) {
        availableNodeNames =
            ResourceUtil.getClassesInPackage("net.sourceforge.pmd.lang." + language.getTerseName() + ".ast")
                        .filter(clazz -> clazz.getSimpleName().startsWith("AST"))
                        .filter(clazz -> !clazz.isInterface() && !Modifier.isAbstract(clazz.getModifiers()))
                        .map(m -> m.getSimpleName().substring("AST".length()))
                        .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));

    }


    @Override
    public List<String> getNodeNames() {
        return availableNodeNames;
    }

    // TODO move to some global Util

}
