/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import org.apache.commons.lang3.SystemUtils;

public final class JavaFxUtil {
    private static final int MIN_JAVAFX_VERSION_ON_MAC_OSX = 14;

    private JavaFxUtil() {
    }

    public static void setSystemProperties() {
        if (SystemUtils.IS_OS_LINUX) {
            // On Linux, JavaFX renders text poorly by default. These settings help to alleviate the problems.
            System.setProperty("prism.text", "t2k");
            System.setProperty("prism.lcdtext", "true");
        }
    }

    /*
     * Must be called after JavaFX has been initialized, that means
     * after Application.launch. Otherwise, the javafx.version property is
     * not available.
     */
    public static void isCompatibleJavaFxVersion() throws IncompatibleJavaFxVersion {
        final String javaFxVersion = System.getProperty("javafx.version");
        if (javaFxVersion == null) {
            throw new IllegalStateException("JavaFX is not initialized yet. No javafx.version known.");
        }

        final int major = Integer.parseInt(javaFxVersion.split("\\.")[0]);

        if (SystemUtils.IS_OS_MAC_OSX) {
            if (major < MIN_JAVAFX_VERSION_ON_MAC_OSX) {
                // Prior to JavaFX 14, text on Mac OSX was garbled and unreadable
                throw new IncompatibleJavaFxVersion("On MacOS JavaFX >= 15 is required. You are currently using " + javaFxVersion);
            }
        }

        if (major >= 25) {
            throw new IncompatibleJavaFxVersion("JavaFX >= 25 is not supported. You are currently using " + javaFxVersion);
        }
    }
}
