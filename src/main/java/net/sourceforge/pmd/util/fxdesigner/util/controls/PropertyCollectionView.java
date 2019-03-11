package net.sourceforge.pmd.util.fxdesigner.util.controls;

import static net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil.rewire;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.PopOver;
import org.controlsfx.tools.ValueExtractor;
import org.kordamp.ikonli.javafx.FontIcon;
import org.reactfx.Subscription;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.model.PropertyDescriptorSpec;
import net.sourceforge.pmd.util.fxdesigner.popups.EditPropertyDialogController;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;

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

/**
 * @author Clément Fournier
 */
public class PropertyCollectionView extends ListView<PropertyDescriptorSpec> implements ApplicationComponent {

    private static final int LIST_CELL_HEIGHT = 24;
    private final DesignerRoot root;


    static {
        ValueExtractor.addObservableValueExtractor(c -> c instanceof ListCell, c -> ((ListCell) c).itemProperty());
    }

    public PropertyCollectionView(@NamedArg("designerRoot") DesignerRoot root) {
        this.root = root;

        setFixedCellSize(LIST_CELL_HEIGHT);
        setCellFactory(lv -> new PropertyDescriptorCell());

        Val.wrap(itemsProperty())
           .values()
           .subscribe(e -> rewire(maxHeightProperty(), Bindings.size(e).multiply(LIST_CELL_HEIGHT).add(5)));
    }


    // TODO PopOver context menu


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
    }

    public static PopOver makePopOver(ObservableList<PropertyDescriptorSpec> items, DesignerRoot designerRoot) {
        PropertyCollectionView view = new PropertyCollectionView(designerRoot);
        view.setItems(items);
        PopOver popOver = new PopOver(view);
        popOver.setTitle("Rule properties");
        popOver.setHeaderAlwaysVisible(true);
        return popOver;
    }

    private class PropertyDescriptorCell extends ListCell<PropertyDescriptorSpec> {

        private Var<PopOver> myEditPopover = Var.newSimpleVar(null);

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


            PopOver popOver = new PopOver(root);
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
            edit.getStyleClass().addAll("icon-button");
            edit.setOnAction(e -> {
                if (myEditPopover.isPresent()) {
                    myEditPopover.getValue().requestFocus();
                } else {
                    PopOver popOver = detailsPopOver(spec);
                    myEditPopover.setValue(popOver);
                    PopOverUtil.showAt(popOver, getMainStage(), this);
                    PopOverUtil.fixStyleSheets(popOver);
                }
            });


            hBox.getChildren().setAll(label, spacer, edit);

            hBox.setAlignment(Pos.CENTER_LEFT);

            return hBox;
        }

    }
}
