/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.List;

import org.kordamp.ikonli.javafx.FontIcon;
import org.reactfx.Subscription;
import org.reactfx.value.Var;

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

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.util.Pair;

/**
 * A copycat of {@link TextFieldListCell}, because it deletes the graphic and
 * is anyway quite simple.
 */
public class TestCaseListCell extends SmartTextFieldListCell<LiveTestCase> {

    private TestCollectionController collection;

    public TestCaseListCell(TestCollectionController testCollectionController) {
        this.collection = testCollectionController;
    }

    protected Pair<Node, Subscription> getNonEditingGraphic(LiveTestCase testCase) {
        HBox hBox = new HBox();
        hBox.setSpacing(10);

        Label label = new Label(testCase.getDescription());

        FontIcon statusIcon = new FontIcon();
        Label statusLabel = new Label();
        statusLabel.setGraphic(statusIcon);
        // todo subscription

        Subscription sub = testCase.statusProperty().values()
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

        Button duplicate = new Button();
        duplicate.setGraphic(new FontIcon("far-copy"));
        duplicate.getStyleClass().addAll("duplicate-test", "icon-button");
        Tooltip.install(duplicate, new Tooltip("Copy test case"));
        duplicate.setOnAction(e1 -> collection.duplicate(testCase));
        sub = sub.and(() -> duplicate.setOnAction(null));


        ToggleButton load = new ToggleButton();
        load.setToggleGroup(collection.getLoadedToggleGroup());
        load.setGraphic(new FontIcon("fas-external-link-alt"));
        load.getStyleClass().addAll("load-button", "icon-button");
        Tooltip.install(load, new Tooltip("Load test case in editor"));

        load.setUserData(testCase);
        load.setOnAction(e -> collection.loadTestCase(getIndex()));

        sub = sub.and(() -> load.setOnAction(null));

        //            Button delete = new Button();
        //            delete.setGraphic(new FontIcon("fas-trash-alt"));
        //            delete.getStyleClass().addAll(DELETE_BUTTON_CLASS, "icon-button");
        //            Tooltip.install(delete, new Tooltip("Remove property"));
        //            delete.setOnAction(e -> getItems().remove(spec));

        label.maxWidthProperty().bind(
            Bindings.subtract(
                collection.testsListView.widthProperty(),
                Bindings.add(
                    Bindings.add(
                        load.widthProperty(),
                        duplicate.widthProperty()
                    ),
                    50// spacing + sep
                )

            )
        );


        hBox.getChildren().setAll(statusLabel, label, spacer, duplicate, new Separator(Orientation.VERTICAL), load);
        hBox.setAlignment(Pos.CENTER_LEFT);


        MyXPathSubscriber subscriber = new MyXPathSubscriber(testCase, collection.getDesignerRoot());
        sub = sub.and(subscriber.init(getManagerOf(testCase)));

        Platform.runLater(() -> {
            if (!testCase.isFrozen()) {
                load.fire();
            }
        });

        return new Pair<>(hBox, sub); // TODO
    }

    @Override
    protected Var<String> extractEditable(LiveTestCase testCase) {
        return testCase.descriptionProperty();
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
