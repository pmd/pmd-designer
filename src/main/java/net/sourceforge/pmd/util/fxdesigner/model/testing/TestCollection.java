/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactfx.EventStream;
import org.reactfx.collection.LiveArrayList;
import org.reactfx.collection.LiveList;

import net.sourceforge.pmd.util.fxdesigner.model.ObservableRuleBuilder;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentSequence;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

public class TestCollection implements SettingsOwner {


    private LiveList<LiveTestCase> stash;
    private final @Nullable ObservableRuleBuilder owner;

    public TestCollection(@Nullable ObservableRuleBuilder owner, List<LiveTestCase> tests) {
        this.stash = new LiveArrayList<>(tests);
        this.owner = owner;
        if (owner != null) {
            stash.forEach(it -> it.setRule(this.owner));
        }
    }

    public void rebase(TestCollection testCases) {
        this.stash.setAll(testCases.stash);
        initOwner();
    }

    public void addAll(TestCollection testCases) {
        this.stash.addAll(testCases.stash);
        initOwner();
    }

    public void initOwner() {
        stash.forEach(it -> it.setRule(owner));
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
        testCase.setRule(owner);
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


    public ObservableRuleBuilder getOwner() {
        return owner;
    }

}
