/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javafx.scene.paint.Color;

public enum TestStatus {
    PASS("fas-check-circle", Color.GREEN),
    FAIL("fas-times-circle", Color.ORANGE),
    ERROR("fas-exclamation-circle", Color.DARKRED),
    UNKNOWN("fas-circle", Color.GRAY);

    public static final String STATUS_CLASS = "test-status";

    private final String icon;
    private final Color color;

    TestStatus(String icon, Color color) {
        this.icon = icon;
        this.color = color;
    }


    public Color getColor() {
        return color;
    }

    public List<String> getStyleClass() {
        return Arrays.asList(STATUS_CLASS, "status-" + name().toLowerCase(Locale.ROOT));
    }

    public String getIcon() {
        return icon;
    }
}
