/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;

import com.github.oowekyala.rxstring.ReactfxExtensions;
import javafx.css.PseudoClass;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Window;
import javafx.util.Callback;

public final class ControlUtil {

    private ControlUtil() {

    }

    public static void makeTextFieldShowPromptEvenIfFocused(TextField field) {

        // See css
        field.textProperty().addListener((obs, old, n) -> field.pseudoClassStateChanged(PseudoClass.getPseudoClass("empty-input"), StringUtils.isBlank(n)));

    }

    public static void registerDoubleClickListener(javafx.scene.Node node, Runnable action) {

        node.addEventHandler(MouseEvent.MOUSE_CLICKED,
                             e -> {
                                 if (e.getButton() == MouseButton.PRIMARY
                                     && (e.getClickCount() > 1)) {
                                     action.run();
                                     e.consume();
                                 }
                             });

    }


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
}
