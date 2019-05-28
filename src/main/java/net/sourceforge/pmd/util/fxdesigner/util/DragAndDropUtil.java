/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import java.util.function.Consumer;

import org.reactfx.Subscription;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextRange;

import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

public final class DragAndDropUtil {

    /** Style class for {@linkplain #registerAsNodeDragTarget(javafx.scene.Node, Consumer) node drag over.} */
    public static final String NODE_DRAG_OVER = "node-drag-over";
    /** Data format for drag and drop. */
    private static final DataFormat NODE_RANGE_DATA_FORMAT = new DataFormat("pmd/node");

    private DragAndDropUtil() {

    }

    /**
     * Registers the given [source] javafx Node as a source for a drag
     * and drop even with {@link #NODE_RANGE_DATA_FORMAT} content.
     */
    public static Subscription registerAsNodeDragSource(javafx.scene.Node source, Node data) {
        source.setOnDragDetected(evt -> {
            // drag and drop
            Dragboard db = source.startDragAndDrop(TransferMode.LINK);
            ClipboardContent content = new ClipboardContent();
            content.put(NODE_RANGE_DATA_FORMAT, TextRange.fullLine(data.getBeginLine(), 10000));
            db.setContent(content);
            evt.consume();
        });
        return () -> source.setOnDragDetected(null);
    }

    /**
     * Registers a {@link javafx.scene.Node} as the target of a drag and
     * drop initiated by {@link #registerAsNodeDragSource(javafx.scene.Node, Node)}.
     *
     * <p>While the mouse is over the target, the target will have the CSS
     * class {@link #NODE_DRAG_OVER}.
     *
     * @param target            Target UI component
     * @param nodeRangeConsumer Action to run with the {@link Dragboard} contents on success
     */
    public static void registerAsNodeDragTarget(javafx.scene.Node target, Consumer<TextRange> nodeRangeConsumer) {
        target.setOnDragOver(evt -> {
            if (evt.getGestureSource() != target &&
                evt.getDragboard().hasContent(NODE_RANGE_DATA_FORMAT)) {
                /* allow for both copying and moving, whatever user chooses */
                evt.acceptTransferModes(TransferMode.LINK);
            }
            evt.consume();
        });

        target.setOnDragEntered(evt -> {
            if (evt.getGestureSource() != target &&
                evt.getDragboard().hasContent(NODE_RANGE_DATA_FORMAT)) {
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
                TextRange content = (TextRange) db.getContent(NODE_RANGE_DATA_FORMAT);
                nodeRangeConsumer.accept(content);
                success = true;
            }

            evt.setDropCompleted(success);

            evt.consume();
        });
    }
}