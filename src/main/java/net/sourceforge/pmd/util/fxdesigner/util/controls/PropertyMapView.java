/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */


package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.controlsfx.control.PopOver;
import org.controlsfx.tools.ValueExtractor;
import org.reactfx.Subscription;
import org.reactfx.collection.LiveArrayList;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.model.ObservableRuleBuilder;
import net.sourceforge.pmd.util.fxdesigner.model.PropertyDescriptorSpec;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveTestCase;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import com.github.oowekyala.rxstring.ReactfxExtensions;
import javafx.beans.NamedArg;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
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
    private final ListView<Pair<PropertyDescriptorSpec, Var<String>>> view;

    private Subscription sub = Subscription.EMPTY;


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


        view = new ListView<>();
        initTableView(view);
        //
        //        AnchorPane footer = new AnchorPane();
        //        footer.setPrefHeight(30);
        //        footer.getStyleClass().addAll("footer");
        //        footer.getStylesheets().addAll(DesignerUtil.getCss("flat").toString());
        //
        //
        //        ToolBar toolBar = new ToolBar();
        //
        //
        //        Button addmapping = new Button();
        //        addmapping.getStyleClass().addAll("icon-button");
        //        addmapping.setGraphic(new FontIcon("fas-plus"));
        //        addmapping.setTooltip(new Tooltip("Add mapping"));
        //
        //        addmapping.setOnAction(e -> {
        //            ObservablePair<String, String> spec = new ObservablePair<>("", "value");
        //            view.getItems().add(spec);
        //        });
        //
        //        Button removeMapping = new Button();
        //        removeMapping.getStyleClass().addAll("icon-button");
        //        removeMapping.setGraphic(new FontIcon("fas-trash"));
        //        removeMapping.setTooltip(new Tooltip("Remove selected elements"));
        //        removeMapping.disableProperty().bind(
        //            LiveList.sizeOf(view.getSelectionModel().getSelectedCells()).map(it -> it == 0)
        //        );
        //        removeMapping.setOnAction(e -> view.getSelectionModel().getSelectedCells().forEach(it -> view.getItems().remove(it.getRow())));
        //
        //        ControlUtil.anchorFirmly(toolBar);
        //
        //        toolBar.getItems().addAll(addmapping, removeMapping);
        //        footer.getChildren().addAll(toolBar);
        this.getChildren().addAll(view);


    }

    @Override
    public DesignerRoot getDesignerRoot() {
        return root;
    }


    public void bind(LiveTestCase testCase) {
        ObservableList<PropertyDescriptorSpec> allProps = Optional.ofNullable(testCase.getRule()).<ObservableList<PropertyDescriptorSpec>>map(ObservableRuleBuilder::getRuleProperties).orElse(FXCollections.emptyObservableList());

        LiveList<Pair<PropertyDescriptorSpec, Var<String>>> properties = LiveList.map(allProps, it -> new Pair<>(it, Var.newSimpleVar(testCase.getProperties().getOrDefault(it.getName(), it.getValue()))));

        view.setItems(new LiveArrayList<>());

        sub = ReactfxUtil.modificationTicks(view.getItems(), p -> p.getValue().values().distinct().or(p.getKey().nameProperty().values().distinct()))
                         .subscribe(tick -> testCase.setProperties(getItems()))
                         .and(
                             ReactfxExtensions.dynamic(
                                 properties,
                                 (e, i) -> {
                                     view.getItems().add(i, e);
                                     return () -> view.getItems().remove(e);
                                 }
                             )
                         );
    }

    public void unbind() {
        sub.unsubscribe();
        sub = Subscription.EMPTY;
    }

    public Map<String, String> getItems() {
        return view.getItems()
                   .stream()
                   .collect(Collectors.toMap(p -> p.getKey().getName(), p -> p.getValue().getValue(), (k, k2) -> k2));
    }


    private void initTableView(@NonNull ListView<Pair<PropertyDescriptorSpec, Var<String>>> view) {
        view.setEditable(true);

        // cancel edit on focus lost
        //        Val.wrap(view.focusedProperty())
        //           .values()
        //           .filter(it -> !it)
        //           .subscribe(it -> view.edit(-1));

        view.setCellFactory(lv -> new PropertyMappingListCell());

        ControlUtil.makeListViewFitToChildren(view, LIST_CELL_HEIGHT);
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

    private class PropertyMappingListCell extends SmartTextFieldListCell<Pair<PropertyDescriptorSpec, Var<String>>> {

        @Override
        protected Pair<Node, Subscription> getNonEditingGraphic(Pair<PropertyDescriptorSpec, Var<String>> testCase) {

            setEditable(true);

            Subscription sub = Subscription.EMPTY;

            Label propName = new Label();
            propName.textProperty().bind(testCase.getKey().nameProperty());

            sub = sub.and(() -> propName.textProperty().unbind());

            Label sep = new Label(" → ");


            Label propValue = new Label();
            propValue.textProperty().bind(testCase.getValue());

            sub = sub.and(() -> propValue.textProperty().unbind());


            HBox box = new HBox();
            box.getChildren().addAll(propName, sep, propValue);
            sub = sub.and(ControlUtil.registerDoubleClickListener(box, this::doStartEdit));

            return new Pair<>(box, sub);
        }

        @Override
        protected Var<String> extractEditable(Pair<PropertyDescriptorSpec, Var<String>> item) {
            return item.getValue();
        }
    }


}
