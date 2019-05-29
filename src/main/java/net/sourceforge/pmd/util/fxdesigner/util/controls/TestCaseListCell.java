/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import static java.lang.Double.max;
import static java.lang.Math.abs;
import static java.lang.Math.min;

import java.util.List;

import org.kordamp.ikonli.javafx.FontIcon;
import org.reactfx.EventStream;
import org.reactfx.Subscription;
import org.reactfx.value.Val;
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
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.util.Duration;
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

        FontIcon statusIcon = new FontIcon();
        Label statusLabel = new Label();
        statusLabel.setGraphic(statusIcon);
        // todo subscription

        Subscription sub = testCase.statusProperty()
                                   .changes()
                                   .subscribe(ch -> {
                                       TestResult st = ch.getNewValue();
                                       statusIcon.getStyleClass().setAll(st.getStatus().getStyleClass());
                                       statusIcon.setIconLiteral(st.getStatus().getIcon());

                                       if (ch.getOldValue() != null
                                           && st.getStatus() != ch.getOldValue().getStatus()
                                           && st.getStatus() == TestStatus.FAIL) {
                                           getStatusTransition(st.getStatus()).play();
                                       }


                                       String message = st.getMessage();
                                       if (message != null) {
                                           statusLabel.setTooltip(new Tooltip(message));
                                       } else {
                                           statusLabel.setTooltip(null);
                                       }
                                   });


        Label descriptionLabel = new Label(testCase.getDescription());

        descriptionLabel.addEventHandler(MouseEvent.MOUSE_CLICKED,
                                         e -> {
                                             if (e.getButton() == MouseButton.PRIMARY
                                                 && e.getClickCount() > 1) {
                                                 doStartEdit();
                                                 e.consume();
                                             }
                                         });


        Button editDescription = new Button();
        editDescription.setGraphic(new FontIcon("far-edit"));
        editDescription.getStyleClass().addAll("edit-test-description", "icon-button");
        Tooltip.install(editDescription, new Tooltip("Edit test description"));
        editDescription.setOnAction(e1 -> doStartEdit());
        sub = sub.and(() -> editDescription.setOnAction(null));

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

        Button delete = new Button();
        delete.setGraphic(new FontIcon("fas-trash-alt"));
        delete.getStyleClass().addAll("delete-button", "icon-button");
        Tooltip.install(delete, new Tooltip("Remove test case"));
        delete.setOnAction(e -> getListView().getItems().remove(testCase));

        spacer.addEventHandler(MouseEvent.MOUSE_CLICKED,
                               e -> {
                                   if (e.getButton() == MouseButton.PRIMARY
                                       && (e.getClickCount() > 1)) {
                                       load.fire();
                                       e.consume();
                                   }
                               });


        hBox.getChildren().setAll(statusLabel, descriptionLabel, editDescription, spacer, delete, duplicate, load);
        hBox.setAlignment(Pos.CENTER_LEFT);


        MyXPathSubscriber subscriber = new MyXPathSubscriber(testCase, collection.getDesignerRoot());
        sub = sub.and(subscriber.init(getManagerOf(testCase)));


        if (!testCase.isFrozen()) {
            load.fire();
        }

        ControlUtil.makeListCellFitListViewWidth(this);

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

    private Animation getStatusTransition(TestStatus newStatus) {

        return new Transition() {

            {
                setCycleDuration(Duration.millis(1200));
                setInterpolator(Interpolator.EASE_BOTH);
                setOnFinished(t -> applyCss());
            }


            @Override
            protected void interpolate(double frac) {
                Color vColor = newStatus.getColor().deriveColor(0, 1, 1, clip(map(frac)));
                setBackground(new Background(new BackgroundFill(vColor, CornerRadii.EMPTY, Insets.EMPTY)));
            }

            private double map(double x) {
                return -abs(x - 0.5) + 0.5;
            }

            private double clip(double i) {
                return min(1, max(0, i));
            }
        };
    }

    private class MyXPathSubscriber extends XPathUpdateSubscriber {

        private final LiveTestCase testCase;

        public MyXPathSubscriber(LiveTestCase testCase, DesignerRoot root) {
            super(root);
            this.testCase = testCase;
        }

        @Override
        public EventStream<?> additionalTicks() {
            return testCase.modificationTicks();
        }

        @Override
        public void handleNoCompilationUnit() {
            testCase.setStatus(new TestResult(TestStatus.UNKNOWN, "No compilation unit"));
        }


        @Override
        public void handleNoXPath() {
            testCase.setStatus(new TestResult(TestStatus.UNKNOWN, "No XPath query"));
        }

        @Override
        public void handleXPathSuccess(List<net.sourceforge.pmd.lang.ast.Node> results) {
            TestResult result = TestCaseUtil.doTest(testCase, results);
            testCase.setStatus(result);
        }

        @Override
        public void handleXPathError(Exception e) {
            testCase.setStatus(new TestResult(TestStatus.ERROR, DesignerUtil.sanitizeExceptionMessage(e)));
        }
    }
}
