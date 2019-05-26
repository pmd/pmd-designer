/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactfx.EventStream;
import org.reactfx.collection.LiveArrayList;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.LanguageVersion;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

/**
 * Live editable version of a test case.
 */
public class LiveTestCase {

    private final Var<String> source = Var.newSimpleVar("");
    private final Var<String> description = Var.newSimpleVar("");
    private final Var<LanguageVersion> languageVersion = Var.newSimpleVar(null);
    private final ObservableMap<String, String> properties = FXCollections.observableHashMap();
    private final Var<Boolean> dirty = Var.newSimpleVar(false);
    private final LiveList<LiveViolationRecord> expectedViolations = new LiveArrayList<>();
    private final Consumer<LiveTestCase> commitHandler;


    LiveTestCase(Consumer<LiveTestCase> commitHandler) {
        this.commitHandler = commitHandler;
        modificationTicks().subscribe(tick -> dirty.setValue(true));
    }

    public String getSource() {
        return source.getValue();
    }

    public Var<String> sourceProperty() {
        return source;
    }

    public void setSource(String source) {
        this.source.setValue(source);
    }

    public String getDescription() {
        return description.getValue();
    }

    public Var<String> descriptionProperty() {
        return description;
    }

    public void setDescription(String description) {
        this.description.setValue(description);
    }

    @Nullable
    public LanguageVersion getLanguageVersion() {
        return languageVersion.getValue();
    }

    public Var<LanguageVersion> languageVersionProperty() {
        return languageVersion;
    }

    public void setLanguageVersion(LanguageVersion languageVersion) {
        this.languageVersion.setValue(languageVersion);
    }

    public Boolean getDirty() {
        return dirty.getValue();
    }

    public Var<Boolean> dirtyProperty() {
        return dirty;
    }

    public void setDirty(Boolean dirty) {
        this.dirty.setValue(dirty);
    }

    public LiveList<LiveViolationRecord> getExpectedViolations() {
        return expectedViolations;
    }

    public ObservableMap<String, String> getProperties() {
        return properties;
    }

    /**
     * Commits the changes.
     */
    public void commitChanges() {
        commitHandler.accept(this);
    }

    public EventStream<?> modificationTicks() {
        return sourceProperty().values()
                               .or(languageVersionProperty().values())
                               .or(expectedViolations.changes());
    }

    public StashedTestCase freeze() {
        return new StashedTestCase(
            getSource(),
            getDescription(),
            getLanguageVersion(),
            properties,
            expectedViolations.map(LiveViolationRecord::freeze)
        );
    }

}
