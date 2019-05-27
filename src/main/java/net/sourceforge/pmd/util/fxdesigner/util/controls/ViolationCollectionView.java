/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import static net.sourceforge.pmd.util.fxdesigner.app.NodeSelectionSource.NODE_RANGE_DATA_FORMAT;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.controlsfx.control.PopOver;
import org.controlsfx.tools.ValueExtractor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.reactfx.Subscription;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveTestCase;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveViolationRecord;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextRange;

import javafx.beans.NamedArg;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

/**
 * @author Clément Fournier
 */
public class ViolationCollectionView extends VBox implements ApplicationComponent {

    private static final int LIST_CELL_HEIGHT = 30;
    public static final String NODE_DRAG_OVER = "node-drag-over";
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

        AnchorPane footer = new AnchorPane();
        footer.setPrefHeight(30);
        footer.getStyleClass().addAll("footer");
        footer.getStylesheets().addAll(DesignerUtil.getCss("flat").toString());

        Label addProperty = new Label("Drag and drop nodes from anywhere");


        AnchorPane.setLeftAnchor(addProperty, 0.);
        AnchorPane.setRightAnchor(addProperty, 0.);
        AnchorPane.setBottomAnchor(addProperty, 0.);
        AnchorPane.setTopAnchor(addProperty, 0.);


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
        view.setFixedCellSize(LIST_CELL_HEIGHT);

        view.setPrefWidth(250);

        view.maxHeightProperty().bind(
            Val.wrap(view.itemsProperty())
               .flatMap(LiveList::sizeOf).map(it -> it * LIST_CELL_HEIGHT + 15)
        );

        view.setEditable(true);

        addDragHandlers(view);


        Label placeholder = new Label("No violations expected in this code");
        placeholder.getStyleClass().addAll("placeholder");
        view.setPlaceholder(placeholder);
        view.setCellFactory(lv -> new ViolationCell());
    }

    private void addDragHandlers(ListView<LiveViolationRecord> view) {
        view.setOnDragOver(evt -> {
            if (evt.getGestureSource() != view &&
                evt.getDragboard().hasContent(NODE_RANGE_DATA_FORMAT)) {
                /* allow for both copying and moving, whatever user chooses */
                evt.acceptTransferModes(TransferMode.LINK);
            }
            evt.consume();
        });

        view.setOnDragEntered(evt -> {
            if (evt.getGestureSource() != view &&
                evt.getDragboard().hasContent(NODE_RANGE_DATA_FORMAT)) {
                view.getStyleClass().addAll(NODE_DRAG_OVER);
            }
            evt.consume();
        });

        view.setOnDragExited(evt -> {
            view.getStyleClass().remove(NODE_DRAG_OVER);
            evt.consume();
        });

        view.setOnDragDropped(evt -> {

            boolean success = false;

            Dragboard db = evt.getDragboard();
            if (db.hasContent(NODE_RANGE_DATA_FORMAT)) {
                TextRange content = (TextRange) db.getContent(NODE_RANGE_DATA_FORMAT);

                LiveViolationRecord record = new LiveViolationRecord();
                record.setRange(content);
                record.setExactRange(true);
                getItems().add(record);
                success = true;
            }

            evt.setDropCompleted(success);

            evt.consume();
        });
    }

    /**
     * Makes the violation popover for a test case
     */
    public static PopOver makePopOver(LiveTestCase rule, DesignerRoot designerRoot) {
        ViolationCollectionView view = new ViolationCollectionView(designerRoot);

        view.setItems(rule.getExpectedViolations());

        PopOver popOver = new SmartPopover(view);
        popOver.getRoot().getStylesheets().add(DesignerUtil.getCss("popover").toString());
        popOver.titleProperty().setValue("Expected violations");
        popOver.setHeaderAlwaysVisible(true);
        popOver.setPrefWidth(150);
        return popOver;
    }

    private class ViolationCell extends SmartTextFieldListCell<LiveViolationRecord> {

        private static final String DETAILS_BUTTON_CLASS = "my-details-button";
        private static final String DELETE_BUTTON_CLASS = "delete-property-button";

        public ViolationCell() {
        }


        @Override
        protected Var<String> extractEditable(LiveViolationRecord liveViolationRecord) {
            return liveViolationRecord.messageProperty();
        }

        @Override
        protected Pair<Node, Subscription> getNonEditingGraphic(LiveViolationRecord violation) {

            HBox hBox = new HBox();
            Label label = new Label();
            label.textProperty().bind(violation.messageProperty()
                                               .filter(StringUtils::isNotBlank)
                                               .orElseConst("(message not asserted)"));

            Pane spacer = new Pane();
            HBox.setHgrow(spacer, Priority.ALWAYS);


            Label lineLabel = new Label(violation.getRange().startPos.toString());

            Button edit = new Button();
            edit.setGraphic(new FontIcon("fas-ellipsis-h"));
            edit.getStyleClass().addAll(DETAILS_BUTTON_CLASS, "icon-button");
            Tooltip.install(edit, new Tooltip("Edit property..."));


            Button delete = new Button();
            delete.setGraphic(new FontIcon("fas-trash-alt"));
            delete.getStyleClass().addAll(DELETE_BUTTON_CLASS, "icon-button");
            Tooltip.install(delete, new Tooltip("Remove property"));
            delete.setOnAction(e -> getItems().remove(violation));

            hBox.getChildren().setAll(lineLabel, label, spacer, delete, edit);
            hBox.setAlignment(Pos.CENTER_LEFT);

            view.setMinWidth(Math.max(view.getMinWidth(), this.getWidth()));

            return new Pair<>(hBox, Subscription.EMPTY);

        }

    }

}
