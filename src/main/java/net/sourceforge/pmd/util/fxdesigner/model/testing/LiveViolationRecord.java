/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.document.TextRegion;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;

public class LiveViolationRecord implements SettingsOwner, Comparable<LiveViolationRecord> {

    private final Var<@Nullable TextRegion> range;
    private final Var<Boolean> exactRange;
    private final Var<@Nullable String> message;
    private int line;


    public LiveViolationRecord() {
        this(null, null, false);
    }


    public LiveViolationRecord(@Nullable TextRegion range, @Nullable String message, boolean exactRange) {
        this.line = -1;
        this.range = Var.newSimpleVar(range);
        this.message = Var.newSimpleVar(message);
        this.exactRange = Var.newSimpleVar(exactRange);
    }

    public LiveViolationRecord(int line, @Nullable String message, boolean exactRange) {
        this.line = line;
        this.range = Var.newSimpleVar(null);
        this.message = Var.newSimpleVar(message);
        this.exactRange = Var.newSimpleVar(exactRange);
    }

    @Override
    public int compareTo(LiveViolationRecord o) {
        TextRegion mine = getRange();
        TextRegion theirs = o.getRange();
        if (mine == null || theirs == null) {
            return 0;
        } else {
            return mine.compareTo(theirs);
        }
    }


    @PersistentProperty
    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }


    @PersistentProperty
    @Nullable
    public TextRegion getRange() {
        return range.getValue();
    }

    public Var<@Nullable TextRegion> rangeProperty() {
        return range;
    }

    public void setRange(@Nullable TextRegion range) {
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
