/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model;

import org.reactfx.EventStream;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.rule.xpath.XPathRule;
import net.sourceforge.pmd.lang.rule.xpath.XPathVersion;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ObservableTickList;


/**
 * Specialises rule builders for XPath rules.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public class ObservableXPathRuleBuilder extends ObservableRuleBuilder {


    private final Var<XPathVersion> xpathVersion = Var.newSimpleVar(XPathVersion.DEFAULT);
    private final Var<String> xpathExpression = Var.newSimpleVar("");


    public ObservableXPathRuleBuilder() {
        setClazz(XPathRule.class);
        setLanguage(null); // this will be set by the constructor of the controller
    }

    public XPathVersion getXpathVersion() {
        return xpathVersion.getValue();
    }


    public void setXpathVersion(XPathVersion xpathVersion) {
        this.xpathVersion.setValue(xpathVersion);
    }


    @PersistentProperty
    public String getPersistedXpathVersion() {
        return xpathVersion.getValue().getXmlName();
    }


    public void setPersistedXpathVersion(String xpathVersion) {
        this.xpathVersion.setValue(XPathVersion.ofId(xpathVersion));
    }


    public Var<XPathVersion> xpathVersionProperty() {
        return xpathVersion;
    }


    @PersistentProperty
    public String getXpathExpression() {
        return xpathExpression.getValue();
    }


    public void setXpathExpression(String value) {
        xpathExpression.setValue(value);
    }


    public Var<String> xpathExpressionProperty() {
        return xpathExpression;
    }


    @Override
    protected ObservableRuleBuilder newBuilder() {
        return new ObservableXPathRuleBuilder();
    }

    @Override
    public ObservableXPathRuleBuilder deepCopy() {
        ObservableXPathRuleBuilder copy = (ObservableXPathRuleBuilder) super.deepCopy();
        copy.setXpathExpression(getXpathExpression());
        copy.setXpathVersion(getXpathVersion());
        return copy;
    }

    /**
     * Pushes an event every time the rule needs to be re-evaluated.
     */
    public EventStream<?> modificationsTicks() {
        return languageProperty().values()
                                 .or(xpathVersion.values())
                                 .or(xpathExpression.values())
                                 .or(rulePropertiesProperty().values().flatMap(lst -> new ObservableTickList<>(lst, PropertyDescriptorSpec::modificationTicks).quasiChanges()));
    }

    // TODO: Once the xpath expression changes, we'll need to rebuild the rule
    //    @Override
    //    public Optional<Rule> build() throws IllegalArgumentException {
    //        return super.build();
    //    }
}
