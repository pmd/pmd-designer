package net.sourceforge.pmd.util.fxdesigner.util.controls;

import static net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil.rewire;

import java.io.IOException;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.PopOver;
import org.controlsfx.tools.ValueExtractor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.reactfx.Subscription;
import org.reactfx.value.Val;

import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.model.ObservableXPathRuleBuilder;
import net.sourceforge.pmd.util.fxdesigner.model.PropertyDescriptorSpec;
import net.sourceforge.pmd.util.fxdesigner.popups.EditPropertyDialogController;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;

import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.binding.Bindings;
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
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * @author Clément Fournier
 */
public class PropertyCollectionView extends ListView<PropertyDescriptorSpec> implements ApplicationComponent {

    private static final int LIST_CELL_HEIGHT = 24;
    private final DesignerRoot root;


    private final PopOverWrapper<PropertyDescriptorSpec> myEditPopover;


    static {
        ValueExtractor.addObservableValueExtractor(c -> c instanceof ListCell, c -> ((ListCell) c).itemProperty());
    }


    public PropertyCollectionView(@NamedArg("designerRoot") DesignerRoot root, ObservableList<PropertyDescriptorSpec> realItems) {
        this.root = root;
        setItems(realItems);

        setFixedCellSize(LIST_CELL_HEIGHT);
        setCellFactory(lv -> new PropertyDescriptorCell());

        Val.wrap(itemsProperty())
           .values()
           .subscribe(e -> rewire(maxHeightProperty(), Bindings.size(e).multiply(LIST_CELL_HEIGHT).add(5)));

        myEditPopover = new PopOverWrapper<>(this::rebindPopover);

        myEditPopover.rebindIfDifferent(new PropertyDescriptorSpec());
        myEditPopover.doFirstLoad(root.getMainStage());

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
        this.getItems().add(spec);

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
     * Makes the property popover for a rule.
     */
    public static PopOver makePopOver(ObservableXPathRuleBuilder rule, Val<String> titleProperty, DesignerRoot designerRoot) {
        VBox vbox = new VBox();
        PropertyCollectionView view = new PropertyCollectionView(designerRoot, rule.getRuleProperties());

        AnchorPane footer = new AnchorPane();
        footer.setPrefHeight(30);
        footer.getStyleClass().addAll("popover-footer");
        footer.getStylesheets().addAll(DesignerUtil.getCss("flat").toString());


        Button addProperty = new Button("Add property");

        AnchorPane.setLeftAnchor(addProperty, 0.);
        AnchorPane.setRightAnchor(addProperty, 0.);
        AnchorPane.setBottomAnchor(addProperty, 0.);
        AnchorPane.setTopAnchor(addProperty, 0.);


        addProperty.setOnAction(e -> {
            PropertyDescriptorSpec spec = new PropertyDescriptorSpec();
            spec.setName(view.getUniqueNewName());
            view.getItems().add(spec);
            // TODO pop the edit view
        });
        footer.getChildren().addAll(addProperty);
        vbox.getChildren().addAll(view, footer);

        PopOver popOver = new SmartPopover(vbox);
        popOver.titleProperty().bind(titleProperty.map(it -> "Properties of " + it));
        popOver.setHeaderAlwaysVisible(true);
        popOver.setPrefWidth(150);
        return popOver;
    }

    private PopOver detailsPopOver(PropertyDescriptorSpec spec) {
        EditPropertyDialogController wizard = new EditPropertyDialogController(root);

        FXMLLoader loader = new FXMLLoader(DesignerUtil.getFxml("edit-property-dialog.fxml"));
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
        Subscription sub = wizard.bindToDescriptor(newSpec, getItems()).and(pop.titleProperty()::unbind);
        pop.setOnHiding(we -> sub.unsubscribe());
        return pop;

    }

    private class PropertyDescriptorCell extends ListCell<PropertyDescriptorSpec> {

        private static final String DETAILS_BUTTON_CLASS = "my-details-button";
        private static final String DELETE_BUTTON_CLASS = "delete-property-button";

        @Override
        protected void updateItem(PropertyDescriptorSpec item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setGraphic(buildGraphic(item));
            }
        }


        private Node buildGraphic(PropertyDescriptorSpec spec) {

            HBox hBox = new HBox();
            Label label = new Label();
            label.textProperty().bind(spec.nameProperty()
                                          .filter(StringUtils::isNotBlank)
                                          .orElseConst("(no name)"));

            Pane spacer = new Pane();
            HBox.setHgrow(spacer, Priority.ALWAYS);


            Button edit = new Button();
            edit.setGraphic(new FontIcon("fas-ellipsis-h"));
            edit.getStyleClass().addAll(DETAILS_BUTTON_CLASS, "icon-button");
            Tooltip.install(edit, new Tooltip("Edit property..."));


            edit.setOnAction(e -> {
                myEditPopover.rebindIfDifferent(spec);
                myEditPopover.showOrFocus(p -> PopOverUtil.showAt(p, getMainStage(), this));
            });

            Button delete = new Button();
            delete.setGraphic(new FontIcon("fas-trash-alt"));
            delete.getStyleClass().addAll(DELETE_BUTTON_CLASS, "icon-button");
            Tooltip.install(delete, new Tooltip("Remove property"));
            delete.setOnAction(e -> getItems().remove(spec));

            hBox.getChildren().setAll(label, spacer, delete, edit);
            hBox.setAlignment(Pos.CENTER_LEFT);

            return hBox;
        }

    }
}
