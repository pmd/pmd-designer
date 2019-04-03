/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.beans.testdata;

import java.util.Objects;

import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;

public class SomeBean implements SettingsOwner {

    private String str = "";
    private Class<?> k = SomeBean.class;
    private int i = 4;

    @PersistentProperty
    public String getStr() {
        return str;
    }

    public void setStr(String str) {
        this.str = str;
    }

    @PersistentProperty
    public Class<?> getK() {
        return k;
    }

    public void setK(Class<?> k) {
        this.k = k;
    }

    @PersistentProperty
    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SomeBean someBean = (SomeBean) o;
        return i == someBean.i
            && Objects.equals(str, someBean.str)
            && Objects.equals(k, someBean.k);
    }

    @Override
    public int hashCode() {
        return Objects.hash(str, k, i);
    }

    @Override
    public String toString() {
        return "SomeBean{str='" + str + '\'' + ", k=" + k + ", i=" + i + '}';
    }
}
