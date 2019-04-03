/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app;

import static java.util.Collections.emptySet;

import java.util.Set;

import org.reactfx.EventStream;
import org.reactfx.value.Val;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.XPathPanelController;
import net.sourceforge.pmd.util.fxdesigner.util.controls.AstTreeView;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;


/**
 * A control or controller that somehow displays nodes in a form that the user can select.
 * When a node is selected by the user (e.g. {@link AstTreeView}, {@link XPathPanelController}, etc),
 * the whole UI is synchronized to reflect information about the node. This includes scrolling
 * the TreeView, the editor, etc. To achieve that uniformly, node selection events are forwarded
 * as messages on a {@link MessageChannel}.
 *
 * @author Clément Fournier
 */
public interface NodeSelectionSource extends ApplicationComponent {


    /**
     * Updates the UI to react to a change in focus node. This is called whenever some selection source
     * in the tree records a change.
     */
    void setFocusNode(Node node, Set<SelectionOption> options);


    /**
     * Initialises this component. Must be called by the component somewhere.
     *
     * @param root                  Instance of the app. Should be the same as {@link #getDesignerRoot()},
     *                              but the parameter here is to make it clear that {@link #getDesignerRoot()}
     *                              must be initialized before this method is called.
     * @param mySelectionEvents     Stream of nodes that should push an event each time the user selects a node
     *                              from this control. The whole app will sync to this new selection.
     * @param alwaysHandleSelection Whether the component should handle selection events that originated from itself.
     *                              For now some must, because they aggregate several selection sources (the {@link net.sourceforge.pmd.util.fxdesigner.NodeInfoPanelController}).
     *                              Splitting it into separate controls will remove the need for that.
     */
    default Val<Node> initNodeSelectionHandling(DesignerRoot root,
                                                EventStream<? extends NodeSelectionEvent> mySelectionEvents,
                                                boolean alwaysHandleSelection) {
        MessageChannel<NodeSelectionEvent> channel = root.getService(DesignerRoot.NODE_SELECTION_CHANNEL);
        mySelectionEvents.subscribe(n -> channel.pushEvent(this, n));
        EventStream<NodeSelectionEvent> selection = channel.messageStream(alwaysHandleSelection, this);
        selection.subscribe(evt -> setFocusNode(evt.selected, evt.options));
        return ReactfxUtil.latestValue(selection.map(it -> it.selected));
    }


    enum SelectionOption {
        /**
         * This selection is the reselection of a node across a parsing.
         * Stuff like scrolling or external style changes should be avoided,
         * only the internal model should be affected.
         */
        SELECTION_RECOVERY
    }

    class NodeSelectionEvent {

        // RRR data class

        public final Node selected;
        public final Set<SelectionOption> options;

        private NodeSelectionEvent(Node selected, Set<SelectionOption> options) {
            this.selected = selected;
            this.options = options;
        }

        @Override
        public String toString() {
            return getClass().getName() + "(node=" + selected + ", options=" + options + ")";
        }

        public static NodeSelectionEvent of(Node selected) {
            return new NodeSelectionEvent(selected, emptySet());
        }

        public static NodeSelectionEvent of(Node selected, Set<SelectionOption> options) {
            return new NodeSelectionEvent(selected, options);
        }
    }
}
