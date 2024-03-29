/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.codearea;

import java.util.Collection;
import java.util.Comparator;

import org.fxmisc.richtext.model.StyledDocument;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.document.TextRegion;
import net.sourceforge.pmd.util.fxdesigner.util.TextAwareNodeWrapper;


/**
 * Wrapper around a node used to declutter the layering algorithm with
 * convenience methods. The point is that it's aware of the code area.
 * See also {@link #snapshot()}.
 *
 * @author Clément Fournier
 * @since 6.5.0
 */
final class NodeStyleSpan {

    private static final Comparator<NodeStyleSpan> COMPARATOR = Comparator.comparing(NodeStyleSpan::getNode, Comparator.comparingInt(Node::getBeginLine).thenComparing(Node::getBeginColumn));
    private final Node node;
    private final SyntaxHighlightingCodeArea codeArea;


    private NodeStyleSpan(Node node, SyntaxHighlightingCodeArea codeArea) {
        this.node = node;
        this.codeArea = codeArea;
    }


    /** Gets the underlying node. */
    public Node getNode() {
        return node;
    }


    /**
     * Snapshots the absolute coordinates of the node in the code area
     * for the duration of the layering algorithm.
     */
    // TODO I don't think there's any good reason for this laziness,
    // if anything, it may cause trouble if the layering algorithm uses
    // a snapshot taken too late, with outdated line and column coordinates
    // I originally wrote it like that because I didn't think enough about it,
    // and I don't have time to simplify it before 6.5.0
    public PositionSnapshot snapshot() {
        TextRegion region = node.getTextRegion();
        int lastKnownStart = region.getStartOffset();
        int lastKnownEnd = region.getEndOffset();
        return new PositionSnapshot(lastKnownStart, lastKnownEnd);
    }


    @Override
    public String toString() {
        return node.getXPathNodeName() + "@" + snapshot();
    }


    /**
     * Returns a comparator that orders spans according to the start
     * index of the node they wrap.
     */
    public static Comparator<NodeStyleSpan> documentOrderComparator() {
        return COMPARATOR;
    }


    /** Builds a new node style span. */
    public static NodeStyleSpan fromNode(Node node, SyntaxHighlightingCodeArea codeArea) {
        return new NodeStyleSpan(node, codeArea);
    }


    /**
     * Snapshot of the node's absolute position in the code area.
     */
    class PositionSnapshot implements TextAwareNodeWrapper {
        private final int beginIndex; // inclusive
        private final int endIndex; // exclusive


        PositionSnapshot(int beginIndex, int endIndex) {
            this.beginIndex = beginIndex;
            this.endIndex = endIndex;
        }


        @Override
        public String toString() {
            // debug only
            return getNodeText() + "@[" + beginIndex + "," + endIndex + ']';
        }


        @Override
        public String getNodeText() {
            return codeArea.getText(beginIndex, endIndex);
        }


        @Override
        public StyledDocument<Collection<String>, String, Collection<String>> getNodeRichText() {
            StyledDocument<Collection<String>, String, Collection<String>> richtText;
            richtText = codeArea.subDocument(beginIndex, endIndex);
            return richtText;
        }


        int getBeginIndex() {
            return beginIndex;
        }


        int getEndIndex() {
            return endIndex;
        }


        int getLength() {
            return endIndex - beginIndex;
        }


        @Override
        public Node getNode() {
            return NodeStyleSpan.this.getNode();
        }
    }
}
