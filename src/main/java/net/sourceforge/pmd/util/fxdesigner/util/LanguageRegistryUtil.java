/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import static net.sourceforge.pmd.lang.LanguageRegistry.findAllVersions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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

    private static final String DEFAULT_LANGUAGE_NAME = "Java";
    private static List<LanguageVersion> supportedLanguageVersions;
    private static Map<String, LanguageVersion> extensionsToLanguage;

    private LanguageRegistryUtil() {

    }

    @NonNull
    public static LanguageVersion defaultLanguageVersion() {
        return defaultLanguage().getDefaultVersion();
    }

    @NonNull
    public static Language defaultLanguage() {
        Language defaultLanguage = LanguageRegistry.getLanguage(DEFAULT_LANGUAGE_NAME);
        if (defaultLanguage != null) {
            return defaultLanguage;
        } else {
            Language fallback = LanguageRegistry.findLanguageByTerseName(PlainTextLanguage.TERSE_NAME);
            if (fallback != null) {
                return fallback;
            } else {
                throw new AssertionError("No registered languages, expecting at least the plain text language");
            }
        }
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

    @Nullable
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

    private static boolean filterLanguageVersion(LanguageVersion lv) {
        return !StringUtils.containsIgnoreCase(lv.getLanguage().getName(), "dummy")
            && Optional.ofNullable(lv.getLanguageVersionHandler())
                       .map(handler -> handler.getParser(handler.getDefaultParserOptions()))
                       .filter(Parser::canParse)
                       .isPresent();
    }

    public static synchronized List<LanguageVersion> getSupportedLanguageVersions() {
        if (supportedLanguageVersions == null) {
            supportedLanguageVersions = findAllVersions().stream().filter(LanguageRegistryUtil::filterLanguageVersion).collect(Collectors.toList());
        }
        return supportedLanguageVersions;
    }

    @NonNull
    public static LanguageVersion getLanguageVersionByName(String name) {
        return getSupportedLanguageVersions().stream()
                                             .filter(it -> it.getName().equals(name))
                                             .findFirst()
                                             .orElse(defaultLanguageVersion());
    }

    @NonNull
    public static Stream<Language> getSupportedLanguages() {
        return getSupportedLanguageVersions().stream().map(LanguageVersion::getLanguage).distinct();
    }

    @NonNull
    public static Language findLanguageByShortName(String shortName) {
        return getSupportedLanguages().filter(it -> it.getShortName().equals(shortName))
                                      .findFirst()
                                      .orElse(defaultLanguage());
    }

    @NonNull
    public static Language findLanguageByName(String n) {
        return getSupportedLanguages().filter(it -> it.getName().equals(n))
                                      .findFirst()
                                      .orElse(defaultLanguage());
    }
}
