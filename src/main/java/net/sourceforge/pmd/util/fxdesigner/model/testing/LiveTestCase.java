/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.util.Arrays;
import java.util.List;
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
import net.sourceforge.pmd.testframework.TestDescriptor;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentSequence;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextRange;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

/**
 * Live editable version of a test case.
 */
public class LiveTestCase implements SettingsOwner {

    private final Var<String> source = Var.newSimpleVar(""); // TODO defaults per language
    private final Var<String> description = Var.newSimpleVar("New test case");
    private final Var<LanguageVersion> languageVersion = Var.newSimpleVar(null);
    private final ObservableMap<String, String> properties = FXCollections.observableHashMap();
    private final LiveList<LiveViolationRecord> expectedViolations = new LiveArrayList<>();
    private final Var<TestResult> status = Var.newSimpleVar(new TestResult(TestStatus.UNKNOWN, null));
    private Consumer<LiveTestCase> commitHandler;
    private final Var<Boolean> frozen = Var.newSimpleVar(true);

    private final UndoManager<TestCaseChange> myUndoModel;

    public LiveTestCase() {
        this(t -> {});
    }

    public LiveTestCase(Consumer<LiveTestCase> commitHandler) {
        this.commitHandler = commitHandler;

        myUndoModel = UndoManagerFactory.unlimitedHistorySingleChangeUM(
            changeStream(),
            TestCaseChange::invert,
            TestCaseChange::redo,
            (a, b) -> Optional.of(a.mergeWith(b))
        );

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


    public Val<Boolean> dirtyProperty() {
        return myUndoModel.undoAvailableProperty().map(it -> !it);
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

    // It's hard to serialize a map bc of the Type parser so we put that into a Properties
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

    @PersistentProperty
    public boolean isFrozen() {
        return frozen.getValue();
    }

    public Var<Boolean> frozenProperty() {
        return frozen;
    }

    public void setFrozen(boolean frozen) {
        this.frozen.setValue(frozen);
    }

    public LiveTestCase freeze() {
        setFrozen(true);
        return this;
    }

    public LiveTestCase unfreeze() {
        setFrozen(false);
        return this;
    }

    public LiveTestCase deepCopy() {
        LiveTestCase live = new LiveTestCase();
        live.setDescription(getDescription());
        live.expectedViolations.setAll(this.expectedViolations);
        live.setProperties(this.properties);
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


        sink.feedFrom(ReactfxUtil.observableMapVal(properties).changes().map(it -> new TestCaseChange(
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
                           ? TextRange.fullLine(line, lines.get(line - 1).length())
                           : null;

            live.getExpectedViolations().add(new LiveViolationRecord(tr, m, false));
        }

        descriptor.getProperties().forEach((k, v) -> live.getProperties().put(k.toString(), v.toString()));
        return live;
    }
}
