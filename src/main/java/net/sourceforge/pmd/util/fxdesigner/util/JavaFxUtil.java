/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.apache.commons.lang3.SystemUtils;

import javafx.application.Application;

public final class JavaFxUtil {
    private static final int MIN_JAVAFX_VERSION_ON_MAC_OSX = 14;

    private JavaFxUtil() {
    }

    public static boolean isJavaFxAvailable() {
        try {
            JavaFxUtil.class.getClassLoader().loadClass("javafx.application.Application");
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    public static String getJavaFxVersion() {
        InputStream javafxProperties = JavaFxUtil.class.getClassLoader().getResourceAsStream("javafx.properties");

        if (javafxProperties == null) {
            try {
                Path path = Paths.get(Application.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                Path propertiesFile1 = path.resolveSibling("javafx.properties");
                Path propertiesFile2 = path.getParent().getParent().resolve("javafx.properties");
                if (Files.exists(propertiesFile1)) {
                    javafxProperties = Files.newInputStream(propertiesFile1);
                } else if (Files.exists(propertiesFile2)) {
                    javafxProperties = Files.newInputStream(propertiesFile2);
                }
            } catch (URISyntaxException | IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (javafxProperties != null) {
            try (InputStream is = javafxProperties) {
                final Properties javaFxProperties = new Properties();
                javaFxProperties.load(is);
                String javaFxVersion = javaFxProperties.getProperty("javafx.version");
                if (javaFxVersion != null) {
                    return javaFxVersion;
                }
                return javaFxProperties.getProperty("javafx.runtime.version");
            } catch (IOException ignored) {
                // Can't determine the version
                System.err.println("Can't determine javafx version: " + ignored);
            }
        } else {
            System.err.println("Can't determine javafx version: javafx.properties not found in classpath.");
        }
        return null;
    }

    public static void setSystemProperties() {
        if (SystemUtils.IS_OS_LINUX) {
            // On Linux, JavaFX renders text poorly by default. These settings help to alleviate the problems.
            System.setProperty("prism.text", "t2k");
            System.setProperty("prism.lcdtext", "true");
        }
    }


    public static boolean isCompatibleJavaFxVersion() {
        if (SystemUtils.IS_OS_MAC_OSX) {
            final String javaFxVersion = getJavaFxVersion();
            if (javaFxVersion != null) {
                final int major = Integer.parseInt(javaFxVersion.split("\\.")[0]);
                if (major < MIN_JAVAFX_VERSION_ON_MAC_OSX) {
                    // Prior to JavaFx 14, text on Mac OSX was garbled and unreadable
                    return false;
                }
            }
        }

        return true;
    }
}
