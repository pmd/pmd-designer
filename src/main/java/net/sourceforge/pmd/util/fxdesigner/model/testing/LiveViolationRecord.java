/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextRange;

public class LiveViolationRecord implements SettingsOwner, Comparable<LiveViolationRecord> {

    private final Var<@Nullable TextRange> range;
    private final Var<Boolean> exactRange;
    private final Var<@Nullable String> message;

    public LiveViolationRecord() {
        this(null, null, false);
    }

    public LiveViolationRecord(int line) {
        this(TextRange.fullLine(line, 10000), null, false);
    }

    public LiveViolationRecord(@Nullable TextRange range, @Nullable String message, boolean exactRange) {
        this.range = Var.newSimpleVar(range);
        this.message = Var.newSimpleVar(message);
        this.exactRange = Var.newSimpleVar(exactRange);
    }

    @Override
    public int compareTo(LiveViolationRecord o) {
        TextRange mine = getRange();
        TextRange theirs = o.getRange();
        if (mine == null || theirs == null) {
            return 0;
        } else {
            return Integer.compare(mine.startPos.line, theirs.startPos.line);
        }
    }

    @PersistentProperty
    @Nullable
    public TextRange getRange() {
        return range.getValue();
    }

    public Var<@Nullable TextRange> rangeProperty() {
        return range;
    }

    public void setRange(@Nullable TextRange range) {
        this.range.setValue(range);
    }

    @PersistentProperty
    public boolean isExactRange() {
        return exactRange.getValue();
    }

    public void setExactRange(boolean exactRange) {
        this.exactRange.setValue(exactRange);
    }

    public Var<Boolean> exactRangeProperty() {
        return exactRange;
    }

    @PersistentProperty
    @Nullable
    public String getMessage() {
        return message.getValue();
    }

    public Var<@Nullable String> messageProperty() {
        return message;
    }

    public void setMessage(@Nullable String message) {
        this.message.setValue(message);
    }


    public LiveViolationRecord deepCopy() {
        return new LiveViolationRecord(
            getRange(),
            getMessage(),
            isExactRange()
        );
    }
}
