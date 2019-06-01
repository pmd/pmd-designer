/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;

import org.apache.commons.lang3.StringUtils;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.util.beans.PropertyUtils;

public final class DumpUtil {


    private DumpUtil() {

    }


    // returns null if the value is unsupported
    private static String valueToString(Object value) {
        if (value instanceof String) {
            String stringVal;
            stringVal = value.toString();
            stringVal = stringVal.replaceAll("\"", "\\\"");
            // escape kt string interpolators
            stringVal = stringVal.replaceAll("\\$(?=[a-zA-Z{])", "\\${'\\$'}");
            return '"' + stringVal + '"';
        } else if (value instanceof Character) {
            return '\'' + value.toString().replaceAll("'", "\\'") + '\'';
        } else if (value instanceof Enum) {
            return ((Enum) value).getDeclaringClass().getCanonicalName() + "." + ((Enum) value).name();
        } else if (value instanceof Class) {
            return ((Class) value).getCanonicalName() + "::class.java";
        } else if (value instanceof Number || value instanceof Boolean || value == null) {
            return String.valueOf(value);
        }
        return null;
    }


    private static void subtreeToNodeTest(Node node, StringBuilder builder, boolean isChild, int indentDepth) {
        // indentDepth is the indent level of the outer block, assertions have +4 depth
        int bodyIndent = indentDepth + 4;

        if (isChild) {
            builder.append("child<").append(node.getClass().getSimpleName()).append("> {");
        } else {
            builder.append("<").append(node.getClass().getSimpleName()).append("> {");
        }

        String[] childrenProps = new String[node.jjtGetNumChildren()];

        for (PropertyDescriptor prop : PropertyUtils.getPropertyDescriptors(node.getClass()).values()) {
            if (prop.getReadMethod() != null && Modifier.isPublic(prop.getReadMethod().getModifiers())
                && prop.getReadMethod().getDeclaringClass() == node.getClass()) {

                Object value;
                try {
                    value = prop.getReadMethod().invoke(node);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    continue;
                }

                String stringVal = valueToString(value);
                if (stringVal != null) {

                    // take care of Kotlin property access conventions
                    String propName = value instanceof Boolean && prop.getReadMethod().getName().startsWith("is")
                                      ? prop.getReadMethod().getName()
                                      : prop.getName();

                    // Filter this one out
                    if ("XPathNodeName".equals(propName)) {
                        continue;
                    }

                    newLine(builder, bodyIndent)
                        .append("it.")
                        .append(StringUtils.uncapitalize(propName))
                        .append(" shouldBe ")
                        .append(stringVal);

                } else if (value instanceof Node) {
                    // The property may give access to a child, in which case we'll use the child call later

                    for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                        Node child = node.jjtGetChild(i);
                        if (value.equals(child)) {
                            // The array contains name of corresponding properties
                            childrenProps[i] = prop.getName();
                            break;
                        }
                    }
                } else {
                    newLine(builder, bodyIndent)
                        .append("// it.").append(prop.getName());
                }
            }
        }
        if (node.jjtGetNumChildren() > 0) {
            for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                Node c = node.jjtGetChild(i);
                newLine(builder, 0);
                newLine(builder, bodyIndent);
                // We have to write them in child order
                if (childrenProps[i] != null) {
                    builder.append("it.").append(childrenProps[i]).append(" shouldBe ");
                }
                subtreeToNodeTest(c, builder, true, bodyIndent);
            }
        }
        newLine(builder, indentDepth).append("}");
    }


    /**
     * Dumps the entire subtree of a node to a Kotlin AST matcher.
     *
     * @param node Node to dump
     *
     * @return A string that can be copy pasted into a kotlin test file
     */
    public static String dumpToSubtreeTest(Node node) {
        StringBuilder sb = new StringBuilder();
        subtreeToNodeTest(node, sb, false, 0);
        String result = sb.toString();
        // some preliminary formatting
        result = result.replaceAll("\\{\\s*+} ", "{}");

        return result;
    }

    private static StringBuilder newLine(StringBuilder builder, int indentDepth) {
        return builder.append("\n").append(StringUtils.repeat(' ', indentDepth));
    }

}
