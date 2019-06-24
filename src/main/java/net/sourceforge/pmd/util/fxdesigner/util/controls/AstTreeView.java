/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import static net.sourceforge.pmd.util.fxdesigner.util.AstTraversalUtil.findOldNodeInNewAst;
import static net.sourceforge.pmd.util.fxdesigner.util.AstTraversalUtil.mapToMyTree;
import static net.sourceforge.pmd.util.fxdesigner.util.AstTraversalUtil.parentIterator;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerIteratorUtil.toIterable;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.reactfx.EventSource;
import org.reactfx.EventStreams;
import org.reactfx.SuspendableEventStream;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.NodeSelectionSource;
import net.sourceforge.pmd.util.fxdesigner.util.DataHolder;

import javafx.beans.NamedArg;
import javafx.scene.control.SelectionModel;
import javafx.scene.control.TreeItem;


/**
 * Treeview that displays an AST.
 *
 * @author Clément Fournier
 * @since 6.12.0
 */
public class AstTreeView extends SearchableTreeView<Node> implements NodeSelectionSource {


    private final TreeViewWrapper<Node> myWrapper = new TreeViewWrapper<>(this);

    private final EventSource<NodeSelectionEvent> baseSelectionEvents;
    private final SuspendableEventStream<NodeSelectionEvent> suppressibleSelectionEvents;
    private final DesignerRoot designerRoot;
    private final Var<Function<Node, Collection<String>>> additionalStyleClasses =
        Var.newSimpleVar(n -> Collections.emptySet());

    private String debugName = "AstTreeView";


    /** Only provided for scenebuilder, not used at runtime. */
    public AstTreeView() {
        designerRoot = null;
        baseSelectionEvents = null;
        suppressibleSelectionEvents = null;
        getStyleClass().addAll("ast-tree-view");
    }


    public AstTreeView(@NamedArg("designerRoot") DesignerRoot root) {
        designerRoot = root;
        baseSelectionEvents = new EventSource<>();
        suppressibleSelectionEvents = baseSelectionEvents.suppressible();

        getStyleClass().addAll("ast-tree-view");

        initNodeSelectionHandling(root, suppressibleSelectionEvents, false);

        // this needs to be done even if the selection originates from this node
        EventStreams.changesOf(getSelectionModel().selectedItemProperty())
                    .subscribe(item -> highlightFocusNodeParents((ASTTreeItem) item.getOldValue(), (ASTTreeItem) item.getNewValue()));

        // push a node selection event whenever...
        //  * The selection changes
        EventStreams.valuesOf(getSelectionModel().selectedItemProperty())
                    .filterMap(Objects::nonNull, TreeItem::getValue)
                    .map(NodeSelectionEvent::of)
                    .subscribe(baseSelectionEvents::push);

        //  * the currently selected cell is explicitly clicked
        setCellFactory(tv -> new ASTTreeCell(n -> {
            ASTTreeItem selectedTreeItem = (ASTTreeItem) getSelectionModel().getSelectedItem();

            // only push an event if the node was already selected
            if (selectedTreeItem != null && selectedTreeItem.getValue() != null
                && selectedTreeItem.getValue().equals(n)) {
                baseSelectionEvents.push(NodeSelectionEvent.of(n));
            }
        }));


        EventStreams.valuesOf(additionalStyleClasses)
                    .repeatOn(EventStreams.valuesOf(rootProperty()))
                    .subscribe(fun -> {
                        TreeItem<Node> rootNode = getRoot();
                        if (rootNode != null && fun != null) {
                            ((ASTTreeItem) rootNode).foreach(it -> ((ASTTreeItem) it).setStyleClasses(fun.apply(it.getValue())));
                        }
                    });

    }

    public void setAstRoot(Node root) {
        // fetch the selected item before setting the root
        ASTTreeItem selectedTreeItem = (ASTTreeItem) getSelectionModel().getSelectedItem();

        setRealRoot(root == null ? null : ASTTreeItem.buildRoot(root));

        if (getDebugName().contains("old")) {
            // prevent the old treeview from shooting back selection recovery events
            return;
        }

        if (root != null && selectedTreeItem != null && selectedTreeItem.getValue() != null) {
            Node newSelection = findOldNodeInNewAst(selectedTreeItem.getValue(), root).orElse(null);
            if (newSelection != null) {
                baseSelectionEvents.push(NodeSelectionEvent.of(newSelection, new DataHolder().withData(SELECTION_RECOVERY, true)));
                setFocusNode(newSelection, new DataHolder()); // rehandle
            } else {
                baseSelectionEvents.push(NodeSelectionEvent.of(null));
            }
        }
    }

    /**
     * Focus the given node, handling scrolling if needed.
     */
    @Override
    public void setFocusNode(final Node node, DataHolder options) {
        SelectionModel<TreeItem<Node>> selectionModel = getSelectionModel();

        if (getRoot() == null || getRoot().getValue() == null) {
            return;
        }

        mapToMyTree(getRoot().getValue(), node, options.getData(CARET_POSITION))
            .map(((ASTTreeItem) getRoot())::findItem)
            .ifPresent(found -> {
                // don't fire any selection event while itself setting the selected item
                suppressibleSelectionEvents.suspendWhile(() -> selectionModel.select(found));

            });

        getFocusModel().focus(selectionModel.getSelectedIndex());
        if (!isIndexVisible(selectionModel.getSelectedIndex())) {
            scrollTo(selectionModel.getSelectedIndex());
        }
    }


    public Function<Node, Collection<String>> getAdditionalStyleClasses() {
        return additionalStyleClasses.getValue();
    }

    public Var<Function<Node, Collection<String>>> additionalStyleClassesProperty() {
        return additionalStyleClasses;
    }

    public void setAdditionalStyleClasses(Function<Node, Collection<String>> mapper) {
        if (mapper == null) {
            mapper = n -> Collections.emptyList();
        }
        this.additionalStyleClasses.setValue(mapper);
    }


    private void highlightFocusNodeParents(ASTTreeItem oldSelection, ASTTreeItem newSelection) {
        if (oldSelection != null) {
            // remove highlighting on the cells of the item
            sideEffectParents(oldSelection, (item, depth) -> item.setStyleClasses(/* empty */));
        }

        if (newSelection != null) {
            // 0 is the deepest node, "depth" goes up as we get up the parents
            sideEffectParents(newSelection, (item, depth) -> item.setStyleClasses("ast-parent",
                                                                                  "depth-"
                                                                                      + depth));
        }
    }


    private void sideEffectParents(ASTTreeItem deepest, BiConsumer<ASTTreeItem, Integer> itemAndDepthConsumer) {

        int depth = 0;
        for (TreeItem<Node> item : toIterable(parentIterator(deepest, true))) {
            // the depth is "reversed" here, i.e. the deepest node has depth 0
            itemAndDepthConsumer.accept((ASTTreeItem) item, depth++);
        }

    }


    /**
     * Returns true if the item at the given index is visible in the TreeView.
     */
    private boolean isIndexVisible(int index) {
        return myWrapper.isIndexVisible(index);
    }


    @Override
    public DesignerRoot getDesignerRoot() {
        return designerRoot;
    }


    public void setDebugName(String debugName) {
        this.debugName = debugName;
    }

    @Override
    public String getDebugName() {
        return debugName;
    }
}
