/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.List;

import org.kordamp.ikonli.javafx.FontIcon;

import net.sourceforge.pmd.util.fxdesigner.TestCollectionController;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.XPathUpdateSubscriber;
import net.sourceforge.pmd.util.fxdesigner.app.services.ASTManager;
import net.sourceforge.pmd.util.fxdesigner.app.services.ASTManagerImpl;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveTestCase;
import net.sourceforge.pmd.util.fxdesigner.model.testing.TestCaseUtil;
import net.sourceforge.pmd.util.fxdesigner.model.testing.TestResult;
import net.sourceforge.pmd.util.fxdesigner.model.testing.TestStatus;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

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

    private TestCollectionController collection;

    private TextField textField;

    private MyXPathSubscriber subscriber;

    public TestCaseListCell(TestCollectionController testCollectionController) {
        this.collection = testCollectionController;
    }

    @Override
    public void updateItem(LiveTestCase item, boolean empty) {
        super.updateItem(item, empty);

        if (isEmpty() || item == null) {
            setGraphic(null);
            textField = null;
            if (subscriber != null) {
                subscriber.unsubscribe();
                subscriber = null;
            }
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
        hBox.setSpacing(10);

        Label label = new Label(testCase.getDescription());

        FontIcon statusIcon = new FontIcon();
        Label statusLabel = new Label();
        statusLabel.setGraphic(statusIcon);
        testCase.statusProperty().values()
                .subscribe(st -> {
                    statusIcon.getStyleClass().setAll(st.getStatus().getStyleClass());
                    statusIcon.setIconLiteral(st.getStatus().getIcon());

                    String message = st.getMessage();
                    if (message != null) {
                        statusLabel.setTooltip(new Tooltip(message));
                    } else {
                        statusLabel.setTooltip(null);
                    }
                });

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button load = new Button();
        load.setGraphic(new FontIcon("fas-external-link-alt"));
        load.getStyleClass().addAll("edit-button", "icon-button");
        Tooltip.install(load, new Tooltip("Load test case in editor"));


        load.setOnAction(e -> collection.loadTestCase(getIndex()));

        //            Button delete = new Button();
        //            delete.setGraphic(new FontIcon("fas-trash-alt"));
        //            delete.getStyleClass().addAll(DELETE_BUTTON_CLASS, "icon-button");
        //            Tooltip.install(delete, new Tooltip("Remove property"));
        //            delete.setOnAction(e -> getItems().remove(spec));

        hBox.getChildren().setAll(statusLabel, label, spacer, load);
        hBox.setAlignment(Pos.CENTER_LEFT);

        if (subscriber != null) {
            subscriber.unsubscribe();
        }

        subscriber = new MyXPathSubscriber(testCase, collection.getDesignerRoot());
        subscriber.init(getManagerOf(testCase));


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

    private ASTManager getManagerOf(LiveTestCase testCase) {

        ASTManagerImpl manager = new ASTManagerImpl(collection.getDesignerRoot());
        manager.sourceCodeProperty().bind(testCase.sourceProperty());
        manager.languageVersionProperty().bind(testCase.languageVersionProperty().orElse(collection.getDefaultLanguageVersion()));
        manager.ruleProperties().bind(ReactfxUtil.observableMapVal(testCase.getProperties()));

        return manager;
    }

    private class MyXPathSubscriber extends XPathUpdateSubscriber {

        private final LiveTestCase testCase;

        public MyXPathSubscriber(LiveTestCase testCase, DesignerRoot root) {
            super(root);
            this.testCase = testCase;
        }


        @Override
        public void handleNoCompilationUnit() {
            testCase.setStatus(TestStatus.UNKNOWN);
        }

        @Override
        public void handleXPathSuccess(List<net.sourceforge.pmd.lang.ast.Node> results) {
            TestResult result = TestCaseUtil.doTest(testCase, results);
            testCase.setStatus(result);
        }

        @Override
        public void handleXPathError(Exception e) {
            testCase.setStatus(TestStatus.ERROR);
        }
    }
}
