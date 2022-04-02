/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.function.Consumer;

import org.reactfx.Subscription;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.TextAvailableNode;
import net.sourceforge.pmd.lang.document.TextDocument;
import net.sourceforge.pmd.lang.document.TextRegion;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;

import javafx.css.PseudoClass;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

public final class DragAndDropUtil {

    // since dragboard contents must be Serializable we
    // put a TextRange2d in the dragboard and not a Node

    /** Style class for {@linkplain #registerAsNodeDragTarget(javafx.scene.Node, Consumer, DesignerRoot) node drag over.} */
    public static final String NODE_DRAG_OVER = "node-drag-over";
    /** Data format for drag and drop. */
    private static final DataFormat NODE_RANGE_DATA_FORMAT = new DataFormat("pmd/node");

    private DragAndDropUtil() {

    }

    /**
     * Registers the given [source] javafx Node as a source for a drag
     * and drop even with {@link #NODE_RANGE_DATA_FORMAT} content.
     */
    public static Subscription registerAsNodeDragSource(javafx.scene.Node source, Node data, DesignerRoot root) {
        source.setOnDragDetected(evt -> {
            // drag and drop
            Dragboard db = source.startDragAndDrop(TransferMode.LINK);
            ClipboardContent content = new ClipboardContent();
            content.put(NODE_RANGE_DATA_FORMAT, data.getTextRegion());
            db.setContent(content);
            root.getService(DesignerRoot.IS_NODE_BEING_DRAGGED).setValue(true);
            evt.consume();
        });

        source.setOnDragDone(evt -> {
            if (evt.getDragboard().hasContent(NODE_RANGE_DATA_FORMAT)) {
                root.getService(DesignerRoot.IS_NODE_BEING_DRAGGED).setValue(false);
            }
        });

        return () -> source.setOnDragDetected(null);
    }


    private static TextRegion getTextRegion(Node data) {
        if (data instanceof TextAvailableNode) {
            return ((TextAvailableNode) data).getTextRegion();
        } else {
            TextDocument doc = data.getAstInfo().getTextDocument();
            // i need a way to turn
            int startOffset = doc.offsetAtLineColumn(data.getBeginLine(), data.getBeginColumn());
            int endOffset = doc.offsetAtLineColumn(data.getEndLine(), data.getEndColumn());
            return TextRegion.fromBothOffsets(startOffset, endOffset);
        }
    }


    /**
     * Registers a {@link javafx.scene.Node} as the target of a drag and
     * drop initiated by {@link #registerAsNodeDragSource(javafx.scene.Node, Node, DesignerRoot)}.
     *
     * <p>While the mouse is over the target, the target will have the CSS
     * class {@link #NODE_DRAG_OVER}.
     *  @param target            Target UI component
     * @param nodeRangeConsumer Action to run with the {@link Dragboard} contents on success
     * @param root
     */
    public static void registerAsNodeDragTarget(javafx.scene.Node target, Consumer<TextRegion> nodeRangeConsumer, DesignerRoot root) {

        root.getService(DesignerRoot.IS_NODE_BEING_DRAGGED)
            .values()
            .subscribe(it -> target.pseudoClassStateChanged(PseudoClass.getPseudoClass("node-drag-possible-target"), it));


        target.setOnDragOver(evt -> {
            if (evt.getGestureSource() != target
                && evt.getDragboard().hasContent(NODE_RANGE_DATA_FORMAT)) {
                /* allow for both copying and moving, whatever user chooses */
                evt.acceptTransferModes(TransferMode.LINK);
            }
            evt.consume();
        });

        target.setOnDragEntered(evt -> {
            if (evt.getGestureSource() != target
                && evt.getDragboard().hasContent(NODE_RANGE_DATA_FORMAT)) {
                target.getStyleClass().addAll(NODE_DRAG_OVER);
            }
            evt.consume();
        });

        target.setOnDragExited(evt -> {
            target.getStyleClass().remove(NODE_DRAG_OVER);
            evt.consume();
        });

        target.setOnDragDropped(evt -> {

            boolean success = false;

            Dragboard db = evt.getDragboard();
            if (db.hasContent(NODE_RANGE_DATA_FORMAT)) {
                TextRegion content = (TextRegion) db.getContent(NODE_RANGE_DATA_FORMAT);
                nodeRangeConsumer.accept(content);
                success = true;
            }

            evt.setDropCompleted(success);

            evt.consume();
        });
    }
}
