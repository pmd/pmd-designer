/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.util.Map;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactfx.Change;

import net.sourceforge.pmd.lang.LanguageVersion;

public class TestCaseChange {


    private final @NonNull LiveTestCase owner;
    private final @Nullable Change<String> newSource;
    private final @Nullable Change<LanguageVersion> newLangVersion;
    private final @Nullable Change<Map<String, @Nullable String>> newPropertyMapping;
    private final @Nullable Change<String> newDescription;

    public TestCaseChange(
        // source is handled by the undo manager of the code area
        @NonNull LiveTestCase owner,
        @Nullable Change<String> newSource,
        @Nullable Change<LanguageVersion> newLangVersion,
        @Nullable Change<Map<String, @Nullable String>> newPropertyMappings,
        @Nullable Change<String> newDescription
    ) {
        this.owner = owner;
        this.newSource = newSource;
        this.newLangVersion = newLangVersion;
        this.newPropertyMapping = newPropertyMappings;
        this.newDescription = newDescription;
    }

    // TODO merge


    public void redo() {
        applyChange(owner);
    }

    public TestCaseChange mergeWith(TestCaseChange latest) {
        return new TestCaseChange(
            owner,
            keepLatest(newSource, latest.newSource),
            keepLatest(newLangVersion, latest.newLangVersion),
            keepLatest(newPropertyMapping, latest.newPropertyMapping),
            keepLatest(newDescription, latest.newDescription)
        );
    }

    public TestCaseChange invert() {
        return new TestCaseChange(
            owner,
            invert(newSource),
            invert(newLangVersion),
            invert(newPropertyMapping),
            invert(newDescription)
        );
    }

    private void applyChange(LiveTestCase live) {
        if (getNewDescription() != null) {
            live.setDescription(getNewDescription().getNewValue());
        }

        if (newSource != null) {
            live.setSource(newSource.getNewValue());
        }

        if (getNewLangVersion() != null) {
            live.setLanguageVersion(getNewLangVersion().getNewValue());
        }

        if (getNewPropertyMapping() != null) {
            live.setProperties(getNewPropertyMapping().getNewValue());
        }
    }

    public @Nullable Change<LanguageVersion> getNewLangVersion() {
        return newLangVersion;
    }

    public @Nullable Change<Map<String, @Nullable String>> getNewPropertyMapping() {
        return newPropertyMapping;
    }

    public @Nullable Change<String> getNewDescription() {
        return newDescription;
    }

    private static <T> Change<T> invert(Change<T> c) {
        return c == null ? null : new Change<>(c.getNewValue(), c.getOldValue());
    }

    private static <T> Change<T> keepLatest(Change<T> c0, Change<T> c) {
        if (c0 == null) {
            return c;
        } else if (c == null) {
            return c0;
        } else {
            return new Change<>(c0.getOldValue(), c.getNewValue());
        }
    }

    public static class ViolationRecordChange {


        private final LiveViolationRecord owner;
        private final Change<Integer> lineChange;
        private final Change<String> messageChange;

        public ViolationRecordChange(LiveViolationRecord owner, Change<Integer> lineChange, Change<String> messageChange) {
            this.owner = owner;
            this.lineChange = lineChange;
            this.messageChange = messageChange;
        }


    }

}
