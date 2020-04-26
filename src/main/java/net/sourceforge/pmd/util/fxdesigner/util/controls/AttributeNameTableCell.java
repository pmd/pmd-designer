/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactfx.value.Val;

import net.sourceforge.pmd.lang.ast.xpath.Attribute;

import javafx.scene.control.Cell;
import javafx.scene.control.TableCell;
import javafx.scene.control.Tooltip;

public class AttributeNameTableCell extends TableCell<Attribute, String> {

    private static final String DEPRECATED_CSS_CLASS = "deprecated-attr";

    private Tooltip tooltip;


    public AttributeNameTableCell() {
        getStyleClass().add("attribute-name");

        Val.wrap(tableRowProperty())
           .flatMap(Cell::itemProperty)
           .values()
           .distinct()
            .subscribe(this::updateAttr);
    }


    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        super.setText(item);
        super.setGraphic(null);
    }


    private void updateAttr(@Nullable Attribute attr) {
        if (tooltip != null) {
            Tooltip.uninstall(this, tooltip);
            getStyleClass().remove(DEPRECATED_CSS_CLASS);
            tooltip = null;
        }

        if (attr == null) {
            return;
        }

        String replacement = attr.replacementIfDeprecated();
        if (replacement != null) {
            String txt = "This attribute is deprecated";
            if (!replacement.isEmpty()) {
                txt += ", please use " + replacement + " instead";
            }
            Tooltip t = new Tooltip(txt);
            tooltip = t;
            getStyleClass().add(DEPRECATED_CSS_CLASS);
            Tooltip.install(this, t);
        }
    }
}
