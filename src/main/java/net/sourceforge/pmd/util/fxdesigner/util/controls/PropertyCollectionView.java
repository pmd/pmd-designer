package net.sourceforge.pmd.util.fxdesigner.util.controls;

import static net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil.rewire;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.PopOver;
import org.controlsfx.tools.ValueExtractor;
import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;
import org.controlsfx.validation.decoration.StyleClassValidationDecoration;
import org.kordamp.ikonli.javafx.FontIcon;
import org.reactfx.Subscription;
import org.reactfx.value.Val;

import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.model.PropertyDescriptorSpec;
import net.sourceforge.pmd.util.fxdesigner.popups.EditPropertyDialogController;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;

import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

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


        @Override
        protected void updateItem(PropertyDescriptorSpec item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(item.getName());
                setGraphic(new FontIcon("fas-ellipsis-h"));


                MenuItem editItem = new MenuItem("Edit...");
                editItem.setOnAction(e -> {
                    PopOver popOver = detailsPopOver(item);
                    PopOverUtil.showAt(popOver, getMainStage(), this);
                    Platform.runLater(() -> {
                        if (editItem.getParentMenu() != null) {
                            editItem.getParentMenu().hide();
                        }
                    });
                });

                MenuItem removeItem = new MenuItem("Remove");
                removeItem.setOnAction(e -> getItems().remove(item));

                MenuItem addItem = new MenuItem("Add property...");
                addItem.setOnAction(e -> onAddPropertyClicked("name"));

                ContextMenu fullMenu = new ContextMenu();
                fullMenu.getItems().addAll(editItem, removeItem, new SeparatorMenuItem(), addItem);

                // Reduced context menu, for when there are no properties or none is selected
                MenuItem addItem2 = new MenuItem("Add property...");
                addItem2.setOnAction(e -> onAddPropertyClicked("name"));

                ContextMenu smallMenu = new ContextMenu();
                smallMenu.getItems().add(addItem2);


                this.addEventHandler(MouseEvent.MOUSE_CLICKED, t -> {
                    if (t.getButton() == MouseButton.SECONDARY
                        || t.getButton() == MouseButton.PRIMARY && t.getClickCount() > 1) {
                        if (getSelectionModel().getSelectedItem() != null) {
                            fullMenu.show(this, t.getScreenX(), t.getScreenY());
                        } else {
                            smallMenu.show(this, t.getScreenX(), t.getScreenY());
                        }
                        t.consume();
                    }
                });


                ValidationSupport validation = new ValidationSupport();
                validation.setValidationDecorator(new StyleClassValidationDecoration());
                validation.registerValidator(this, validator());
                Val.wrap(validation.validationResultProperty())
                   .values().subscribe(System.out::println);
            }
        }

        private Validator<PropertyDescriptorSpec> validator() {

            return (lv, spec) -> {
                Validator<String> noWhitespaceName
                    = Validator.createRegexValidator("Name cannot contain whitespace", "\\S*+", Severity.ERROR);
                Validator<String> emptyName = Validator.createEmptyValidator("Name required");
                Validator<String> uniqueName = (c, val) -> {
                    long sameNameDescriptors = getItems().stream()
                                                         .map(PropertyDescriptorSpec::getName)
                                                         .filter(val::equals)
                                                         .count();

                    return new ValidationResult().addErrorIf(c, "The name must be unique", sameNameDescriptors > 1);
                };

                Validator<String> nameValidator = Validator.combine(noWhitespaceName, emptyName, uniqueName);

                return new ValidationResult().addErrorIf(lv, "The name must not be empty", StringUtils.isBlank(spec.getName()));
            };

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
            popOver.titleProperty().bind(spec.nameProperty().map(it -> "Property '" + it + "'"));
            Subscription closeSub = wizard.bindToDescriptor(spec, getItems());
            popOver.setOnHiding(e -> closeSub.unsubscribe());
            return popOver;
        }
    }
}
