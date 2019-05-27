/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextRange;

public class LiveViolationRecord implements SettingsOwner {

    private final Var<TextRange> range;
    private final Var<Boolean> exactRange;
    private final Var<String> message;

    public LiveViolationRecord() {
        this(null, null, false);
    }

    public LiveViolationRecord(int line) {
        this(TextRange.fullLine(line, 10000), null, false);
    }

    public LiveViolationRecord(TextRange range, String message, boolean exactRange) {
        this.range = Var.newSimpleVar(range);
        this.message = Var.newSimpleVar(message);
        this.exactRange = Var.newSimpleVar(exactRange);
    }


    public ViolationRecord freeze() {
        return new ViolationRecord(
            range.getValue(),
            exactRange.getValue(),
            message.getValue()
        );
    }

    @PersistentProperty
    public TextRange getRange() {
        return range.getValue();
    }

    public Var<TextRange> rangeProperty() {
        return range;
    }

    public void setRange(TextRange range) {
        this.range.setValue(range);
    }

    @PersistentProperty
    public boolean getExactRange() {
        return exactRange.getValue();
    }

    public void setExactRange(boolean exactRange) {
        this.exactRange.setValue(exactRange);
    }

    public Var<Boolean> exactRangeProperty() {
        return exactRange;
    }

    @PersistentProperty
    public String getMessage() {
        return message.getValue();
    }

    public Var<String> messageProperty() {
        return message;
    }

    public void setMessage(String message) {
        this.message.setValue(message);
    }
}
