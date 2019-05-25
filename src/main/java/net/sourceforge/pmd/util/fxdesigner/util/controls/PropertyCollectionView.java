package net.sourceforge.pmd.util.fxdesigner.util.controls;

import static net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil.rewire;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.PopOver;
import org.controlsfx.tools.ValueExtractor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.reactfx.Subscription;
import org.reactfx.value.Val;

import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.model.PropertyDescriptorSpec;
import net.sourceforge.pmd.util.fxdesigner.popups.EditPropertyDialogController;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.FakeTailObservableList;

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
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * @author Clément Fournier
 */
public class PropertyCollectionView extends ListView<PropertyDescriptorSpec> implements ApplicationComponent {

    private static final int LIST_CELL_HEIGHT = 24;
    private int id = 0;
    private final DesignerRoot root;


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

    private String getUniqueNewName() {
        return "New property (" + id++ + ")";
    }

    public static PopOver makePopOver(ObservableList<PropertyDescriptorSpec> items, DesignerRoot designerRoot) {
        VBox vbox = new VBox();
        PropertyCollectionView view = new PropertyCollectionView(designerRoot, items);
        Pane footer = new Pane();
        footer.setPrefHeight(30);
        footer.getStyleClass().addAll("popover-footer");
        footer.getStylesheets().addAll(DesignerUtil.getCss("flat").toString());
        Button addProperty = new Button("Add property");
        addProperty.setOnAction(e -> {
            PropertyDescriptorSpec spec = new PropertyDescriptorSpec();
            spec.setName(view.getUniqueNewName());
            view.getItems().add(spec);
        });
        footer.getChildren().addAll(addProperty);
        vbox.getChildren().addAll(view, footer);

        PopOver popOver = new SmartPopover(vbox);
        popOver.setTitle("Rule properties");
        popOver.setHeaderAlwaysVisible(true);
        return popOver;
    }

    private class PropertyDescriptorCell extends ListCell<PropertyDescriptorSpec> {

        private static final String DETAILS_BUTTON_CLASS = "my-details-button";
        private final PopOverWrapper<PropertyDescriptorSpec> myEditPopover = new PopOverWrapper<>(null, () -> null);

        @Override
        protected void updateItem(PropertyDescriptorSpec item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                myEditPopover.rebindIfDifferent(item, () -> detailsPopOver(item));
                setGraphic(buildGraphic(item));
            }
        }


        private PopOver detailsPopOver(PropertyDescriptorSpec spec) {
            EditPropertyDialogController wizard = new EditPropertyDialogController();

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
            popOver.titleProperty().bind(spec.nameProperty()
                                             .filter(StringUtils::isNotBlank)
                                             .orElseConst("(no name)")
                                             .map(it -> "Property '" + it + "'"));
            Subscription closeSub = wizard.bindToDescriptor(spec, getItems());
            popOver.setOnHiding(e -> closeSub.unsubscribe());
            return popOver;
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
            edit.setOnAction(e -> myEditPopover.showOrFocus(p -> PopOverUtil.showAt(p, getMainStage(), this)));

            hBox.getChildren().setAll(label, spacer, edit);

            hBox.setAlignment(Pos.CENTER_LEFT);

            return hBox;
        }

    }
}
