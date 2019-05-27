/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import org.kordamp.ikonli.javafx.FontIcon;

import net.sourceforge.pmd.util.fxdesigner.TestCollectionController;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveTestCase;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

/**
 * A copycat of {@link TextFieldListCell}, because it deletes the graphic and
 * is anyway quite simple.
 */
public class TestCaseListCell extends ListCell<LiveTestCase> {

    private TestCollectionController testCollectionController;

    private TextField textField;

    public TestCaseListCell(TestCollectionController testCollectionController) {
        this.testCollectionController = testCollectionController;
    }

    @Override
    public void updateItem(LiveTestCase item, boolean empty) {
        super.updateItem(item, empty);

        if (isEmpty() || item == null) {
            setGraphic(null);
            textField = null;
        } else {
            if (isEditing()) {
                if (textField != null) {
                    textField.setText(item.getDescription());
                } else {
                    textField = getEditingGraphic(item);
                }
                setGraphic(textField);
            } else {
                setGraphic(getNonEditingGraphic(item));
            }
        }
    }


    private Node getNonEditingGraphic(LiveTestCase testCase) {
        HBox hBox = new HBox();

        Label label = new Label(testCase.getDescription());

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button load = new Button();
        load.setGraphic(new FontIcon("fas-external-link-alt"));
        load.getStyleClass().addAll("edit-button", "icon-button");
        Tooltip.install(load, new Tooltip("Load test case in editor"));


        load.setOnAction(e -> testCollectionController.loadTestCase(getIndex()));

        //            Button delete = new Button();
        //            delete.setGraphic(new FontIcon("fas-trash-alt"));
        //            delete.getStyleClass().addAll(DELETE_BUTTON_CLASS, "icon-button");
        //            Tooltip.install(delete, new Tooltip("Remove property"));
        //            delete.setOnAction(e -> getItems().remove(spec));

        hBox.getChildren().setAll(label, spacer, load);
        hBox.setAlignment(Pos.CENTER_LEFT);

        return hBox;
    }


    private TextField getEditingGraphic(LiveTestCase testCase) {
        final TextField textField = new TextField(testCase.getDescription());

        // Use onAction here rather than onKeyReleased (with check for Enter),
        // as otherwise we encounter RT-34685
        textField.setOnAction(event -> {
            testCase.setDescription(textField.getText());
            commitEdit(testCase);
            event.consume();
        });
        textField.setOnKeyReleased(t -> {
            if (t.getCode() == KeyCode.ESCAPE) {
                cancelEdit();
                t.consume();
            }
        });
        return textField;
    }


    @Override
    public void startEdit() {
        super.startEdit();
        if (textField == null) {
            textField = getEditingGraphic(getItem());
        }
        textField.setText(getItem().getDescription());

        setGraphic(textField);

        textField.selectAll();

        // requesting focus so that key input can immediately go into the
        // TextField (see RT-28132)
        textField.requestFocus();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setGraphic(getNonEditingGraphic(getItem()));
    }

}
