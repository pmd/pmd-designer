/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextRange;

public class ViolationRecord {

    private final TextRange range;
    private final boolean isExactRange;
    private final String message;


    public ViolationRecord(TextRange range, boolean isExactRange, String message) {
        this.range = range;
        this.isExactRange = isExactRange;
        this.message = message;
    }

    public LiveViolationRecord unfreeze() {
        return new LiveViolationRecord(
            range,
            message,
            isExactRange
        );
    }


    public TextRange getRange() {
        return range;
    }

    public boolean isExactRange() {
        return isExactRange;
    }

    public String getMessage() {
        return message;
    }
}
