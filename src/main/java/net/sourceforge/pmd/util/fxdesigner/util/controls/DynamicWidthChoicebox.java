/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import static net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil.rewire;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

/**
 * A choicebox that fits the width of the currently displayed item instead
 * of taking the width of the largest item.
 *
 * @author Clément Fournier
 */
public class DynamicWidthChoicebox<T> extends ChoiceBox<T> {

    public DynamicWidthChoicebox() {

        ControlUtil.subscribeOnSkin(
            this,
            skin -> {
                Label label = (Label) skin.getNode().lookup(".label");
                StackPane arrow = (StackPane) skin.getNode().lookup(".open-button");
                boolean showArrow = !getStyleClass().contains("no-arrow");

                label.setAlignment(showArrow ? Pos.CENTER_LEFT : Pos.CENTER);

                DoubleBinding widthBinding = Bindings.createDoubleBinding(
                    () -> {
                        Insets myInsets = getInsets();
                        double arrowWidth = showArrow ? arrow.prefWidth(-1) : 0;
                        return myInsets.getLeft() + label.prefWidth(-1) + arrowWidth
                            + myInsets.getRight();
                    },
                    label.widthProperty(),
                    this.getSelectionModel().selectedItemProperty(),
                    this.insetsProperty()
                );


                return rewire(this.prefWidthProperty(), widthBinding);
            }
        );

    }
}
