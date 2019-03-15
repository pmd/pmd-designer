package net.sourceforge.pmd.util.fxdesigner.util.controls;

import org.controlsfx.control.PopOver;
import org.reactfx.EventStreams;

import javafx.css.PseudoClass;
import javafx.scene.Node;

/**
 * @author Clément Fournier
 */
public class SmartPopover extends PopOver {


    public SmartPopover() {
        super();
        registerPseudoClassListeners(this);
    }

    public SmartPopover(Node contents) {
        super(contents);
        registerPseudoClassListeners(this);
    }

    private static void registerPseudoClassListeners(SmartPopover popOver) {

        EventStreams.valuesOf(popOver.detachedProperty())
                    .hook(System.out::println)
                    .subscribe(v -> popOver.pseudoClassStateChanged(PseudoClass.getPseudoClass("detached"), v));
        EventStreams.valuesOf(popOver.focusedProperty())
                    .subscribe(v -> popOver.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), v));

    }


}
