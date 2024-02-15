/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

import net.sourceforge.pmd.PMDVersion;
import net.sourceforge.pmd.util.fxdesigner.util.ResourceUtil;

/**
 * Stores the current Designer and PMD version and provides utility methods around them.
 */
public final class DesignerVersion {

    /**
     * Constant that contains always the current version of the designer.
     */
    private static final String VERSION;
    private static final String PMD_CORE_MIN_VERSION;
    private static final String UNKNOWN_VERSION = "unknown";

    /**
     * Determines the version from maven's generated pom.properties file.
     */
    static {
        VERSION = readProperty("/META-INF/maven/net.sourceforge.pmd/pmd-designer/pom.properties", "version").orElse(UNKNOWN_VERSION);
        PMD_CORE_MIN_VERSION = readProperty(ResourceUtil.resolveResource("designer.properties"), "pmd.core.version").orElse(UNKNOWN_VERSION);
    }

    private DesignerVersion() {
        throw new AssertionError("Can't instantiate a utility class.");
    }

    public static String getCurrentVersion() {
        return VERSION;
    }

    public static String getPmdCoreMinVersion() {
        return PMD_CORE_MIN_VERSION;
    }

    private static Optional<String> readProperty(String resourcePath, String key) {
        try (InputStream stream = PMDVersion.class.getResourceAsStream(resourcePath)) {
            if (stream != null) {
                final Properties properties = new Properties();
                properties.load(stream);
                return Optional.ofNullable(properties.getProperty(key));
            }
        } catch (final IOException ignored) {
            // fallthrough
        }
        return Optional.empty();
    }
}
