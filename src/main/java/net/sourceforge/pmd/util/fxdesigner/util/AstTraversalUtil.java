/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import static net.sourceforge.pmd.util.fxdesigner.util.DesignerIteratorUtil.parentIterator;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerIteratorUtil.reverse;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerIteratorUtil.toIterable;

import java.util.Iterator;
import java.util.Optional;

import net.sourceforge.pmd.lang.ast.Node;

/**
 * @author Clément Fournier
 */
public final class AstTraversalUtil {

    private AstTraversalUtil() {

    }

    /**
     * TODO move to some util.
     *
     * @param oldSelection Not null
     * @param newRoot      Not null
     */
    public static Optional<Node> findOldNodeInNewAst(final Node oldSelection, final Node newRoot) {
        if (oldSelection.jjtGetParent() == null) {
            return Optional.of(newRoot);
        }

        Iterator<Node> pathFromOldRoot = reverse(parentIterator(oldSelection, true));

        pathFromOldRoot.next(); // skip root

        Node currentNewNode = newRoot;

        for (Node step : toIterable(pathFromOldRoot)) {

            int n = step.jjtGetChildIndex();

            if (n >= 0 && n < currentNewNode.jjtGetNumChildren()) {
                currentNewNode = currentNewNode.jjtGetChild(n);
            } else {
                return Optional.empty();
            }
        }

        return currentNewNode.getClass() == oldSelection.getClass()
               ? Optional.of(currentNewNode) : Optional.empty();
    }
}
