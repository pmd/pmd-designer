/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.codearea;

import static net.sourceforge.pmd.util.fxdesigner.util.AstTraversalUtil.parentIterator;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerIteratorUtil.toIterable;

import java.util.Optional;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.document.TextRegion;


/**
 * Maps PMD's (line, column) coordinate system to and from the code
 * area's one-dimensional (absolute offset-based) system.
 *
 * @author Clément Fournier
 * @since 6.13.0
 */
public final class PmdCoordinatesSystem {


    private PmdCoordinatesSystem() {

    }

    public static int getRtfxParIndexFromPmdLine(int line) {
        return line - 1;
    }


    /**
     * Locates the innermost node in the given [root] that contains the
     * position at [textOffset] in the [codeArea].
     */
    public static Optional<Node> findNodeAt(Node root, int target) {
        return Optional.ofNullable(findNodeRec(root, target)).filter(it -> it.getTextRegion().contains(target));
    }


    /**
     * Simple recursive search algo. Makes the same assumptions about text bounds
     * as {@link UniformStyleCollection#toSpans()}. Then:
     * - We only have to explore one node at each level of the tree, and we quickly
     * hit the bottom (average depth of a Java AST ~20-25, with 6.x.x grammar).
     * - At each level, the next node to explore is chosen via binary search.
     */
    private static Node findNodeRec(Node subject, int target) {
        Node child = binarySearchInChildren(subject, target);
        return child == null ? subject : findNodeRec(child, target);
    }


    // returns the child of the [parent] that contains the target
    // it's assumed to be unique
    private static Node binarySearchInChildren(Node parent, int target) {

        int low = 0;
        int high = parent.getNumChildren() - 1;

        while (low <= high) {
            int mid = (low + high) / 2;
            Node child = parent.getChild(mid);
            TextRegion childRegion = child.getTextRegion();
            int cmp = Integer.compare(childRegion.getStartOffset(), target);

            if (cmp < 0) {
                // node start is before target
                low = mid + 1;
                if (childRegion.getEndOffset() >= target) {
                    // node end is after target
                    return child;
                }
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                // target is node start position
                return child; // key found
            }
        }
        return null;  // key not found
    }


    /**
     * Returns the innermost node that covers the entire given text range
     * in the given tree.
     *
     * @param root  Root of the tree
     * @param range Range to find
     * @param exact If true, will return the *outermost* node whose range
     *              is *exactly* the given text range, otherwise it may be larger.
     */
    public static Optional<Node> findNodeCovering(Node root, TextRegion range, boolean exact) {
        return findNodeAt(root, range.getStartOffset()).map(innermost -> {
            for (Node parent : toIterable(parentIterator(innermost, true))) {
                TextRegion parentRange = parent.getTextRegion();
                if (!exact && parentRange.contains(range)) {
                    return parent;
                } else if (exact && parentRange.equals(range)) {
                    // previously this used node streams to get the highest node
                    // on a single child path
                    return parent;
                } else if (exact && parentRange.contains(range)) {
                    // if it isn't the same, then we can't find better so better stop looking
                    return null;
                }
            }
            return null;
        });
    }

}
