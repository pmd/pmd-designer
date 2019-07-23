/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
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
    private final LiveList<LiveViolationRecord> expectedViolations = new LiveArrayList<>();
    private final Var<Boolean> isIgnored = Var.newSimpleVar(false);

    private final PropertyMapModel liveProperties = new PropertyMapModel(null);

    private final Var<ObservableRuleBuilder> rule = Var.newSimpleVar(null);
    private final Var<TestResult> status = Var.newSimpleVar(new TestResult(TestStatus.UNKNOWN, null));
    private Consumer<LiveTestCase> commitHandler = t -> {};
    private final Var<Boolean> frozen = Var.newSimpleVar(true);



    public LiveTestCase() {
        this(null);
    }

    public LiveTestCase(@Nullable ObservableRuleBuilder owner) {
        freeze();
        rule.setValue(owner);

        rule.values().subscribe(
            r -> liveProperties.setKnownProperties(r == null ? null : r.getRuleProperties())
        );
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

    @PersistentSequence
    public LiveList<LiveViolationRecord> getExpectedViolations() {
        return expectedViolations;
    }


    public PropertyMapModel getLiveProperties() {
        return liveProperties;
    }

    public Val<Map<String, String>> nonDefaultProperties() {
        return liveProperties.nonDefaultProperty();
    }

    public void setProperty(String name, String value) {
        getLiveProperties().setProperty(name, value);
    }

    public void addCommitHandler(@NonNull Consumer<LiveTestCase> liveTestCaseConsumer) {
        commitHandler = commitHandler.andThen(liveTestCaseConsumer);
    }

    // It's hard to serialize a map bc of the Type parser so we put that into a Properties
    @PersistentProperty
    public Properties getPersistenceOnlyProps() {
        Properties props = new Properties();
        getLiveProperties().getNonDefault().forEach(props::put);
        return props;
    }

    public void setPersistenceOnlyProps(Properties props) {
        props.forEach((k, v) -> liveProperties.setProperty(k.toString(), v.toString()));
    }

    /**
     * Commits the changes.
     */
    public void commitChanges() {
        commitHandler.accept(freeze());
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
        LiveTestCase live = new LiveTestCase(getRule());
        live.setDescription(getDescription());
        live.expectedViolations.setAll(this.expectedViolations.stream().map(LiveViolationRecord::deepCopy).collect(Collectors.toList()));
        live.setLanguageVersion(getLanguageVersion());
        live.setSource(getSource());
        live.setFrozen(isFrozen());
        liveProperties.getNonDefault().forEach(live.liveProperties::setProperty);

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


}
