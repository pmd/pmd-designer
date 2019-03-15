package net.sourceforge.pmd.util.fxdesigner.util.controls;

import static java.util.Objects.requireNonNull;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil.addCustomStyleSheets;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.controlsfx.control.PopOver;

import javafx.css.Styleable;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.stage.Window;

/**
 * @author Clément Fournier
 */
public final class PopOverUtil {

    private PopOverUtil() {

    }


    public static Styleable getStyleableNode(PopOver popOver) {
        try {
            return ((Styleable) FieldUtils.readField(popOver, "bridge", true));
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static void showAt(PopOver popOver, Window owner, javafx.scene.Node anchor) {
        //todo there's a bug with placement
        showAt(popOver, owner, anchor, 4);
    }

    /**
     * Display a popover at an anchor node, but owned by an arbitrary other window.
     * In case of nested popovers, the inner ones should not be owned by the outer
     * one, otherwise their detached behavior is not independent.
     *
     * @see PopOver#show(Node, double)
     */
    public static void showAt(PopOver popOver, Window owner, javafx.scene.Node anchor, double offset) {
        requireNonNull(owner);

        Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());

        switch (popOver.getArrowLocation()) {
        case BOTTOM_CENTER:
        case BOTTOM_LEFT:
        case BOTTOM_RIGHT:
            popOver.show(owner, bounds.getMinX() + bounds.getWidth() / 2,
                         bounds.getMinY() + offset);
            break;
        case LEFT_BOTTOM:
        case LEFT_CENTER:
        case LEFT_TOP:
            popOver.show(owner, bounds.getMaxX() - offset,
                         bounds.getMinY() + bounds.getHeight() / 2);
            break;
        case RIGHT_BOTTOM:
        case RIGHT_CENTER:
        case RIGHT_TOP:
            popOver.show(owner, bounds.getMinX() + offset,
                         bounds.getMinY() + bounds.getHeight() / 2);
            break;
        case TOP_CENTER:
        case TOP_LEFT:
        case TOP_RIGHT:
            popOver.show(owner, bounds.getMinX() + bounds.getWidth() / 2,
                         bounds.getMinY() + bounds.getHeight() - offset);
            break;
        default:
            break;
        }
    }


    /**
     * Must be called after "show".
     *
     * @param popOver
     */
    public static void fixStyleSheets(PopOver popOver) {
        addCustomStyleSheets(((Parent) popOver.getSkin().getNode()), "popover");
    }

}
