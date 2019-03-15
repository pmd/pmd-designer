package net.sourceforge.pmd.util.fxdesigner.util.controls;

import static net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil.rewire;

import java.util.Objects;

import org.reactfx.EventStreams;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;

/**
 * A choicebox that fits the width of the currently displayed item instead
 * of taking the width of the largest item.
 *
 * @author Clément Fournier
 */
public class DynamicWidthChoicebox<T> extends ChoiceBox<T> {


    public DynamicWidthChoicebox() {

        EventStreams.valuesOf(skinProperty())
                    .filter(Objects::nonNull)
                    .map(skin -> (Label) skin.getNode().lookup(".label"))
                    .subscribe(
                        label -> {
                            label.setAlignment(Pos.CENTER);

                            DoubleBinding widthBinding = Bindings.createDoubleBinding(
                                () -> {
                                    Insets myInsets = getInsets();
                                    return myInsets.getLeft() + label.prefWidth(-1) + myInsets.getRight();
                                },
                                label.widthProperty(),
                                this.getSelectionModel().selectedItemProperty(),
                                this.insetsProperty());


                            rewire(this.prefWidthProperty(), widthBinding);
                        }
                    );


    }

}
