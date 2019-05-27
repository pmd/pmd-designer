/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactfx.collection.LiveArrayList;
import org.reactfx.collection.LiveList;

import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentSequence;

public class TestCollection implements SettingsOwner {


    private LiveList<LiveTestCase> stash;

    public TestCollection(List<LiveTestCase> tests) {
        this.stash = new LiveArrayList<>(tests);
    }

    public void rebase(TestCollection testCases) {
        this.stash.setAll(testCases.stash);
    }


    @PersistentSequence
    public LiveList<LiveTestCase> getStash() {
        return stash;
    }

    /**
     * Opens a test case for write access.
     */
    @Nullable
    public LiveTestCase export(int i) {
        if (0 <= i && i < stash.size()) {
            return stash.get(i).unfreeze(it -> stash.set(i, it.freeze()));
        } else {
            return null;
        }
    }


}
