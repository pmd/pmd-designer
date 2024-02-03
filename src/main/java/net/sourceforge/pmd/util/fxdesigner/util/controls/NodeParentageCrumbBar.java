/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;


import static net.sourceforge.pmd.util.fxdesigner.util.AstTraversalUtil.parentIterator;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerIteratorUtil.asReversed;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerIteratorUtil.count;

import java.util.Objects;
import java.util.function.Function;

import org.controlsfx.control.BreadCrumbBar;
import org.reactfx.EventSource;
import org.reactfx.value.Val;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.NodeSelectionSource;
import net.sourceforge.pmd.util.fxdesigner.util.DataHolder;

import javafx.beans.NamedArg;
import javafx.css.PseudoClass;
import javafx.scene.control.Button;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.Region;
import javafx.util.Callback;


/**
 * Bread crumb bar to display the parents of a node. Avoids overflow by estimating the number of
 * crumbs we can layout based on the actual size of the control.
 *
 * @author Clément Fournier
 * @since 7.0.0
 */
public class NodeParentageCrumbBar extends BreadCrumbBar<Node> implements NodeSelectionSource {

    private static final int DEFAULT_PX_BY_CHAR = 5;
    private static final int DEFAULT_CONSTANT_PADDING = 19;

    /** Special item used to truncate paths when they're too long. */
    private final TreeItem<Node> ellipsisCrumb = new TreeItem<>(null);
    private final EventSource<Node> selectionEvents = new EventSource<>();
    private final DesignerRoot designerRoot;
    /** number of nodes currently behind the ellipsis */
    private int numElidedNodes = 0;


    public NodeParentageCrumbBar() {
        this.designerRoot = null;
    }

    public NodeParentageCrumbBar(@NamedArg("designerRoot") DesignerRoot designerRoot) {
        this.designerRoot = designerRoot;

        // This allows to click on a parent crumb and keep the children crumb
        setAutoNavigationEnabled(false);

        // captured in the closure
        final Callback<TreeItem<Node>, Button> originalCrumbFactory = getCrumbFactory();

        setOnCrumbAction(ev -> {
            if (ev.getSelectedCrumb() != ellipsisCrumb) { // NOPMD - CompareObjectsWithEquals
                selectionEvents.push(ev.getSelectedCrumb().getValue());
            }
        });

        setCrumbFactory(item -> {
            Button button = originalCrumbFactory.call(item);
            if (item == ellipsisCrumb) { // NOPMD - CompareObjectsWithEquals
                button.setText("... (" + numElidedNodes + ")");
                button.setTooltip(new Tooltip(numElidedNodes + " ancestors are not shown"));
            } else if (item != null) {
                button.setText(item.getValue().getXPathNodeName());
                DragAndDropUtil.registerAsNodeDragSource(button, item.getValue(), getDesignerRoot());
            }

            // we use that to communicate the node later on
            button.setUserData(item);
            Val.wrap(button.focusedProperty())
               .values()
               .distinct()
               .filter(Boolean::booleanValue)
                // will change the node in the treeview on <- -> key presses
                .subscribe(b -> getOnCrumbAction().handle(new BreadCrumbActionEvent<>(item)));
            return button;
        });


        initNodeSelectionHandling(
            designerRoot,
            selectionEvents.map(NodeSelectionEvent::of),
            true
        );

    }


    // getSelectedCrumb gets the deepest displayed node

    private Node currentSelection;
    /** Index wrt the rightmost crumb. */
    private int selectedIdx = -1;


    /**
     * If the node is already displayed on the crumbbar, only sets the focus on it. Otherwise, sets
     * the node to be the deepest one of the crumb bar. Noop if node is null.
     */
    @Override
    public void setFocusNode(final Node newSelection, DataHolder options) {

        if (newSelection == null) {
            setSelectedCrumb(null);
            return;
        }

        if (Objects.equals(newSelection, currentSelection)) {
            return;
        }

        currentSelection = newSelection;

        boolean found = false;

        // We're trying to estimate the ratio of px/crumb,
        // to make an educated guess about how many crumbs we can fit
        // in case we need to call setDeepestNode
        int totalNumChar = 0;
        int totalNumCrumbs = 0;
        // the sum of children width is the actual width with overflow
        // the width of this control is the max acceptable width *without* overflow
        double totalChildrenWidth = 0;
        // constant padding around the graphic of a BreadCrumbButton
        // (difference between width of a BreadCrumbButton and that of its graphic)
        double constantPadding = Double.NaN;


        int i = 0;
        // right to left
        for (javafx.scene.Node button : asReversed(getChildren())) {
            Node n = (Node) ((TreeItem<?>) button.getUserData()).getValue();
            // when recovering from a selection it's impossible that the node be found,
            // updating the style would cause visible twitching
            if (!options.hasData(SELECTION_RECOVERY)) {
                // set the focus on the one being selected, remove on the others
                // calling requestFocus would switch the focus from eg the treeview to the crumb bar (unusable)
                button.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), newSelection.equals(n));
            }
            // update counters
            totalNumChar += ((Labeled) button).getText().length();
            double childWidth = ((Region) button).getWidth();
            totalChildrenWidth += childWidth;
            totalNumCrumbs++;
            if (Double.isNaN(constantPadding)) {
                Region graphic = (Region) ((Labeled) button).getGraphic();
                if (graphic != null) {
                    constantPadding = childWidth - graphic.getWidth();
                }
            }

            if (newSelection.equals(n)) {
                found = true;
                selectedIdx = getChildren().size() - i;
            }

            i++;
        }

        if (!found && !options.hasData(SELECTION_RECOVERY) || options.hasData(SELECTION_RECOVERY) && selectedIdx != 0) {
            // Then we reset the deepest node.

            setDeepestNode(newSelection, getWidthEstimator(totalNumChar, totalChildrenWidth, totalNumCrumbs, constantPadding));
            // set the deepest as focused
            getChildren().get(getChildren().size() - 1)
                         .pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);
            selectedIdx = 0;
        } else if (options.hasData(SELECTION_RECOVERY)) {

            Node cur = newSelection;
            // right to left, update underlying nodes without changing display
            // this relies on the fact that selection recovery only selects nodes with exactly the same path
            for (javafx.scene.Node child : asReversed(getChildren())) {
                if (cur == null) {
                    break;
                }
                @SuppressWarnings("unchecked")
                TreeItem<Node> userData = (TreeItem<Node>) child.getUserData();
                userData.setValue(cur);
                cur = cur.getParent();
            }
        }
    }


    /**
     * Sets the given node to the selected (deepest) crumb. Parent crumbs are added until they are
     * estimated to overflow the visual space, after which they are hidden into the ellipsis crumb.
     *
     * @param node           Node to set
     * @param widthEstimator Estimates the visual width of the crumb for one node
     */
    private void setDeepestNode(Node node, Function<Node, Double> widthEstimator) {
        TreeItem<Node> deepest = new TreeItem<>(node);
        TreeItem<Node> current = deepest;
        Node parent = node.getParent();
        double pathLength = widthEstimator.apply(node);

        final double maxPathLength = getWidth() * 0.9;

        while (parent != null && pathLength < maxPathLength) {
            TreeItem<Node> newItem = new TreeItem<>(parent);
            newItem.getChildren().add(current);
            current = newItem;
            pathLength += widthEstimator.apply(parent);
            parent = current.getValue().getParent();
        }

        if (pathLength >= maxPathLength
            // if parent == null then it's the root, no need for ellipsis
            && parent != null) {

            numElidedNodes = count(parentIterator(parent, true));

            // the rest are children of the ellipsis
            ellipsisCrumb.getChildren().clear();
            ellipsisCrumb.getChildren().add(current);
        }

        setSelectedCrumb(deepest);
    }


    private Function<Node, Double> getWidthEstimator(int totalNumDisplayedChars, double totalChildrenWidth, int totalNumCrumbs, double constantPadding) {

        double safeConstantPadding = Double.isNaN(constantPadding)
                                     ? DEFAULT_CONSTANT_PADDING // that's the value on my machine
                                     : constantPadding;

        double thisPxByChar = totalNumDisplayedChars == 0
                              ? DEFAULT_PX_BY_CHAR // we have no data, too bad
                              : (totalChildrenWidth - safeConstantPadding * totalNumCrumbs)
                                  / totalNumDisplayedChars;

        return node -> node.getXPathNodeName().length() * (thisPxByChar + 1 /*scale it up a bit*/)
            + safeConstantPadding;
    }


    @Override
    public DesignerRoot getDesignerRoot() {
        return designerRoot;
    }


    @Override
    public String getDebugName() {
        return "crumb-bar";
    }
}
