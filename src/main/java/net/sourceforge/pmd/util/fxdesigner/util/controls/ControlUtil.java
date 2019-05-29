/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.function.Function;

import org.reactfx.EventStreams;
import org.reactfx.Subscription;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;

import com.github.oowekyala.rxstring.ReactfxExtensions;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.css.PseudoClass;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Control;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.layout.Region;
import javafx.stage.Window;
import javafx.util.Callback;

public final class ControlUtil {

    private ControlUtil() {

    }


    public static <T> ListCell<T> makeListCellFitListViewWidth(ListCell<T> cell) {
        if (cell != null) {
            cell.prefWidthProperty().bind(
                Val.wrap(cell.listViewProperty())
                   .flatMap(Region::widthProperty).map(it -> it.doubleValue() - 5)
                   .orElseConst(0.));
            cell.setMaxWidth(Control.USE_PREF_SIZE);
        }
        return cell;
    }

    public static <T> void decorateCellFactory(ListView<T> lv, Function<ListCell<T>, ListCell<T>> f) {

        Callback<ListView<T>, ListCell<T>> originalCellF = lv.getCellFactory();

        lv.setCellFactory(l -> f.apply(originalCellF.call(l)));

    }

    /**
     * This is supported by some CSS.
     *
     * @param lv
     * @param <T>
     */
    public static <T> void makeListViewNeverScrollHorizontal(ListView<T> lv) {

//        decorateCellFactory(lv, ControlUtil::makeListCellFitListViewWidth);

        lv.getStyleClass().addAll("no-horizontal-scroll");

        subscribeOnSkin(lv, skin -> {
            Group group = (Group) skin.getNode().lookup(".sheet");

            final double tolerance = 5;

            BooleanBinding yOverflow = Bindings.createBooleanBinding(
                () -> group.getLayoutBounds().getHeight() - lv.getHeight() > tolerance,
                group.layoutBoundsProperty(),
                lv.heightProperty()
            );

            return EventStreams.valuesOf(yOverflow)
                               .distinct()
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
