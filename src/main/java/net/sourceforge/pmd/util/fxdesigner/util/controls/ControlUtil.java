/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import javafx.scene.control.Control;
import javafx.scene.control.ListCell;

public final class ControlUtil {

    private ControlUtil() {

    }


    public static void makeListCellFitListViewWidth(ListCell<?> cell) {

        cell.prefWidthProperty().bind(cell.getListView().widthProperty().subtract(5));
        cell.setMaxWidth(Control.USE_PREF_SIZE);

    }

}
