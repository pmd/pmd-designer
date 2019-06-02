/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactfx.EventStream;
import org.reactfx.collection.LiveArrayList;
import org.reactfx.collection.LiveList;

import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentSequence;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

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
     * Mark the given [testCase] as the only unfrozen one and appends
     * it to the {@link #stash}.
     */
    public void addTestCase(LiveTestCase testCase) {
        if (!testCase.isFrozen()) {
            stash.forEach(LiveTestCase::freeze);
            stash.add(testCase);
        }
    }

    @Nullable
    public LiveTestCase getOpenTest() {
        return stash.stream().filter(it -> !it.isFrozen()).findFirst().orElse(null);
    }

    /**
     * Opens a test case for write access.
     */
    @Nullable
    public LiveTestCase export(int i) {
        if (0 <= i && i < stash.size()) {
            stash.forEach(LiveTestCase::freeze);
            return stash.get(i).unfreeze();
        } else {
            return null;
        }
    }


    public EventStream<?> modificationTicks() {
        return ReactfxUtil.modificationTicks(getStash(), LiveTestCase::modificationTicks);
    }


}
