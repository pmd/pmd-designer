/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

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
                    .subscribe(v -> popOver.pseudoClassStateChanged(PseudoClass.getPseudoClass("detached"), v));
        EventStreams.valuesOf(popOver.focusedProperty())
                    // JavaFX lacks a focus model that works across several popups and stuff.
                    // The only solution we have to avoid having duplicate carets or so, is
                    // to *not* let a popover that openly has textfields or other controls
                    // that steal focus *be detachable*
                    .subscribe(v -> popOver.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), v));

    }


}
