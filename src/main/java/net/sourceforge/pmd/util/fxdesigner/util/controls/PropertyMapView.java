/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */


package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.controlsfx.control.PopOver;
import org.controlsfx.tools.ValueExtractor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.reactfx.Subscription;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.model.PropertyDescriptorSpec;
import net.sourceforge.pmd.util.fxdesigner.model.export.LiveTreeRenderer;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveTestCase;

import javafx.beans.NamedArg;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
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
        this.getChildren().addAll(view);
    }

    @Override
    public DesignerRoot getDesignerRoot() {
        return root;
    }


    public void bind(LiveTestCase testCase) {
        view.setItems(testCase.getLiveProperties().asList());
    }

    public void bind(LiveTreeRenderer descriptor) {
        view.setItems(descriptor.getLiveProperties().asList());
    }

    public void unbind() {
        view.setItems(FXCollections.emptyObservableList());
    }

    public Map<String, String> getItems() {
        return view.getItems()
                   .stream()
                   .collect(Collectors.toMap(p -> p.getKey().getName(), p -> p.getValue().getValue(), (k, k2) -> k2));
    }


    private void initTableView(@NonNull ListView<Pair<PropertyDescriptorSpec, Var<String>>> view) {
        view.setEditable(true);

        //        ControlUtil.makeListViewNeverScrollHorizontal(view);

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
        setHeader(view, popOver, "Properties for this test case");
        return popOver;
    }

    /**
     * Makes the property popover for a rule.
     */
    public static PopOver makePopOver(LiveTreeRenderer renderer, DesignerRoot designerRoot) {
        PropertyMapView view = new PropertyMapView(designerRoot);

        view.bind(renderer);

        PopOver popOver = new SmartPopover(view);
        setHeader(view, popOver, "Options for this renderer");
        return popOver;
    }

    private static void setHeader(PropertyMapView view, PopOver popOver, String s) {
        popOver.setTitle(s);
        popOver.setHeaderAlwaysVisible(true);
        popOver.setPrefWidth(150);
        popOver.setUserData(view);
    }

    private final class PropertyMappingListCell extends SmartTextFieldListCell<Pair<PropertyDescriptorSpec, Var<String>>> {

        @Override
        protected Pair<Node, Subscription> getNonEditingGraphic(Pair<PropertyDescriptorSpec, Var<String>> pair) {

            setEditable(true);

            Subscription sub = Subscription.EMPTY;

            Label propName = new Label();
            propName.textProperty().bind(pair.getKey().nameProperty());

            sub = sub.and(() -> propName.textProperty().unbind());

            Label sep = new Label(" → ");


            Label propValue = new Label();
            propValue.textProperty().bind(pair.getValue());

            sub = sub.and(() -> propValue.textProperty().unbind());

            Pane spacer = new Pane();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            FontIcon defaultIcon = new FontIcon("fas-map-marker");
            Label defaultLabel = new Label("", defaultIcon);
            defaultLabel.visibleProperty().bind(
                pair.getValue().flatMap(it -> pair.getKey().valueProperty())
                    .map(it -> Objects.equals(it, pair.getValue().getValue()))
            );
            defaultLabel.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            defaultLabel.setTooltip(new Tooltip("Default value"));

            sub = sub.and(() -> defaultLabel.visibleProperty().unbind());

            HBox box = new HBox();
            box.getChildren().addAll(propName, sep, propValue, spacer, defaultLabel);
            sub = sub.and(ControlUtil.registerDoubleClickListener(box, this::doStartEdit));

            return new Pair<>(box, sub);
        }

        @Override
        protected Var<String> extractEditable(Pair<PropertyDescriptorSpec, Var<String>> item) {
            return item.getValue();
        }
    }


}
