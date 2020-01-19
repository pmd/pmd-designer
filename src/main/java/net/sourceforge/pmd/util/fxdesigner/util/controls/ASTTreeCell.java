/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.function.Consumer;

import org.kordamp.ikonli.javafx.FontIcon;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.util.controls.SearchableTreeView.SearchableTreeCell;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;


/**
 * Formats the cell for AST nodes in the main AST TreeView.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public class ASTTreeCell extends SearchableTreeCell<Node> implements ApplicationComponent {

    private final DesignerRoot root;
    private final Consumer<Node> onNodeItemSelected;


    public ASTTreeCell(DesignerRoot root, Consumer<Node> clickHandler) {
        this.root = root;
        this.onNodeItemSelected = clickHandler;
    }

    @Override
    public DesignerRoot getDesignerRoot() {
        return root;
    }

    private ContextMenu buildContextMenu(Node item) {
        ContextMenu contextMenu = new ContextMenuWithNoArrows();
        CustomMenuItem menuItem = new CustomMenuItem(new Label("Export subtree...",
                                                               new FontIcon("fas-external-link-alt")));

        Tooltip tooltip = new Tooltip("Export subtree to a text format");
        Tooltip.install(menuItem.getContent(), tooltip);

        menuItem.setOnAction(
            e -> getService(DesignerRoot.TREE_EXPORT_WIZARD).apply(x -> x.showYourself(x.bindToNode(item)))
        );

        contextMenu.getItems().add(menuItem);

        return contextMenu;
    }


    @Override
    public void commonUpdate(Node item) {
        setContextMenu(buildContextMenu(item));

        DragAndDropUtil.registerAsNodeDragSource(this, item, root);

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
