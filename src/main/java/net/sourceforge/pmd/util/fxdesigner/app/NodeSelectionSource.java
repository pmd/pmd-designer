/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app;

import static net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil.printShortStackTrace;

import java.util.Objects;

import org.reactfx.EventStream;
import org.reactfx.value.Val;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.XPathRuleEditorController;
import net.sourceforge.pmd.util.fxdesigner.util.DataHolder;
import net.sourceforge.pmd.util.fxdesigner.util.DataHolder.DataKey;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextPos2D;
import net.sourceforge.pmd.util.fxdesigner.util.controls.AstTreeView;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import javafx.scene.input.DataFormat;


/**
 * A control or controller that somehow displays nodes in a form that the user can select.
 * When a node is selected by the user (e.g. {@link AstTreeView}, {@link XPathRuleEditorController}, etc),
 * the whole UI is synchronized to reflect information about the node. This includes scrolling
 * the TreeView, the editor, etc. To achieve that uniformly, node selection events are forwarded
 * as messages on a {@link MessageChannel}.
 *
 * @author Clément Fournier
 */
public interface NodeSelectionSource extends ApplicationComponent {

    /**
     * This selection is the reselection of a node across a parsing.
     * Stuff like scrolling or external style changes should be avoided,
     * only the internal model should be affected.
     */
    DataKey<Boolean> SELECTION_RECOVERY = new DataKey<>("isSelectionRecover");


    /**
     * The position of the caret, when the selection is carried out from
     * the code area.
     */
    DataKey<TextPos2D> CARET_POSITION = new DataKey<>("caretPosition");
    DataFormat NODE_RANGE_DATA_FORMAT = new DataFormat("pmd/node");


    /**
     * Updates the UI to react to a change in focus node. This is called whenever some selection source
     * in the tree records a change.
     */
    void setFocusNode(Node node, DataHolder options);


    /**
     * Initialises this component. Must be called by the component somewhere.
     *
     * @param root                  Instance of the app. Should be the same as {@link #getDesignerRoot()},
     *                              but the parameter here is to make it clear that {@link #getDesignerRoot()}
     *                              must be initialized before this method is called.
     * @param mySelectionEvents     Stream of nodes that should push an event each time the user selects a node
     *                              from this control. The whole app will sync to this new selection.
     * @param alwaysHandleSelection Whether the component should handle selection events that originated from itself.
     *
     * @return A Val reflecting the current global selection for the app.
     * Note that that Val is lazy and so if you don't subscribe to it or
     * {@linkplain Val#pin() pin it} you won't see updates!
     */
    default Val<Node> initNodeSelectionHandling(DesignerRoot root,
                                                EventStream<? extends NodeSelectionEvent> mySelectionEvents,
                                                boolean alwaysHandleSelection) {
        MessageChannel<NodeSelectionEvent> channel = root.getService(DesignerRoot.NODE_SELECTION_CHANNEL);
        mySelectionEvents.subscribe(n -> channel.pushEvent(this, n));
        EventStream<NodeSelectionEvent> selection = channel.messageStream(alwaysHandleSelection, this);
        selection.subscribe(evt -> {
            try {
                setFocusNode(evt.selected, evt.options);
            } catch (Exception e) {
                logInternalException(e);
                printShortStackTrace(e);
                // don't rethrow so that an error by one source doesn't affect others
            }
        });
        return ReactfxUtil.latestValue(selection.map(it -> it.selected));
    }



    class NodeSelectionEvent {

        // RRR data class

        public final Node selected;
        public final DataHolder options;

        private NodeSelectionEvent(Node selected, DataHolder options) {
            this.selected = selected;
            this.options = options;
        }

        @Override
        public String toString() {
            return "{node=" + selected + ", options=" + options + "}";
        }

        public static NodeSelectionEvent of(Node selected) {
            return new NodeSelectionEvent(selected, new DataHolder());
        }

        public static NodeSelectionEvent of(Node selected, DataHolder options) {
            return new NodeSelectionEvent(selected, options);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            NodeSelectionEvent that = (NodeSelectionEvent) o;
            return Objects.equals(selected, that.selected)
                && Objects.equals(options, that.options);
        }

        @Override
        public int hashCode() {
            return Objects.hash(selected, options);
        }
    }
}
