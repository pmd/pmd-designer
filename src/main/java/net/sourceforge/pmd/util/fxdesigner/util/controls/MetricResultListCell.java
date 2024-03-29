/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.Locale;

import net.sourceforge.pmd.util.fxdesigner.model.MetricResult;

import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.util.Callback;


/**
 * List cell for a metric result.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public class MetricResultListCell extends ListCell<MetricResult> {


    @Override
    protected void updateItem(MetricResult item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(item.getKey().displayName() + " = " + niceDoubleString(item.getValue()));
        }
    }


    /** Gets a nice string representation of a double. */
    private String niceDoubleString(Number val) {
        if (val instanceof Double || val instanceof Float) {
            return String.format(Locale.ROOT, "%.4f %%", val.doubleValue() * 100);
        }
        return val.toString(); // integrals
    }


    public static Callback<ListView<MetricResult>, MetricResultListCell> callback() {
        return param -> new MetricResultListCell();
    }


}
