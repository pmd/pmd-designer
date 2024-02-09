/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.reactfx.collection.LiveArrayList;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.rule.RulePriority;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveTestCase;
import net.sourceforge.pmd.util.fxdesigner.model.testing.TestCollection;
import net.sourceforge.pmd.util.fxdesigner.util.AuxLanguageRegistry;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentSequence;

import javafx.collections.ObservableList;


/**
 * Holds info about a rule, and can build it to validate it.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public class ObservableRuleBuilder implements SettingsOwner {

    private final Var<Language> language = Var.newSimpleVar(AuxLanguageRegistry.defaultLanguage());
    private final Var<String> name = Var.newSimpleVar(null);
    private final Var<Class<?>> clazz = Var.newSimpleVar(null);

    // doesn't contain the "xpath" and "version" properties for XPath rules
    private final LiveList<PropertyDescriptorSpec> ruleProperties = new LiveArrayList<>();
    private final LiveList<String> examples = new LiveArrayList<>();

    private final Var<LanguageVersion> minimumVersion = Var.newSimpleVar(null);
    private final Var<LanguageVersion> maximumVersion = Var.newSimpleVar(null);


    private final Var<String> since = Var.newSimpleVar("");

    private final Var<String> message = Var.newSimpleVar("");
    private final Var<String> externalInfoUrl = Var.newSimpleVar("");
    private final Var<String> description = Var.newSimpleVar("");

    private final Var<RulePriority> priority = Var.newSimpleVar(RulePriority.MEDIUM);
    private final Var<Boolean> deprecated = Var.newSimpleVar(false);

    private final TestCollection testCollection = new TestCollection(this, Collections.emptyList());


    @PersistentProperty // CUSTOM?
    public Language getLanguage() {
        return language.getValue();
    }


    public void setLanguage(Language language) {
        this.language.setValue(language);
    }


    public Var<Language> languageProperty() {
        return language;
    }


    @PersistentProperty
    public String getName() {
        return name.getValue();
    }


    public void setName(String name) {
        this.name.setValue(name);
    }


    public Var<String> nameProperty() {
        return name;
    }


    @PersistentProperty
    public Class<?> getClazz() {
        return clazz.getValue();
    }


    public final void setClazz(Class<?> clazz) {
        this.clazz.setValue(clazz);
    }


    public Var<Class<?>> clazzProperty() {
        return clazz;
    }


    @PersistentSequence
    public LiveList<PropertyDescriptorSpec> getRuleProperties() {
        return ruleProperties;
    }


    public void setRuleProperties(ObservableList<PropertyDescriptorSpec> ruleProperties) {
        this.ruleProperties.setAll(ruleProperties);
    }


    public Var<ObservableList<PropertyDescriptorSpec>> rulePropertiesProperty() {
        return Var.fromVal(Val.constant(ruleProperties), this::setRuleProperties);
    }

    public Optional<PropertyDescriptorSpec> getProperty(String name) {
        return getRuleProperties().stream().filter(it -> it.getName().equals(name)).findFirst();
    }


    public LanguageVersion getMinimumVersion() {
        return minimumVersion.getValue();
    }


    public void setMinimumVersion(LanguageVersion minimumVersion) {
        this.minimumVersion.setValue(minimumVersion);
    }


    public Var<LanguageVersion> minimumVersionProperty() {
        return minimumVersion;
    }


    public LanguageVersion getMaximumVersion() {
        return maximumVersion.getValue();
    }


    public void setMaximumVersion(LanguageVersion maximumVersion) {
        this.maximumVersion.setValue(maximumVersion);
    }


    public Var<LanguageVersion> maximumVersionProperty() {
        return maximumVersion;
    }


    @PersistentProperty
    public String getSince() {
        return since.getValue();
    }


    public void setSince(String since) {
        this.since.setValue(since);
    }


    public Var<String> sinceProperty() {
        return since;
    }


    @PersistentProperty
    public String getMessage() {
        return message.getValue();
    }


    public void setMessage(String message) {
        this.message.setValue(message);
    }


    public Var<String> messageProperty() {
        return message;
    }


    @PersistentProperty
    public String getExternalInfoUrl() {
        return externalInfoUrl.getValue();
    }


    public void setExternalInfoUrl(String externalInfoUrl) {
        this.externalInfoUrl.setValue(externalInfoUrl);
    }


    public Var<String> externalInfoUrlProperty() {
        return externalInfoUrl;
    }


    @PersistentProperty
    public String getDescription() {
        return description.getValue();
    }


    public void setDescription(String description) {
        this.description.setValue(description);
    }


    public Var<String> descriptionProperty() {
        return description;
    }


    public ObservableList<String> getExamples() {
        return examples;
    }


    public void setExamples(ObservableList<String> examples) {
        this.examples.setAll(examples);
    }


    @PersistentProperty
    public RulePriority getPriority() {
        return priority.getValue();
    }


    public void setPriority(RulePriority priority) {
        this.priority.setValue(priority);
    }


    public Var<RulePriority> priorityProperty() {
        return priority;
    }


    public boolean isDeprecated() {
        return deprecated.getValue();
    }


    public void setDeprecated(boolean deprecated) {
        this.deprecated.setValue(deprecated);
    }

    public TestCollection getTestCollection() {
        return testCollection;
    }

    @Override
    public List<? extends SettingsOwner> getChildrenSettingsNodes() {
        return Collections.singletonList(testCollection);
    }

    public Var<Boolean> deprecatedProperty() {
        return deprecated;
    }


    public ObservableRuleBuilder deepCopy() {
        ObservableRuleBuilder copy = newBuilder();
        copy.setName(getName());
        copy.setDeprecated(isDeprecated());
        copy.setDescription(getDescription());
        copy.setMessage(getMessage());
        copy.setExternalInfoUrl(getExternalInfoUrl());
        copy.setClazz(getClazz());
        copy.setExamples(getExamples());
        copy.setSince(getSince());
        copy.setLanguage(getLanguage());
        copy.setMaximumVersion(getMaximumVersion());
        copy.setMinimumVersion(getMinimumVersion());
        copy.setPriority(getPriority());

        TestCollection coll = new TestCollection(copy, getTestCollection().getStash().stream().map(LiveTestCase::deepCopy).collect(Collectors.toList()));
        copy.getTestCollection().rebase(coll);
        copy.getRuleProperties().addAll(getRuleProperties().stream().map(PropertyDescriptorSpec::deepCopy).collect(Collectors.toList()));


        return copy;
    }

    protected ObservableRuleBuilder newBuilder() {
        return new ObservableRuleBuilder();
    }

}
