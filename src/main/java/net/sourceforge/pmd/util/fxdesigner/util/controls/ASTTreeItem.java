/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import static net.sourceforge.pmd.util.fxdesigner.util.AstTraversalUtil.parentIterator;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerIteratorUtil.reverse;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.xpath.Attribute;
import net.sourceforge.pmd.util.designerbindings.DesignerBindings;
import net.sourceforge.pmd.util.designerbindings.DesignerBindings.DefaultDesignerBindings;
import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.util.controls.SearchableTreeView.SearchableTreeItem;

import javafx.scene.control.TreeItem;

/**
 * Represents a tree item (data, not UI) in the ast TreeView.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public final class ASTTreeItem extends SearchableTreeItem<Node> implements ApplicationComponent {


    /**
     * Latent style classes are style classes that logically belong to this tree item (i.e. the node it wraps).
     * The TreeItem must sync them to the TreeCell that currently displays it. The value is never null.
     */
    private final Var<Collection<String>> latentStyleClasses = Var.newSimpleVar(Collections.emptyList());
    private final DesignerRoot designerRoot;


    private ASTTreeItem(Node n, int treeIndex, DesignerRoot designerRoot) {
        super(n, treeIndex);
        this.designerRoot = designerRoot;
        setExpanded(true);

        treeCellProperty().changes().subscribe(change -> {
            if (change.getOldValue() != null) {
                change.getOldValue().getStyleClass().removeAll(latentStyleClasses.getValue());
            }

            if (change.getNewValue() != null) {
                change.getNewValue().getStyleClass().addAll(latentStyleClasses.getValue());
            }

        });

        latentStyleClasses.changes()
                          // .conditionOn(treeCellProperty().map(Objects::nonNull))
                          .subscribe(change -> {
                              if (treeCellProperty().isPresent()) {
                                  treeCellProperty().getValue().getStyleClass().removeAll(change.getOldValue());
                                  treeCellProperty().getValue().getStyleClass().addAll(change.getNewValue());
                              }
                          });
    }


    /**
     * Finds the tree item corresponding to the given node
     * among the descendants of this item. This method assumes
     * this item is the root node.
     *
     * @param node The node to find
     *
     * @return The found item, or null if this item doesn't wrap the
     *     root of the tree to which the parameter belongs
     */
    public ASTTreeItem findItem(Node node) {
        // This is an improvement over the previous algorithm which performed a greedy
        // depth-first traversal over all the tree (was at worst O(size of the tree),
        // now it's at worst O(number of parents of the searched node))

        if (node == null) {
            return null;
        }

        Iterator<Node> pathToNode = reverse(parentIterator(node, true));

        if (pathToNode.next() != getValue()) {
            // this node is not the root of the tree
            // to which the node we're looking for belongs
            return null;
        }

        ASTTreeItem current = this;

        while (pathToNode.hasNext()) {
            Node currentNode = pathToNode.next();

            current = current.getChildren().stream()
                             .filter(item -> item.getValue() == currentNode)
                             .findAny()
                             .map(ASTTreeItem.class::cast)
                             .get(); // theoretically, this cannot fail, since we use reference identity

        }

        return current;
    }

    public void setStyleClasses(Collection<String> classes) {
        latentStyleClasses.setValue(classes == null ? Collections.emptyList() : classes);
    }


    public void setStyleClasses(String... classes) {
        setStyleClasses(Arrays.asList(classes));
    }


    @Override
    public String getSearchableText() {
        return getValue() != null ? nodePresentableText(getValue()) : null;
    }


    @Override
    public DesignerRoot getDesignerRoot() {
        return designerRoot;
    }


    /**
     * Builds an ASTTreeItem recursively from a node.
     */
    static ASTTreeItem buildRoot(Node n, DesignerRoot designerRoot) {
        return buildRootImpl(n, new MutableInt(0), designerRoot);
    }


    /**
     * Builds an ASTTreeItem recursively from a node.
     */
    private static ASTTreeItem buildRootImpl(Node n, MutableInt idx, DesignerRoot designerRoot) {
        ASTTreeItem item = new ASTTreeItem(n, idx.getAndIncrement(), designerRoot);
        if (n.getNumChildren() > 0) {
            for (int i = 0; i < n.getNumChildren(); i++) {
                item.getChildren().add(buildRootImpl(n.getChild(i), idx, designerRoot));
            }
        }
        return item;
    }

    public static <T, N extends TreeItem<T>> void foreach(N root, Consumer<? super N> fun) {

        if (root == null) {
            return;
        }

        fun.accept(root);

        for (TreeItem<T> child : root.getChildren()) {
            @SuppressWarnings("unchecked")
            N c = (N) child;
            foreach(c, fun);
        }
    }


    private String nodePresentableText(Node node) {
        DesignerBindings bindings = languageBindingsProperty().getOrElse(DefaultDesignerBindings.getInstance());

        Attribute attr = bindings.getMainAttribute(node);
        if (attr == null || attr.getStringValue() == null) {
            return node.getXPathNodeName();
        } else {
            String stringValue = attr.getStringValue();
            Object v = attr.getValue();
            if (v instanceof String || v instanceof Enum) {
                stringValue = "\"" + StringEscapeUtils.escapeJava(stringValue) + "\"";
            }
            return node.getXPathNodeName()
                + " [@" + attr.getName() + " = " + stringValue + "]";
        }
    }

}
