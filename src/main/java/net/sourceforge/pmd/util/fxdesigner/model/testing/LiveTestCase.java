/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactfx.EventStream;
import org.reactfx.collection.LiveArrayList;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.testframework.TestDescriptor;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentSequence;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextRange;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

/**
 * Live editable version of a test case.
 */
public class LiveTestCase implements SettingsOwner {

    private final Var<String> source = Var.newSimpleVar("");
    private final Var<String> description = Var.newSimpleVar("");
    private final Var<LanguageVersion> languageVersion = Var.newSimpleVar(null);
    private final ObservableMap<String, String> properties = FXCollections.observableHashMap();
    private final Var<Boolean> dirty = Var.newSimpleVar(false);
    private final LiveList<LiveViolationRecord> expectedViolations = new LiveArrayList<>();
    private Consumer<LiveTestCase> commitHandler;
    private boolean frozen;


    public LiveTestCase() {
        this(t -> {});
    }

    public LiveTestCase(Consumer<LiveTestCase> commitHandler) {
        this.commitHandler = commitHandler;
        //        TODO // modificationTicks().subscribe(tick -> dirty.setValue(true));
    }

    @PersistentProperty
    public String getSource() {
        return source.getValue();
    }

    public Var<String> sourceProperty() {
        return source;
    }

    public void setSource(String source) {
        this.source.setValue(source);
    }

    @PersistentProperty
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
    @PersistentProperty
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

    @PersistentSequence
    public LiveList<LiveViolationRecord> getExpectedViolations() {
        return expectedViolations;
    }

    public ObservableMap<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> stringMap) {
        properties.clear();
        properties.putAll(stringMap);
    }

    public void addCommitHandler(@NonNull Consumer<LiveTestCase> liveTestCaseConsumer) {
        commitHandler = commitHandler.andThen(liveTestCaseConsumer);
    }

    @PersistentProperty
    public Properties getPersistenceOnlyProps() {
        Properties props = new Properties();
        properties.forEach(props::put);
        return props;
    }

    public void setPersistenceOnlyProps(Properties props) {
        props.forEach((k, v) -> properties.put(k.toString(), v.toString()));
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


    public LiveTestCase freeze() {
        LiveTestCase live = unfreeze(t -> {});
        live.frozen = true;
        return live;
    }

    public LiveTestCase unfreeze(Consumer<LiveTestCase> commitHandler) {
        LiveTestCase live = new LiveTestCase(commitHandler);
        live.setDescription(getDescription());
        live.setLanguageVersion(getLanguageVersion());
        live.setDirty(false);
        live.setSource(getSource());
        live.frozen = false;
        return live;
    }


    public static LiveTestCase fromDescriptor(TestDescriptor descriptor) {

        LiveTestCase live = new LiveTestCase(t -> {});
        live.setSource(descriptor.getCode());
        live.setDescription(descriptor.getDescription());

        List<String> lines = Arrays.asList(descriptor.getCode().split("\\r?\\n"));
        List<String> messages = descriptor.getExpectedMessages();
        List<Integer> lineNumbers = descriptor.getExpectedLineNumbers();

        for (int i = 0; i < descriptor.getNumberOfProblemsExpected(); i++) {
            String m = messages.size() > i ? messages.get(i) : null;
            int line = lineNumbers.size() > i ? lineNumbers.get(i) : -1;

            TextRange tr = line >= 0
                           ? TextRange.fullLine(line, lines.get(line).length())
                           : null;

            live.getExpectedViolations().add(new ViolationRecord(tr, false, m).unfreeze());
        }

        descriptor.getProperties().forEach((k, v) -> live.getProperties().put(k.toString(), v.toString()));
        return live;
    }
}
