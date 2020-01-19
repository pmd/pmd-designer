/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import static net.sourceforge.pmd.util.fxdesigner.util.DesignerIteratorUtil.any;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerIteratorUtil.iteratorFrom;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerIteratorUtil.reverse;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerIteratorUtil.toIterable;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil.or;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.endPosition;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.findNodeAt;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.findNodeCovering;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.rangeOf;

import java.util.Iterator;
import java.util.Optional;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextPos2D;

import javafx.scene.control.TreeItem;

/**
 * @author Clément Fournier
 */
public final class AstTraversalUtil {

    private AstTraversalUtil() {

    }


    public static Node getRoot(Node n) {
        return n == null ? null
                         : n.getParent() == null
                           ? n : getRoot(n.getParent());
    }

    /**
     * Tries hard to find the node in [myRoot] that corresponds most closely
     * to the given [node], which may be from another tree.
     *
     * @param myRoot (Nullable) root of the tree in which to search
     * @param node   (Nullable) node to look for
     */
    public static Optional<Node> mapToMyTree(final Node myRoot, final Node node, TextPos2D caretPositionOrNull) {
        if (myRoot == null || node == null) {
            return Optional.empty();
        }

        if (AstTraversalUtil.getRoot(node) == myRoot) {
            return Optional.of(node); // same tree, don't set cache
        }

        // user data of a node is the node it maps to in the other tree
        if (node.getUserData() instanceof Node) {
            return Optional.of((Node) node.getUserData());
        }

        Optional<Node> result =
            or(
                or(
                    // first try with path
                    findOldNodeInNewAst(node, myRoot),
                    // then try with exact range
                    () -> findNodeCovering(myRoot, rangeOf(node), true)
                ),
                // fallback on leaf if nothing works
                () -> findNodeAt(myRoot, caretPositionOrNull == null ? endPosition(node)
                                                                     : caretPositionOrNull)
            );

        // the [node] is mapped to the [result]
        // since several nodes may map to the same node in another tree,
        // it's not safe to set both cache entries
        result.ifPresent(node::setUserData);

        return result;
    }


    /**
     * @param oldSelection Not null
     * @param newRoot      Not null
     */
    public static Optional<Node> findOldNodeInNewAst(final Node oldSelection, final Node newRoot) {
        if (oldSelection.getParent() == null) {
            return Optional.of(newRoot);
        }

        Iterator<Node> pathFromOldRoot = reverse(parentIterator(oldSelection, true));

        pathFromOldRoot.next(); // skip root

        Node currentNewNode = newRoot;

        for (Node step : toIterable(pathFromOldRoot)) {

            int n = step.getIndexInParent();

            if (n >= 0 && n < currentNewNode.getNumChildren()) {
                currentNewNode = currentNewNode.getChild(n);
            } else {
                return Optional.empty();
            }
        }

        return currentNewNode.getXPathNodeName().equals(oldSelection.getXPathNodeName())
               ? Optional.of(currentNewNode) : Optional.empty();
    }

    /**
     * Returns an iterator over the parents of the given node, in innermost to outermost order.
     */
    public static Iterator<Node> parentIterator(Node deepest, boolean includeSelf) {
        return iteratorFrom(deepest, n -> n.getParent() != null, Node::getParent, includeSelf);
    }

    /**
     * Returns an iterator over the parents of the given node, in innermost to outermost order.
     */
    public static <T> Iterator<TreeItem<T>> parentIterator(TreeItem<T> deepest, boolean includeSelf) {
        return iteratorFrom(deepest, n -> n.getParent() != null, TreeItem::getParent, includeSelf);
    }


    public static boolean isParent(Node parent, Node child) {
        return any(parentIterator(child, false), p -> parent == p);
    }
}
