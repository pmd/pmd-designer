/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import java.io.File;

import org.kordamp.ikonli.javafx.FontIcon;

import net.sourceforge.pmd.util.fxdesigner.app.AbstractController;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.services.LogEntry.Category;
import net.sourceforge.pmd.util.fxdesigner.model.ObservableXPathRuleBuilder;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveTestCase;
import net.sourceforge.pmd.util.fxdesigner.model.testing.StashedTestCase;
import net.sourceforge.pmd.util.fxdesigner.model.testing.TestCollection;
import net.sourceforge.pmd.util.fxdesigner.model.testing.TestXmlParser;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ToolbarTitledPane;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;

public class TestCollectionController extends AbstractController {

    @FXML
    private ToolbarTitledPane titledPane;
    @FXML
    private ListView<StashedTestCase> testsListView;
    @FXML
    private Button importTestsButton;
    @FXML
    private Button addTestButton;
    @FXML
    private Button exportTestsButton;

    private final ObservableXPathRuleBuilder builder;

    protected TestCollectionController(DesignerRoot root, ObservableXPathRuleBuilder builder) {
        super(root);
        this.builder = builder;
    }


    @Override
    protected void beforeParentInit() {

        testsListView.setCellFactory(c -> new TestCaseListCell());
        this.testsListView.setItems(getTestCollection().getStash());

        importTestsButton.setOnAction(any -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Load source from file");
            File file = chooser.showOpenDialog(getMainStage());

            TestCollection coll = TestXmlParser.readXmlTestFile(file.toPath(), e -> logUserException(e, Category.TEST_LOADING_EXCEPTION));
            // TODO what if there's already test cases?
            getTestCollection().rebase(coll);
        });

    }

    private TestCollection getTestCollection() {
        return builder.getTestCollection();
    }


    public void loadTestCase(int index) {
        LiveTestCase live = getTestCollection().export(index);
        if (live == null) {
            logInternalException(new RuntimeException("Wrong index in test list: " + index));
            return;
        }
        ReactfxUtil.rewireInit(live.sourceProperty(), getService(DesignerRoot.AST_MANAGER).sourceCodeProperty());
    }


    private class TestCaseListCell extends ListCell<StashedTestCase> {

        @Override
        protected void updateItem(StashedTestCase item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setGraphic(buildGraphic(item));
            }
        }


        private Node buildGraphic(StashedTestCase testCase) {

            HBox hBox = new HBox();
            Label label = new Label(testCase.getDescription());

            Pane spacer = new Pane();
            HBox.setHgrow(spacer, Priority.ALWAYS);


            Button load = new Button();
            load.setGraphic(new FontIcon("fas-external-link-alt"));
            load.getStyleClass().addAll("edit-button", "icon-button");
            Tooltip.install(load, new Tooltip("Load test case in editor"));


            load.setOnAction(e -> loadTestCase(getIndex()));

            //            Button delete = new Button();
            //            delete.setGraphic(new FontIcon("fas-trash-alt"));
            //            delete.getStyleClass().addAll(DELETE_BUTTON_CLASS, "icon-button");
            //            Tooltip.install(delete, new Tooltip("Remove property"));
            //            delete.setOnAction(e -> getItems().remove(spec));

            hBox.getChildren().setAll(label, spacer, load);
            hBox.setAlignment(Pos.CENTER_LEFT);

            return hBox;


        }

    }

}
