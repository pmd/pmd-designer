/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum TestStatus {
    PASS("fas-check-circle"),
    FAIL("fas-times-circle"),
    ERROR("fas-exclamation-circle"),
    UNKNOWN("fas-circle");

    public static final String STATUS_CLASS = "test-status";

    private final String icon;

    TestStatus(String icon) {
        this.icon = icon;
    }

    public List<String> getStyleClass() {
        return Arrays.asList(STATUS_CLASS, "status-" + name().toLowerCase(Locale.ROOT));
    }

    public String getIcon() {
        return icon;
    }
}
