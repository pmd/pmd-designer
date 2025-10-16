/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.document.TextRegion;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;

public class LiveViolationRecord implements SettingsOwner, Comparable<LiveViolationRecord> {

    /**
     * The region in which the violation has to occur.
     */
    private final Var<@NonNull TextRegion> region;
    private final Var<@Nullable String> message;
    private int line;


    // this ctor is used by the thing that restores application state
    @SuppressWarnings("unused")
    public LiveViolationRecord() {
        this(-1, TextRegion.caretAt(0), null);
    }

    public LiveViolationRecord(@NonNull TextRegion region, @Nullable String message) {
        this(-1, region, message);
    }


    public LiveViolationRecord(int line, @NonNull TextRegion region, String message) {
        this.line = line;
        this.region = Var.newSimpleVar(region);
        this.message = Var.newSimpleVar(message);
    }


    @Override
    @SuppressWarnings("PMD.OverrideBothEqualsAndHashCodeOnComparable") // not used in a sorted set/map
    public int compareTo(LiveViolationRecord o) {
        TextRegion mine = getRegion();
        TextRegion theirs = o.getRegion();
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
    @NonNull
    public TextRegion getRegion() {
        return region.getValue();
    }


    public Var<@NonNull TextRegion> regionProperty() {
        return region;
    }


    public void setRegion(@NonNull TextRegion region) {
        this.region.setValue(region);
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
            getRegion(),
            getMessage()
        );
    }
}
