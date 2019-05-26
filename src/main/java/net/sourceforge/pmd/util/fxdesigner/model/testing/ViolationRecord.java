/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import org.checkerframework.checker.nullness.qual.Nullable;

import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextRange;

public class ViolationRecord {

    @Nullable
    private final TextRange range;
    private final boolean isExactRange;
    @Nullable
    private final String message;


    public ViolationRecord(@Nullable TextRange range, boolean isExactRange, @Nullable String message) {
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
