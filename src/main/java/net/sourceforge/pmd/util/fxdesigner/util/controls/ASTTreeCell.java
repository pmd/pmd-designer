/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import static net.sourceforge.pmd.util.fxdesigner.util.DumpUtil.dumpToSubtreeTest;

import java.util.function.Consumer;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.util.controls.SearchableTreeView.SearchableTreeCell;

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
    public void commonUpdate(Node item) {
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
