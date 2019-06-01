/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.controlsfx.control.PopOver;
import org.controlsfx.tools.ValueExtractor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.reactfx.Subscription;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveTestCase;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveViolationRecord;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import javafx.beans.NamedArg;
import javafx.collections.ObservableList;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

/**
 * @author Clément Fournier
 */
public class ViolationCollectionView extends VBox implements ApplicationComponent {

    private static final int LIST_CELL_HEIGHT = 30;
    @NonNull
    private final DesignerRoot root;
    @NonNull
    private final ListView<LiveViolationRecord> view;


    static {
        ValueExtractor.addObservableValueExtractor(c -> c instanceof ListCell, c -> ((ListCell) c).itemProperty());
    }


    // for scenebuilder
    @SuppressWarnings("ConstantConditions") // suppress nullability issue
    public ViolationCollectionView() {
        this.root = null;
        this.view = null;
    }

    public ViolationCollectionView(@NamedArg("designerRoot") DesignerRoot root) {
        this.root = root;

        this.getStyleClass().addAll("property-collection-view");

        view = new ListView<>();
        initListView(view);

        StackPane footer = new StackPane();
        footer.setPrefHeight(LIST_CELL_HEIGHT);
        footer.getStyleClass().addAll("footer");
        footer.getStylesheets().addAll(DesignerUtil.getCss("flat").toString());

        Label addProperty = new Label("Drag and drop nodes from anywhere");
        StackPane.setAlignment(addProperty, Pos.CENTER);


        footer.getChildren().addAll(addProperty);
        this.getChildren().addAll(view, footer);


    }

    @Override
    public DesignerRoot getDesignerRoot() {
        return root;
    }


    public void setItems(ObservableList<LiveViolationRecord> ruleProperties) {
        view.setItems(ruleProperties);
    }

    public ObservableList<LiveViolationRecord> getItems() {
        return view.getItems();
    }

    public Val<ObservableList<LiveViolationRecord>> itemsProperty() {
        return Val.wrap(view.itemsProperty());
    }

    private void initListView(ListView<LiveViolationRecord> view) {

        ControlUtil.makeListViewFitToChildren(view, LIST_CELL_HEIGHT);

        view.setEditable(true);

        DragAndDropUtil.registerAsNodeDragTarget(view, textRange -> {
            LiveViolationRecord record = new LiveViolationRecord();
            record.setRange(textRange);
            record.setExactRange(true);
            getItems().add(record);
        });

        ControlUtil.makeListViewNeverScrollHorizontal(view);

        // go into normal state on window hide
        ControlUtil.subscribeOnWindow(
            this,
            w -> ReactfxUtil.addEventHandler(w.onHiddenProperty(), evt -> view.edit(-1))
        );

        Label placeholder = new Label("No violations expected in this code");
        placeholder.getStyleClass().addAll("placeholder");
        view.setPlaceholder(placeholder);
        view.setCellFactory(lv -> new ViolationCell());
    }

    /**
     * Makes the violation popover for a test case
     */
    public static PopOver makePopOver(LiveTestCase rule, DesignerRoot designerRoot) {
        ViolationCollectionView view = new ViolationCollectionView(designerRoot);

        view.setItems(rule.getExpectedViolations());

        PopOver popOver = new SmartPopover(view);
        popOver.setUserData(view);
        popOver.getRoot().getStylesheets().add(DesignerUtil.getCss("popover").toString());
        popOver.titleProperty().setValue("Expected violations");
        popOver.setHeaderAlwaysVisible(true);

        //        popOver.setPrefWidth(150);
        return popOver;
    }

    private class ViolationCell extends SmartTextFieldListCell<LiveViolationRecord> {

        ViolationCell() {
            getStyleClass().addAll("expected-violation-list-cell");
        }


        @Override
        protected Var<String> extractEditable(LiveViolationRecord liveViolationRecord) {
            return liveViolationRecord.messageProperty();
        }


        @Override
        protected String getPrompt() {
            return "Expected violation message...";
        }

        @Override
        protected Pair<Node, Subscription> getNonEditingGraphic(LiveViolationRecord violation) {

            HBox hBox = new HBox();

            Pane spacer = new Pane();
            HBox.setHgrow(spacer, Priority.ALWAYS);


            Label lineLabel = new Label("(L. " + violation.getRange().startPos.line + ")");
            lineLabel.getStyleClass().addAll("line-label");

            Label messageLabel = new Label();

            ControlUtil.bindLabelPropertyWithDefault(messageLabel, "(no message)", violation.messageProperty());

            ControlUtil.registerDoubleClickListener(messageLabel, this::doStartEdit);


            Button edit = new Button();
            edit.setGraphic(new FontIcon("far-edit"));
            edit.getStyleClass().addAll("edit-message", "icon-button");
            edit.setOnAction(e -> doStartEdit());
            Tooltip.install(edit, new Tooltip("Edit expected message"));


            Button delete = new Button();
            delete.setGraphic(new FontIcon("fas-trash-alt"));
            delete.getStyleClass().addAll("delete-violation-button", "icon-button");
            Tooltip.install(delete, new Tooltip("Remove violation"));
            delete.setOnAction(e -> getItems().remove(violation));

            hBox.getChildren().setAll(lineLabel, messageLabel, edit, spacer, delete);
            hBox.setAlignment(Pos.CENTER_LEFT);

            view.setMinWidth(Math.max(view.getMinWidth(), this.getWidth()));

            return new Pair<>(hBox, Subscription.EMPTY);

        }

    }

}
