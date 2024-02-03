/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;

import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.services.LogEntry.Category;
import net.sourceforge.pmd.util.fxdesigner.popups.SimplePopups;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import com.github.oowekyala.rxstring.ReactfxExtensions;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Callback;

public final class ControlUtil {

    private ControlUtil() {

    }

    public static void anchorFirmly(Node node) {
        AnchorPane.setLeftAnchor(node, 0.);
        AnchorPane.setRightAnchor(node, 0.);
        AnchorPane.setBottomAnchor(node, 0.);
        AnchorPane.setTopAnchor(node, 0.);
    }

    /**
     * When the [boundProp] is blank, display instead the [defaultText]
     * as grayed.
     */
    public static void bindLabelPropertyWithDefault(Label label, String defaultText, Val<String> boundProp) {
        Val<String> filteredContent = boundProp.filter(StringUtils::isNotBlank);
        label.textProperty().bind(filteredContent.orElseConst(defaultText));

        filteredContent.values().subscribe(it -> label.pseudoClassStateChanged(PseudoClass.getPseudoClass("default-message"),
                                                                               it == null));

    }

    /**
     * Make a list view fit precisely the height of its items.
     *
     * @param view            The listview to configure
     * @param fixedCellHeight The cell height to use, a good default is 24
     */
    public static void makeListViewFitToChildren(ListView<?> view, double fixedCellHeight) {
        view.setFixedCellSize(fixedCellHeight);

        view.maxHeightProperty().bind(
            Val.wrap(view.itemsProperty())
               .flatMap(LiveList::sizeOf).map(it -> it == 0 ? fixedCellHeight : it * fixedCellHeight + 5)
        );
    }

    /**
     * Make a list view fit precisely the height of its items.
     *
     * @param view            The listview to configure
     * @param fixedCellHeight The cell height to use, a good default is 24
     */
    public static void makeTableViewFitToChildren(TableView<?> view, double fixedCellHeight) {
        view.setFixedCellSize(fixedCellHeight);

        subscribeOnSkin(view, skin -> {

            Region header = (Region) skin.getNode().lookup(".nested-column-header");

            view.maxHeightProperty().bind(
                Val.wrap(view.itemsProperty())
                   .flatMap(LiveList::sizeOf).map(it -> header.prefHeight(-1) + (it == 0 ? fixedCellHeight
                                                                                         : it * fixedCellHeight + 5))
            );

            return view.maxHeightProperty()::unbind;
        });

    }

    /**
     * By default text fields don't show the prompt when the caret is
     * inside the field, even if the text is empty.
     */
    public static void makeTextFieldShowPromptEvenIfFocused(TextField field) {
        // See css

        Val.wrap(field.textProperty())
           .values()
           .withDefaultEvent(field.getText())
            .subscribe(text -> field.pseudoClassStateChanged(PseudoClass.getPseudoClass("empty-input"), StringUtils.isBlank(text)));

    }

    public static Pane spacerPane() {
        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    public static Subscription registerDoubleClickListener(javafx.scene.Node node, Runnable action) {
        return ReactfxUtil.addEventHandler(
            node,
            MouseEvent.MOUSE_CLICKED,
            e -> {
                if (e.getButton() == MouseButton.PRIMARY
                    && e.getClickCount() > 1) {
                    action.run();
                    e.consume();
                }
            });

    }

    /**
     * Make a list cell never overflow the width of its container, to
     * avoid having a horizontal scroll bar showing up. This defers
     * resizing constraints to the contents of the cell.
     *
     * @return The given cell
     */
    public static <T> ListCell<T> makeListCellFitListViewWidth(ListCell<T> cell) {
        if (cell != null) {
            cell.prefWidthProperty().bind(
                Val.wrap(cell.listViewProperty())
                   .flatMap(Region::widthProperty).map(it -> it.doubleValue() - 5)
                   .orElseConst(0.)
            );
            cell.setMaxWidth(Control.USE_PREF_SIZE);
        }
        return cell;
    }

    public static <T> void decorateCellFactory(ListView<T> lv, Function<ListCell<T>, ListCell<T>> f) {

        Callback<ListView<T>, ListCell<T>> originalCellF = lv.getCellFactory();

        lv.setCellFactory(l -> f.apply(originalCellF.call(l)));

    }

    /**
     * This is supported by some CSS. Hides the horizontal scroll, and
     * alters the padding when the vertical scrollbar is shown so that
     * the whole contents of the list cell are shown.
     *
     * @param lv List view to alter
     */
    public static <T> void makeListViewNeverScrollHorizontal(ListView<T> lv) {


        lv.getStyleClass().addAll("no-horizontal-scroll");
        subscribeOnSkin(lv, skin -> {
            ScrollBar scroll = (ScrollBar) skin.getNode().lookup(".scroll-bar:vertical");

            return EventStreams.valuesOf(scroll.visibleProperty())
                               .subscribe(it -> lv.pseudoClassStateChanged(PseudoClass.getPseudoClass("vertical-scroll-showing"), it));
        });


    }

    /**
     * Add a hook on the owner window. It's not possible to do this statically,
     * since at construction time the window might not be set.
     */
    public static void subscribeOnWindow(javafx.scene.Node node,
                                         Function<Window, Subscription> hook) {
        ReactfxExtensions.dynamic(
            LiveList.wrapVal(Val.wrap(node.sceneProperty()).flatMap(Scene::windowProperty)),
            (w, i) -> hook.apply(w)
        );
    }

    /**
     * Add a hook on the owner window. It's not possible to do this statically,
     * since at construction time the window might not be set.
     */
    public static void subscribeOnSkin(Control node,
                                       Function<Skin<?>, Subscription> hook) {
        ReactfxExtensions.dynamic(
            LiveList.wrapVal(node.skinProperty()),
            (w, i) -> hook.apply(w)
        );
    }

    public static void copyToClipboardButton(Button button, Supplier<String> copiedText) {
        button.setOnAction(e -> {
            final ClipboardContent content = new ClipboardContent(); // NOPMD - can't use Map<> because of putString(...) method
            content.putString(copiedText.get());
            Clipboard.getSystemClipboard().setContent(content);
            SimplePopups.showActionFeedback(button, AlertType.CONFIRMATION, "Copied to clipboard");
        });
    }

    public static void saveToFileButton(Button button, Stage popupStage, Supplier<String> content, ApplicationComponent owner, Supplier<String> initialFileName) {
        button.setOnAction(e -> {

            FileChooser chooser = new FileChooser();
            chooser.setInitialFileName(initialFileName.get());
            chooser.setTitle("Write to a file");
            File file = chooser.showSaveDialog(popupStage);

            if (file != null) {

                try (OutputStream is = Files.newOutputStream(file.toPath());
                     Writer out = new BufferedWriter(new OutputStreamWriter(is))) {

                    out.write(content.get());
                    SimplePopups.showActionFeedback(button, AlertType.CONFIRMATION, "File saved");

                } catch (IOException ex) {
                    owner.logUserException(ex, Category.TEST_EXPORT_EXCEPTION);
                }
            }
        });
    }
}
