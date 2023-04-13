/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.rule.xpath.XPathVersion;
import net.sourceforge.pmd.lang.rule.xpath.internal.DeprecatedAttrLogger; // NOPMD
import net.sourceforge.pmd.lang.rule.xpath.internal.SaxonXPathRuleQuery; // NOPMD
import net.sourceforge.pmd.properties.PropertyDescriptor;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;


/**
 * Evaluates XPath expressions.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public final class XPathEvaluator {


    private XPathEvaluator() {

    }

    /**
     * Evaluates the query with default parameters on the global compilation
     * unit and with the global language version. This method hides errors.
     *
     * @return The results, or an empty list if there was an error
     */
    public static List<Node> simpleEvaluate(DesignerRoot root, String query) {
        return root.getService(DesignerRoot.AST_MANAGER)
                   .compilationUnitProperty()
                   .getOpt()
                   .map(n -> {
                       try {
                           return evaluateQuery(n,
                                                XPathVersion.DEFAULT,
                                                query,
                                                emptyMap(),
                                                emptyList());
                       } catch (XPathEvaluationException e) {
                           e.printStackTrace();
                           return Collections.<Node>emptyList();
                       }
                   })
                   .orElse(Collections.emptyList());
    }

    /**
     * Evaluates an XPath query on the compilation unit. Performs
     * no side effects.
     *
     * @param compilationUnit AST root
     * @param xpathVersion    XPath version
     * @param xpathQuery      XPath query
     * @param properties      Properties of the rule
     *
     * @throws XPathEvaluationException if there was an error during the evaluation. The cause is preserved
     */
    public static List<Node> evaluateQuery(Node compilationUnit,
                                           XPathVersion xpathVersion,
                                           String xpathQuery,
                                           Map<String, String> propertyValues,
                                           List<PropertyDescriptorSpec> properties) throws XPathEvaluationException {

        if (StringUtils.isBlank(xpathQuery)) {
            return emptyList();
        }

        try {

            Map<String, ? extends PropertyDescriptor<?>> descriptors = properties.stream().collect(Collectors.toMap(PropertyDescriptorSpec::getName, PropertyDescriptorSpec::build));
            Map<PropertyDescriptor<?>, Object> allProperties =
                propertyValues.entrySet().stream()
                              .collect(Collectors.toMap(e -> descriptors.get(e.getKey()), e -> descriptors.get(e.getKey()).valueFrom(e.getValue())));

            SaxonXPathRuleQuery xpathRule =
                new SaxonXPathRuleQuery(
                    xpathQuery,
                    xpathVersion,
                    allProperties,
                    compilationUnit.getAstInfo().getLanguageProcessor().services().getXPathHandler(),
                    DeprecatedAttrLogger.noop()
                );

            return xpathRule.evaluate(compilationUnit);

        } catch (RuntimeException e) {
            throw new XPathEvaluationException(e);
        }
    }
}
