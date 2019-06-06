/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import org.kordamp.ikonli.javafx.FontIcon;

import javafx.beans.NamedArg;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;

public class ModifiedIcon extends StackPane {


    public ModifiedIcon(@NamedArg("base") String baseLiteral, @NamedArg("modifier") String modLiteral) {
        FontIcon base = new FontIcon(baseLiteral);
        base.getStyleClass().addAll("modified-icon");

        getChildren().addAll(base);
        StackPane.setAlignment(base, Pos.CENTER);

        FontIcon mod = new FontIcon(modLiteral);
        mod.getStyleClass().addAll("modifier-icon");
        getChildren().addAll(mod);
        StackPane.setAlignment(mod, Pos.BOTTOM_RIGHT);
    }


}
