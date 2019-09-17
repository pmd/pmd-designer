/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import static net.sourceforge.pmd.util.fxdesigner.util.ResourceUtil.resolveResource;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.app.AbstractController;
import net.sourceforge.pmd.util.fxdesigner.app.services.CloseableService;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;


/**
 * A tab pane that can add new tabs with a button.
 *
 * @param <T> Type of controllers for the content of each tab. Conformance of
 *            the controllers to this type must be enforced by the client.
 *
 * @author Clément Fournier
 */
public final class MutableTabPane<T extends AbstractController & TitleOwner> extends AnchorPane {

    /** The TabPane hosting the tabs. */
    private final TabPane tabPane = new TabPane();


    /** Name of the FXML file that will populate the tabs' contents. */
    private final String tabFxmlResource;
    /** Supplier of controllers for each tab. */
    private final Var<Supplier<T>> controllerSupplier = Var.newSimpleVar(() -> null);
    private final Var<@Nullable Function<? super T, ? extends T>> deepCopyFun = Var.newSimpleVar(null);


    public MutableTabPane(@NamedArg("tabFxmlContent") String tabFxmlContent) {
        this.tabFxmlResource = Objects.requireNonNull(tabFxmlContent);

        assert DesignerUtil.getFxml(tabFxmlContent) != null;

        AnchorPane.setRightAnchor(tabPane, 0d);
        AnchorPane.setLeftAnchor(tabPane, 0d);
        AnchorPane.setBottomAnchor(tabPane, 0d);
        AnchorPane.setTopAnchor(tabPane, 0d);

        tabPane.getStyleClass().addAll("mutable-tab-pane");

        getTabs().addListener((ListChangeListener<Tab>) change -> {
            final ObservableList<Tab> tabs = getTabs();
            tabs.get(0).setClosable(tabs.size() > 1);
        });

        getChildren().addAll(tabPane);

        initAddButton();
    }


    private void initAddButton() {

        Val.wrap(tabPane.skinProperty())
            .values()
            .filter(Objects::nonNull)
            .subscribeForOne(skin -> Platform.runLater(this::makeAddButton));

    }

    private void makeAddButton() {
        // Basically we superimpose a transparent HBox over the TabPane
        // it needs to be done in a runLater because we need to access the
        // TabPane's header region, which is created by the TabPane's skin
        // on the first layout

        Region headersRegion = (Region) tabPane.lookup(".headers-region");

        // a pane that always has the size of the header region,
        // pushing the new tab button to its right
        Pane headerSizePane = new Pane();
        headerSizePane.setMouseTransparent(true);
        headerSizePane.prefWidthProperty().bind(headersRegion.widthProperty());

        // the new tab button
        Button newTabButton = new Button();
        newTabButton.getStyleClass().addAll("icon-button", "add-tab-button");
        newTabButton.setTooltip(new Tooltip("Add new tab"));
        newTabButton.setGraphic(new FontIcon("fas-plus"));
        newTabButton.onActionProperty().set(actionEvent -> addTabWithNewController());
        // bind bounds to a square that fits inside the header's region
        newTabButton.maxHeightProperty().bind(headersRegion.heightProperty());
        newTabButton.maxWidthProperty().bind(headersRegion.heightProperty());

        // the copy tab button
        Button copyButton = new Button();
        copyButton.getStyleClass().addAll("icon-button", "duplicate-tab-button");
        copyButton.setTooltip(new Tooltip("Duplicate current tab"));
        copyButton.setGraphic(new FontIcon("far-copy"));
        copyButton.onActionProperty().set(actionEvent -> {
            T cur = currentFocusedController().getValue();
            Function<? super T, ? extends T> copy = deepCopyFun.getValue();
            if (cur == null || copy == null) {
                addTabWithNewController();
            } else {
                addTabWithController(copy.apply(cur));
            }
        });
        // bind bounds to a square that fits inside the header's region
        copyButton.maxHeightProperty().bind(headersRegion.heightProperty());
        copyButton.maxWidthProperty().bind(headersRegion.heightProperty());

        copyButton.visibleProperty().bind(ReactfxUtil.isPresentProperty(deepCopyFun));
        copyButton.managedProperty().bind(copyButton.visibleProperty());

        // Rightmost node, grows to fill the rest of the horizontal space
        Pane spring = new Pane();
        spring.setMouseTransparent(true);
        HBox.setHgrow(spring, Priority.ALWAYS);

        HBox box = new HBox();
        box.getStylesheets().addAll(resolveResource("css/flat.css"), resolveResource("css/designer.css"));
        // makes the HBox's transparent regions click-through
        // https://stackoverflow.com/questions/16876083/javafx-pass-mouseevents-through-transparent-node-to-children
        box.setPickOnBounds(false);
        box.prefHeightProperty().bind(headersRegion.heightProperty());

        box.getChildren().addAll(headerSizePane, newTabButton, copyButton, spring);

        // Fits the HBox's size to the container
        AnchorPane.setTopAnchor(box, 0d);
        AnchorPane.setRightAnchor(box, 0d);
        AnchorPane.setLeftAnchor(box, 0d);

        // don't forget that
        this.getChildren().addAll(box);
    }


    /**
     * Unmodifiable list of controllers for each tab in order. There's at least one.
     */
    public LiveList<T> getControllers() {
        return LiveList.map(getTabs(), this::controllerFromTab);
    }


    /**
     * Currently focused tab.
     */
    public Val<T> currentFocusedController() {
        return Val.map(getSelectionModel().selectedItemProperty(), this::controllerFromTab);
    }


    /**
     * Creates a new tab and assigns it a new controller using
     * the controller supplier ({@link #setControllerSupplier(Supplier)}).
     *
     * @return The controller of the tab
     */
    @SuppressWarnings("UnusedReturnValue")
    public T addTabWithNewController() {
        Tab tab = newTabWithDefaultController();
        addTabAndFocus(tab);
        return controllerFromTab(tab);
    }


    public void addTabWithController(T controller) {
        Tab tab = tabMaker().apply(Objects.requireNonNull(controller));
        addTabAndFocus(tab);
    }


    private void addTabAndFocus(Tab tab) {
        tab.textProperty().bind(uniqueNameBinding(controllerFromTab(tab).titleProperty(), getTabs().size()));

        this.getTabs().add(tab);
        getSelectionModel().select(tab);
        // Finish the initialisation of the tab
        controllerFromTab(tab).afterParentInit();
    }


    public void setControllerSupplier(Supplier<T> supplier) {
        this.controllerSupplier.setValue(supplier);
    }

    public void setDeepCopyFunction(Function<? super T, ? extends T> deepCopyFun) {
        this.deepCopyFun.setValue(deepCopyFun);
    }

    /** Retrieves the controller of a tab. */
    @SuppressWarnings("unchecked")
    private T controllerFromTab(Tab tab) {
        return (T) tab.getUserData();
    }


    private Tab newTabWithDefaultController() {
        return tabMaker().apply(controllerSupplier.getOrElse(() -> null).get());
    }


    /** Makes the title unique w.r.t. already present tabs. */
    private Val<String> uniqueNameBinding(Val<String> titleProperty, int tabIdx) {
        Binding<String> uniqueBinding = Bindings.createStringBinding(
            () -> {
                String title = titleProperty.getOrElse("Unnamed");
                int sameName = 0;
                LiveList<T> controllers = getControllers();
                for (int i = 0; i < controllers.size() && i < tabIdx; i++) {
                    if (title.equals(controllers.get(i).titleProperty().getOrElse("Unnamed"))) {
                        sameName++;
                    }

                }
                return sameName == 0 ? title : title + " (" + sameName + ")";
            },
            titleProperty,
            getTabs()
        );
        return Val.wrap(uniqueBinding);
    }


    private ObservableList<Tab> getTabs() {
        return tabPane.getTabs();
    }


    public SingleSelectionModel<Tab> getSelectionModel() {
        return tabPane.getSelectionModel();
    }

    /**
     * Returns an object creating tabs on demand, populated with the content
     * from the given FXML resource location. Each tab's userdata is its controller,
     * necessarily of type T.
     */
    private Function<T, Tab> tabMaker() {
        return controller -> {
            URL url = DesignerUtil.getFxml(tabFxmlResource);

            if (url == null) {
                System.err.println("Unresolved FXML resource " + tabFxmlResource);
                return null;
            }
            FXMLLoader loader = new FXMLLoader(url);

            if (controller != null) {
                List<AbstractController> lst = new ArrayList<>(controller.getChildren());
                lst.add(0, controller);
                // TODO this adds the children but not descendants
                loader.setControllerFactory(DesignerUtil.controllerFactoryKnowing(lst.toArray()));
            }

            Parent root;
            try {
                root = loader.load();
            } catch (IOException e) {
                System.err.println("Error loading FXML " + tabFxmlResource);
                e.printStackTrace();
                return null;
            }
            Tab newTab = new Tab();
            newTab.setContent(root);

            T realController = loader.getController();
            newTab.setUserData(realController);
            newTab.setOnClosed(evt -> {
                if (realController instanceof CloseableService) {
                    try {
                        ((CloseableService) realController).close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            return newTab;
        };

    }


}
