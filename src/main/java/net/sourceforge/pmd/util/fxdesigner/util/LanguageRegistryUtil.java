/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.Parser;

/**
 * Utilities to extend the functionality of {@link LanguageRegistry}.
 *
 * @author Clément Fournier
 */
public final class LanguageRegistryUtil {

    private static List<LanguageVersion> supportedLanguageVersions;
    private static Map<String, LanguageVersion> extensionsToLanguage;

    private LanguageRegistryUtil() {

    }

    public static LanguageVersion defaultLanguageVersion() {
        Language defaultLanguage = LanguageRegistry.getDefaultLanguage();
        return defaultLanguage == null ? null : defaultLanguage.getDefaultVersion();
    }

    private static Map<String, LanguageVersion> getExtensionsToLanguageMap() {
        Map<String, LanguageVersion> result = new HashMap<>();
        getSupportedLanguageVersions().stream()
                                      .map(LanguageVersion::getLanguage)
                                      .distinct()
                                      .collect(Collectors.toMap(Language::getExtensions,
                                                                Language::getDefaultVersion))
                                      .forEach((key, value) -> key.forEach(ext -> result.put(ext, value)));
        return result;
    }

    public static synchronized LanguageVersion getLanguageVersionFromExtension(String filename) {
        if (extensionsToLanguage == null) {
            extensionsToLanguage = getExtensionsToLanguageMap();
        }

        if (filename.indexOf('.') > 0) {
            String[] tokens = filename.split("\\.");
            return extensionsToLanguage.get(tokens[tokens.length - 1]);
        }
        return null;
    }

    public static synchronized List<LanguageVersion> getSupportedLanguageVersions() {
        if (supportedLanguageVersions == null) {
            List<LanguageVersion> languageVersions = new ArrayList<>();
            for (LanguageVersion languageVersion : LanguageRegistry.findAllVersions()) {
                Optional.ofNullable(languageVersion.getLanguageVersionHandler())
                        .map(handler -> handler.getParser(handler.getDefaultParserOptions()))
                        .filter(Parser::canParse)
                        .ifPresent(p -> languageVersions.add(languageVersion));
            }
            supportedLanguageVersions = languageVersions;
        }
        return supportedLanguageVersions;
    }
}
