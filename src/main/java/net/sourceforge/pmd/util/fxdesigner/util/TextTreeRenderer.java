/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import java.io.IOException;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.properties.AbstractPropertySource;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import net.sourceforge.pmd.properties.PropertyFactory;
import net.sourceforge.pmd.properties.PropertySource;
import net.sourceforge.pmd.util.treeexport.TreeRenderer;
import net.sourceforge.pmd.util.treeexport.TreeRendererDescriptor;


public class TextTreeRenderer implements TreeRenderer {

    public static final TreeRendererDescriptor DESCRIPTOR = new TreeRendererDescriptor() {

        private final PropertyDescriptor<Boolean> onlyAscii =
            PropertyFactory.booleanProperty("onlyAsciiChars")
                           .defaultValue(false)
                           .desc("Use only ASCII characters in the structure")
                           .build();

        private final PropertyDescriptor<Integer> maxLevel =
            PropertyFactory.intProperty("maxLevel")
                           .defaultValue(-1)
                           .desc("Max level on which to recurse. Negative means unbounded")
                           .build();

        @Override
        public PropertySource newPropertyBundle() {

            PropertySource bundle = new AbstractPropertySource() {
                @Override
                protected String getPropertySourceType() {
                    return "tree renderer";
                }

                @Override
                public String getName() {
                    return "text";
                }
            };

            bundle.definePropertyDescriptor(onlyAscii);
            bundle.definePropertyDescriptor(maxLevel);

            return bundle;
        }

        @Override
        public String id() {
            return "text";
        }

        @Override
        public String description() {
            return "Text renderer";
        }

        @Override
        public TreeRenderer produceRenderer(PropertySource properties) {

            Strings str = properties.getProperty(onlyAscii) ? ASCII : UNICODE;

            return new TextTreeRenderer(str, properties.getProperty(maxLevel));
        }
    };

    private static final Strings ASCII = new Strings(
        "+- ",
        "+- ",
        "|  ",
        "   "
    );
    private static final Strings UNICODE = new Strings(
        "└─ ",
        "├─ ",
        "│  ",
        "   "
    );

    private final Strings str;
    private final int maxLevel;

    public TextTreeRenderer(Strings str, int maxLevel) {
        this.str = str;
        this.maxLevel = maxLevel;
    }

    @Override
    public void renderSubtree(Node node, Appendable out) throws IOException {
        printInnerNode(node, out, 0, "", true);
    }

    private String childPrefix(String prefix, boolean isTail) {
        return prefix + (isTail ? str.gap : str.verticalEdge);
    }


    protected void appendIndent(Appendable out, String prefix, boolean isTail) throws IOException {
        out.append(prefix).append(isTail ? str.tailFork : str.fork);
    }


    private void printInnerNode(Node node,
                                Appendable out,
                                int level,
                                String prefix,
                                boolean isTail) throws IOException {

        appendIndent(out, prefix, isTail);
        out.append(node.getXPathNodeName()).append("\n");

        if (level == maxLevel) {
            if (node.getNumChildren() > 0) {
                appendBoundaryForNode(node, out, prefix, isTail);
            }
        } else {
            int n = node.getNumChildren() - 1;
            String childPrefix = childPrefix(prefix, isTail);
            for (int i = 0; i < node.getNumChildren(); i++) {
                Node child = node.getChild(i);
                printInnerNode(child, out, level + 1, childPrefix, i == n);
            }
        }
    }

    protected void appendBoundaryForNode(Node node, Appendable out, String prefix, boolean isTail) throws IOException {
        appendIndent(out, childPrefix(prefix, isTail), true);

        if (node.getNumChildren() == 1) {
            out.append("1 child is not shown");
        } else {
            out.append(String.valueOf(node.getNumChildren())).append(" children are not shown");
        }

        out.append('\n');
    }

    private static class Strings {


        private final String tailFork;
        private final String fork;
        private final String verticalEdge;
        private final String gap;


        private Strings(String tailFork, String fork, String verticalEdge, String gap) {
            this.tailFork = tailFork;
            this.fork = fork;
            this.verticalEdge = verticalEdge;
            this.gap = gap;
        }
    }

}
