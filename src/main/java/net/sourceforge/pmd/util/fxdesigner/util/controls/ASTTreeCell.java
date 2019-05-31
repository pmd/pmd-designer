/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import static net.sourceforge.pmd.util.fxdesigner.util.DumpUtil.dumpToSubtreeTest;

import java.util.function.Consumer;

import org.apache.commons.lang3.StringEscapeUtils;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.util.controls.SearchableTreeView.SearchableTreeCell;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;


/**
 * Formats the cell for AST nodes in the main AST TreeView.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public class ASTTreeCell extends SearchableTreeCell<Node> {

    private final Consumer<Node> onNodeItemSelected;


    public ASTTreeCell(Consumer<Node> clickHandler) {
        this.onNodeItemSelected = clickHandler;
    }


    private ContextMenu buildContextMenu(Node item) {
        ContextMenu contextMenu = new ContextMenuWithNoArrows();
        CustomMenuItem menuItem = new CustomMenuItem(new Label("Copy subtree test to clipboard..."));

        Tooltip tooltip = new Tooltip("Creates a node spec using the Kotlin AST matcher DSL, and dumps it to the clipboard");
        Tooltip.install(menuItem.getContent(), tooltip);

        menuItem.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(dumpToSubtreeTest(item)); // item is captured in the closure
            clipboard.setContent(content);
        });

        contextMenu.getItems().add(menuItem);

        return contextMenu;
    }

    @Override
    public final void updateItem(Node item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            if (searchText.isPresent()) {
                setGraphic(searchText.getValue());
                setText(null);
                return;
            }

            setGraphic(null);
            setText(nodePresentableText(item));
            setContextMenu(buildContextMenu(item));

            DragAndDropUtil.registerAsNodeDragSource(this, item);

            // Reclicking the selected node in the ast will scroll back to the node in the editor
            this.addEventHandler(MouseEvent.MOUSE_CLICKED, t -> {
                TreeItem<Node> selectedItem = getTreeView().getSelectionModel().getSelectedItem();
                if (t.getButton() == MouseButton.PRIMARY
                    && selectedItem != null && selectedItem.getValue() == item) {
                    onNodeItemSelected.accept(item);
                    t.consume();
                }
            });
        }
    }

    private static String nodePresentableText(Node node) {
        String image = node.getImage() == null ? "" : " \"" + StringEscapeUtils.escapeJava(node.getImage()) + "\"";
        return node.getXPathNodeName() + image;
    }

    @Override
    public String getSearchableText() {
        return getItem() != null ? nodePresentableText(getItem()) : null;
    }
}
