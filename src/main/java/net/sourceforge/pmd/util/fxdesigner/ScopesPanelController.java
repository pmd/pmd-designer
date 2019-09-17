/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import static net.sourceforge.pmd.util.fxdesigner.util.AstTraversalUtil.parentIterator;

import java.util.Objects;

import org.reactfx.EventStream;
import org.reactfx.EventStreams;
import org.reactfx.SuspendableEventStream;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.symboltable.NameDeclaration;
import net.sourceforge.pmd.util.fxdesigner.app.AbstractController;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.NodeSelectionSource;
import net.sourceforge.pmd.util.fxdesigner.util.DataHolder;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerIteratorUtil;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ScopeHierarchyTreeCell;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ScopeHierarchyTreeItem;

import javafx.fxml.FXML;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;


/**
 * Controller of the scopes panel
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
@SuppressWarnings("PMD.UnusedPrivateField")
public class ScopesPanelController extends AbstractController implements NodeSelectionSource {


    @FXML
    private TreeView<Object> scopeHierarchyTreeView;


    private SuspendableEventStream<TreeItem<Object>> myScopeItemSelectionEvents;


    public ScopesPanelController(DesignerRoot designerRoot) {
        super(designerRoot);
    }


    @Override
    protected void beforeParentInit() {


        scopeHierarchyTreeView.setCellFactory(view -> new ScopeHierarchyTreeCell());


        // suppress as early as possible in the pipeline
        myScopeItemSelectionEvents = EventStreams.valuesOf(scopeHierarchyTreeView.getSelectionModel().selectedItemProperty()).suppressible();

        EventStream<NodeSelectionEvent> selectionEvents = myScopeItemSelectionEvents.filter(Objects::nonNull)
                                                                                    .map(TreeItem::getValue)
                                                                                    .filterMap(o -> o instanceof NameDeclaration, o -> (NameDeclaration) o)
                                                                                    .map(NameDeclaration::getNode)
                                                                                    .map(NodeSelectionEvent::of);

        initNodeSelectionHandling(getDesignerRoot(), selectionEvents, true);
    }


    @Override
    public void setFocusNode(final Node node, DataHolder options) {
        if (node == null) {
            scopeHierarchyTreeView.setRoot(null);
            return;
        }

        // current selection
        TreeItem<Object> previousSelection = scopeHierarchyTreeView.getSelectionModel().getSelectedItem();

        ScopeHierarchyTreeItem rootScope = ScopeHierarchyTreeItem.buildAscendantHierarchy(node);
        scopeHierarchyTreeView.setRoot(rootScope);


        if (previousSelection != null) {
            // Try to find the node that was previously selected and focus it in the new ascendant hierarchy.
            // Otherwise, when you select a node in the scope tree, since focus of the app is shifted to that
            // node, the scope hierarchy is reset and you lose the selection - even though obviously the node
            // you selected is in its own scope hierarchy so it looks buggy.
            int maxDepth = DesignerIteratorUtil.count(parentIterator(previousSelection, true));
            rootScope.tryFindNode(previousSelection.getValue(), maxDepth)
                     // suspend notifications while selecting
                     .ifPresent(item -> myScopeItemSelectionEvents.suspendWhile(() -> scopeHierarchyTreeView.getSelectionModel().select(item)));
        }
    }


    @Override
    public String getDebugName() {
        return "scopes-panel";
    }
}
