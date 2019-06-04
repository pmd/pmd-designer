/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import javafx.scene.control.Button;
import javafx.scene.control.Skin;

public class RippleButton extends Button {

    public RippleButton() {
        super();
        getStyleClass().addAll("md-button");
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        final Skin<?> buttonSkin = super.createDefaultSkin();
        RippleEffect.attach(this, this::getChildren);
        return buttonSkin;
    }

}
