/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import org.checkerframework.checker.nullness.qual.Nullable;

import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextRange;

public class ViolationRecord implements SettingsOwner {

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


    @PersistentProperty
    public TextRange getRange() {
        return range;
    }

    @PersistentProperty
    public boolean isExactRange() {
        return isExactRange;
    }

    @PersistentProperty
    public String getMessage() {
        return message;
    }
}
