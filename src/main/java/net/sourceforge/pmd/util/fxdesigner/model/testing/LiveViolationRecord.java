/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextRange;

public class LiveViolationRecord {

    private final Var<TextRange> range;
    private final Var<Boolean> exactRange;
    private final Var<String> message;


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

    public TextRange getRange() {
        return range.getValue();
    }

    public Var<TextRange> rangeProperty() {
        return range;
    }

    public void setRange(TextRange range) {
        this.range.setValue(range);
    }

    public Boolean getExactRange() {
        return exactRange.getValue();
    }

    public Var<Boolean> exactRangeProperty() {
        return exactRange;
    }

    public void setExactRange(Boolean exactRange) {
        this.exactRange.setValue(exactRange);
    }

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
