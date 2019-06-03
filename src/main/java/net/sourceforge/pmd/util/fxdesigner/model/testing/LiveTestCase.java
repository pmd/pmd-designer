/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.fxmisc.undo.UndoManager;
import org.fxmisc.undo.UndoManagerFactory;
import org.reactfx.EventSource;
import org.reactfx.EventStream;
import org.reactfx.collection.LiveArrayList;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.util.fxdesigner.model.ObservableRuleBuilder;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentSequence;

/**
 * Live editable version of a test case.
 */
public class LiveTestCase implements SettingsOwner {

    private final Var<String> source = Var.newSimpleVar(""); // TODO defaults per language
    private final Var<String> description = Var.newSimpleVar("New test case");
    private final Var<LanguageVersion> languageVersion = Var.newSimpleVar(null);
    private final Var<Map<String, String>> properties = Var.newSimpleVar(Collections.emptyMap());
    private final LiveList<LiveViolationRecord> expectedViolations = new LiveArrayList<>();
    private final Var<Boolean> isIgnored = Var.newSimpleVar(false);


    private final Var<ObservableRuleBuilder> rule = Var.newSimpleVar(null);
    private final Var<TestResult> status = Var.newSimpleVar(new TestResult(TestStatus.UNKNOWN, null));
    private Consumer<LiveTestCase> commitHandler;
    private final Var<Boolean> frozen = Var.newSimpleVar(true);

    private final UndoManager<TestCaseChange> myUndoModel;

    public LiveTestCase() {
        this(t -> {});
    }

    public LiveTestCase(Consumer<LiveTestCase> commitHandler) {
        this.commitHandler = t -> commitHandler.accept(t.freeze());

        myUndoModel = UndoManagerFactory.unlimitedHistorySingleChangeUM(
            changeStream(),
            TestCaseChange::invert,
            TestCaseChange::redo,
            (a, b) -> Optional.of(a.mergeWith(b))
        );

        freeze();
    }

    public UndoManager<TestCaseChange> getUndoManager() {
        return myUndoModel;
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
    public boolean isIgnored() {
        return isIgnored.getValue();
    }

    public Var<Boolean> isIgnoredProperty() {
        return isIgnored;
    }

    public void setIgnored(boolean b) {
        isIgnored.setValue(b);
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


    // not persisted
    public ObservableRuleBuilder getRule() {
        return rule.getValue();
    }

    public Var<ObservableRuleBuilder> ruleProperty() {
        return rule;
    }

    public void setRule(ObservableRuleBuilder rule) {
        this.rule.setValue(rule);
    }

    public Val<Boolean> dirtyProperty() {
        return myUndoModel.undoAvailableProperty().map(it -> !it);
    }


    @PersistentSequence
    public LiveList<LiveViolationRecord> getExpectedViolations() {
        return expectedViolations;
    }

    public Map<String, String> getProperties() {
        return properties.getValue();
    }

    public Var<Map<String, String>> propertiesProperty() {
        return properties;
    }

    public void setProperties(Map<String, String> stringMap) {
        properties.setValue(stringMap);
    }

    public void addCommitHandler(@NonNull Consumer<LiveTestCase> liveTestCaseConsumer) {
        commitHandler = commitHandler.andThen(liveTestCaseConsumer);
    }

    // It's hard to serialize a map bc of the Type parser so we put that into a Properties
    @PersistentProperty
    public Properties getPersistenceOnlyProps() {
        Properties props = new Properties();
        properties.getValue().forEach(props::put);
        return props;
    }

    public void setPersistenceOnlyProps(Properties props) {
        Map<String, String> p = new HashMap<>();
        props.forEach((k, v) -> p.put(k.toString(), v.toString()));
        properties.setValue(p);
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

    public boolean isFrozen() {
        return frozen.getValue();
    }

    public Var<Boolean> frozenProperty() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen.setValue(frozen);
    }

    /**
     * Make a mark in the history of this test case and marks the
     * descriptor as "read-only". Bindings may still exist but they
     * shouldn't.
     */
    LiveTestCase freeze() {
        if (!frozen.getValue()) {
            setFrozen(true);
            getUndoManager().mark();
        }
        return this;
    }

    /**
     * Marks this descriptor as open for write.
     */
    public LiveTestCase unfreeze() {
        setFrozen(false);
        return this;
    }

    public LiveTestCase deepCopy() {
        LiveTestCase live = new LiveTestCase();
        live.setDescription(getDescription());
        live.expectedViolations.setAll(this.expectedViolations);
        live.setProperties(getProperties());
        live.setLanguageVersion(getLanguageVersion());
        live.setSource(getSource());
        live.setFrozen(isFrozen());
        return live;
    }

    public TestResult getStatus() {
        return status.getValue();
    }

    public Var<TestResult> statusProperty() {
        return status;
    }

    public void setStatus(TestResult testResult) {
        statusProperty().setValue(testResult);
    }

    public void setStatus(TestStatus status) {
        statusProperty().setValue(new TestResult(status, null));
    }


    public EventStream<TestCaseChange> changeStream() {

        EventSource<TestCaseChange> sink = new EventSource<>();

        sink.feedFrom(sourceProperty().changes().map(it -> new TestCaseChange(
            this,
            it,
            null,
            null,
            null
        )));

        sink.feedFrom(languageVersionProperty().changes().map(it -> new TestCaseChange(
            this,
            null,
            it,
            null,
            null
        )));


        sink.feedFrom(properties.changes().map(it -> new TestCaseChange(
            this,
            null,
            null,
            it,
            null

        )));

        sink.feedFrom(descriptionProperty().changes().map(it -> new TestCaseChange(
            this,
            null,
            null,
            null,
            it
        )));


        return sink;
    }


}
