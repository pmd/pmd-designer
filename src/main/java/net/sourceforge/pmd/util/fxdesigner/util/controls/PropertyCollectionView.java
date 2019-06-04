/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.controlsfx.control.PopOver;
import org.controlsfx.tools.ValueExtractor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.reactfx.Subscription;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.properties.PropertyTypeId;
import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.model.ObservableXPathRuleBuilder;
import net.sourceforge.pmd.util.fxdesigner.model.PropertyDescriptorSpec;
import net.sourceforge.pmd.util.fxdesigner.popups.EditPropertyDialogController;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;

import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Pair;

/**
 * @author Clément Fournier
 */
public class PropertyCollectionView extends VBox implements ApplicationComponent {

    private static final int LIST_CELL_HEIGHT = 31;
    @NonNull
    private final DesignerRoot root;
    @NonNull
    private final ListView<PropertyDescriptorSpec> view;
    @NonNull
    private final PopOverWrapper<PropertyDescriptorSpec> myEditPopover;


    static {
        ValueExtractor.addObservableValueExtractor(c -> c instanceof ListCell, c -> ((ListCell) c).itemProperty());
    }


    // for scenebuilder
    @SuppressWarnings("ConstantConditions") // suppress nullability issue
    public PropertyCollectionView() {
        this.root = null;
        this.myEditPopover = null;
        this.view = null;
    }

    public PropertyCollectionView(@NamedArg("designerRoot") DesignerRoot root) {
        this.root = root;

        this.getStyleClass().addAll("property-collection-view");

        view = new ListView<>();
        initListView(view);
        setOwnerStageFactory(root.getMainStage()); // default

        AnchorPane footer = new AnchorPane();
        footer.setPrefHeight(30);
        footer.getStyleClass().addAll("footer");
        footer.getStylesheets().addAll(DesignerUtil.getCss("flat").toString());


        Button addProperty = new Button("Add property");

        ControlUtil.anchorFirmly(addProperty);

        addProperty.setOnAction(e -> {
            PropertyDescriptorSpec spec = new PropertyDescriptorSpec();
            spec.setName(getUniqueNewName());
            view.getItems().add(spec);
            // TODO pop the edit view
        });
        footer.getChildren().addAll(addProperty);
        this.getChildren().addAll(view, footer);

        myEditPopover = new PopOverWrapper<>(this::rebindPopover);

        myEditPopover.rebind(new PropertyDescriptorSpec());
        myEditPopover.doFirstLoad(root.getMainStage());

    }

    public void setOwnerStageFactory(Stage stage) {
        view.setCellFactory(lv -> new PropertyDescriptorCell(stage));
    }

    @Override
    public DesignerRoot getDesignerRoot() {
        return root;
    }

    /**
     * Call this to pop the "new property" popup.
     */
    public void onAddPropertyClicked(String name) {
        PropertyDescriptorSpec spec = new PropertyDescriptorSpec();
        spec.setName(name);
        view.getItems().add(spec);

        Platform.runLater(
            () -> {
                Node node = getChildrenUnmodifiable().get(getChildrenUnmodifiable().size() - 1);
                System.out.println(node);
                ((Button) node.lookup("." + PropertyDescriptorCell.DETAILS_BUTTON_CLASS)).fire();
            });
    }

    /**
     * Gets a unique new name for a property.
     */
    private String getUniqueNewName() {
        return getUniqueNewName("New property");
    }

    private String getUniqueNewName(String attempt) {
        // this suffixing scheme is obnoxious, but nobody should
        // spam the button
        if (getItems().stream().anyMatch(it -> attempt.equals(it.getName()))) {
            return getUniqueNewName(attempt + " (1)");
        } else {
            return attempt;
        }
    }

    /**
     * Make the edit popover for a single spec.
     */
    private PopOver detailsPopOver(PropertyDescriptorSpec spec) {
        EditPropertyDialogController wizard = new EditPropertyDialogController(root);

        FXMLLoader loader = new FXMLLoader(DesignerUtil.getFxml("edit-property-dialog"));
        loader.setController(wizard);

        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        PopOver popOver = new SmartPopover(root);
        popOver.setHeaderAlwaysVisible(true);
        popOver.setDetachable(false); // Well we can't let it be detached, it grabs focus...........
        popOver.setUserData(wizard);
        return rebindPopover(spec, popOver);
    }

    private PopOver rebindPopover(PropertyDescriptorSpec newSpec, PopOver pop) {
        if (pop == null) {
            // create it
            return detailsPopOver(newSpec);
        }
        Optional.ofNullable(pop.getOnHiding()).ifPresent(it -> it.handle(null));

        pop.titleProperty().bind(newSpec.nameProperty()
                                        .filter(StringUtils::isNotBlank)
                                        .orElseConst("(no name)")
                                        .map(it -> "Property '" + it + "'"));

        EditPropertyDialogController wizard = (EditPropertyDialogController) pop.getUserData();
        Subscription sub = wizard.bindToDescriptor(newSpec, view.getItems()).and(pop.titleProperty()::unbind);
        pop.setOnHiding(we -> sub.unsubscribe());
        return pop;

    }

    public void setItems(ObservableList<PropertyDescriptorSpec> ruleProperties) {
        view.setItems(ruleProperties);
    }

    public ObservableList<PropertyDescriptorSpec> getItems() {
        return view.getItems();
    }

    public Val<ObservableList<PropertyDescriptorSpec>> itemsProperty() {
        return Val.wrap(view.itemsProperty());
    }

    private static void initListView(ListView<PropertyDescriptorSpec> view) {

        ControlUtil.makeListViewFitToChildren(view, LIST_CELL_HEIGHT);
        view.setEditable(true);

        ControlUtil.makeListViewNeverScrollHorizontal(view);

        Label placeholder = new Label("No properties yet");
        placeholder.getStyleClass().addAll("placeholder");
        view.setPlaceholder(placeholder);
    }

    /**
     * Makes the property popover for a rule.
     */
    public static PopOver makePopOver(ObservableXPathRuleBuilder rule, Val<String> titleProperty, DesignerRoot designerRoot) {
        PropertyCollectionView view = new PropertyCollectionView(designerRoot);

        view.setItems(rule.getRuleProperties());

        PopOver popOver = new SmartPopover(view);
        popOver.titleProperty().bind(titleProperty.map(it -> "Properties of " + it));
        popOver.setHeaderAlwaysVisible(true);
        popOver.setPrefWidth(150);



        return popOver;
    }

    private class PropertyDescriptorCell extends SmartTextFieldListCell<PropertyDescriptorSpec> {

        private static final String DETAILS_BUTTON_CLASS = "my-details-button";
        private static final String DELETE_BUTTON_CLASS = "delete-property-button";
        private final Stage owner;

        PropertyDescriptorCell(Stage owner) {
            this.owner = owner;
        }


        @Override
        protected Var<String> extractEditable(PropertyDescriptorSpec propertyDescriptorSpec) {
            return propertyDescriptorSpec.nameProperty();
        }


        @Override
        protected Pair<Node, Subscription> getNonEditingGraphic(PropertyDescriptorSpec spec) {

            Subscription sub = Subscription.EMPTY;

            HBox hBox = new HBox();
            hBox.setSpacing(10);
            Label label = new Label();

            label.textProperty().bind(spec.nameProperty()
                                          .filter(StringUtils::isNotBlank)
                                          .orElseConst("(no name)"));

            sub = sub.and(label.textProperty()::unbind);
            sub = sub.and(ControlUtil.registerDoubleClickListener(label, this::doStartEdit));

            Label typeLabel = new Label();
            typeLabel.textProperty().bind(spec.typeIdProperty().map(PropertyTypeId::getStringId).map(it -> ": " + it));

            sub = sub.and(typeLabel.textProperty()::unbind);

            Pane spacer = ControlUtil.spacerPane();

            Button edit = new Button();
            edit.setGraphic(new FontIcon("fas-ellipsis-h"));
            edit.getStyleClass().addAll(DETAILS_BUTTON_CLASS, "icon-button");
            Tooltip.install(edit, new Tooltip("Edit property..."));


            edit.setOnAction(e -> {
                myEditPopover.rebind(spec);
                myEditPopover.showOrFocus(p -> PopOverUtil.showAt(p, owner, this));
            });

            sub = sub.and(() -> edit.setOnAction(null));

            Button delete = new Button();
            delete.setGraphic(new FontIcon("fas-trash-alt"));
            delete.getStyleClass().addAll(DELETE_BUTTON_CLASS, "icon-button");
            Tooltip.install(delete, new Tooltip("Remove property"));
            delete.setOnAction(e -> getItems().remove(spec));

            sub = sub.and(() -> delete.setOnAction(null));


            hBox.getChildren().setAll(label, typeLabel, spacer, delete, edit);
            hBox.setAlignment(Pos.CENTER_LEFT);

            return new Pair<>(hBox, sub);
        }

    }

}
