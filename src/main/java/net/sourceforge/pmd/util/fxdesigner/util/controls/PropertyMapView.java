/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */


package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.controlsfx.control.PopOver;
import org.controlsfx.tools.ValueExtractor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.reactfx.collection.LiveArrayList;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveTestCase;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import javafx.beans.NamedArg;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

/**
 * @author Clément Fournier
 */
public class PropertyMapView extends VBox implements ApplicationComponent {

    private static final int LIST_CELL_HEIGHT = 24;
    @NonNull
    private final DesignerRoot root;
    @NonNull
    private final TableView<Pair<Var<String>, Var<String>>> view;

    private LiveTestCase bound;


    static {
        ValueExtractor.addObservableValueExtractor(c -> c instanceof ListCell, c -> ((ListCell) c).itemProperty());
    }


    // for scenebuilder
    @SuppressWarnings("ConstantConditions") // suppress nullability issue
    public PropertyMapView() {
        this.root = null;
        this.view = null;
    }

    public PropertyMapView(@NamedArg("designerRoot") DesignerRoot root) {
        this.root = root;

        this.getStyleClass().addAll("property-map-view");


        view = new TableView<>();
        initTableView(view);

        AnchorPane footer = new AnchorPane();
        footer.setPrefHeight(30);
        footer.getStyleClass().addAll("footer");
        footer.getStylesheets().addAll(DesignerUtil.getCss("flat").toString());


        ToolBar toolBar = new ToolBar();


        Button addmapping = new Button();
        addmapping.getStyleClass().addAll("icon-button");
        addmapping.setGraphic(new FontIcon("fas-plus"));
        addmapping.setTooltip(new Tooltip("Add mapping"));

        addmapping.setOnAction(e -> {
            Pair<Var<String>, Var<String>> spec = new Pair<>(Var.newSimpleVar("name"), Var.newSimpleVar("value"));
            view.getItems().add(spec);
        });

        Button removeMapping = new Button();
        removeMapping.getStyleClass().addAll("icon-button");
        removeMapping.setGraphic(new FontIcon("fas-trash"));
        removeMapping.setTooltip(new Tooltip("Remove selected elements"));
        removeMapping.disableProperty().bind(
            LiveList.sizeOf(view.getSelectionModel().getSelectedCells()).map(it -> it == 0)
        );
        removeMapping.setOnAction(e -> view.getSelectionModel().getSelectedCells().forEach(it -> view.getItems().remove(it.getRow())));

        ControlUtil.anchorFirmly(toolBar);

        toolBar.getItems().addAll(addmapping, removeMapping);
        footer.getChildren().addAll(toolBar);
        this.getChildren().addAll(view, footer);


    }

    @Override
    public DesignerRoot getDesignerRoot() {
        return root;
    }


    public void bind(LiveTestCase testCase) {
        bound = testCase;
        view.setItems(ReactfxUtil.observableMapList(testCase.getProperties()).map(it -> new Pair<>(Var.newSimpleVar(it.getKey()), Var.newSimpleVar(it.getValue()))).stream().collect(Collectors.toCollection(LiveArrayList::new)));
    }

    public void unbind() {
        if (bound != null) {
            bound.setProperties(getItems());
            bound = null;
        }
    }

    public ObservableMap<String, String> getItems() {
        ObservableMap<String, String> map = FXCollections.observableHashMap();
        view.getItems().forEach(kv -> map.put(kv.getKey().getValue(), kv.getValue().getValue()));
        return map;
    }


    private static void initTableView(TableView<Pair<Var<String>, Var<String>>> view) {


        TableColumn<Pair<Var<String>, Var<String>>, String> name = new TableColumn<>("Property");
        name.setCellValueFactory((cdf) -> cdf.getValue().getKey());
        name.setCellFactory(TextFieldTableCell.forTableColumn());


        TableColumn<Pair<Var<String>, Var<String>>, String> value = new TableColumn<>("Value");
        value.setCellValueFactory((cdf) -> cdf.getValue().getValue());
        value.setCellFactory(TextFieldTableCell.forTableColumn());

        view.setEditable(true);
        name.setEditable(true);
        value.setEditable(true);

        view.getColumns().add(name);
        view.getColumns().add(value);

        ControlUtil.makeTableViewFitToChildren(view, LIST_CELL_HEIGHT);
        view.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        value.prefWidthProperty()
             .bind(view.widthProperty()
                       .subtract(name.getWidth())
                       .subtract(2)); // makes it work


        Label placeholder = new Label("No properties yet");
        placeholder.getStyleClass().addAll("placeholder");
        view.setPlaceholder(placeholder);
    }

    /**
     * Makes the property popover for a rule.
     */
    public static PopOver makePopOver(LiveTestCase testCase, DesignerRoot designerRoot) {
        PropertyMapView view = new PropertyMapView(designerRoot);

        view.bind(testCase);

        PopOver popOver = new SmartPopover(view);
        popOver.setTitle("Properties for this test case");
        popOver.setHeaderAlwaysVisible(true);
        popOver.setPrefWidth(150);
        popOver.setUserData(view);
        return popOver;
    }

}
