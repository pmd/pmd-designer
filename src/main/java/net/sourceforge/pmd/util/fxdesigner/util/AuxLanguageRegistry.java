/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import static net.sourceforge.pmd.util.CollectionUtil.setOf;
import static net.sourceforge.pmd.util.CollectionUtil.union;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.PlainTextLanguage;

/**
 * Utilities to extend the functionality of {@link LanguageRegistry}.
 *
 * @author Clément Fournier
 */
public final class AuxLanguageRegistry {

    private static final String DEFAULT_LANGUAGE_NAME = "Java";
    public static final LanguageRegistry AUX_LANG_REGISTRY = new LanguageRegistry(union(LanguageRegistry.PMD.getLanguages(),
                                                                                        setOf(plainTextLanguage())));
    private static List<LanguageVersion> supportedLanguageVersions;
    private static Map<String, LanguageVersion> extensionsToLanguage;


    private AuxLanguageRegistry() {

    }


    public static LanguageRegistry supportedLangs() {
        return AUX_LANG_REGISTRY;
    }


    public static LanguageVersion findLanguageVersionByTerseName(String string) {
        String[] split = string.split(" ");
        Language lang = findLanguageByTerseName(split[0]);
        if (lang == null) {
            return null;
        }
        return split.length == 1 ? lang.getDefaultVersion()
                                 : lang.getVersion(split[1]);
    }

    public static @NonNull LanguageVersion defaultLanguageVersion() {
        return defaultLanguage().getDefaultVersion();
    }

    // TODO need a notion of dialect in core + language services
    public static boolean isXmlDialect(Language language) {
        switch (language.getId()) {
        case "xml":
        case "pom":
        case "wsql":
        case "fxml":
        case "xsl":
            return true;
        default:
            return false;
        }
    }

    public static PlainTextLanguage plainTextLanguage() {
        return PlainTextLanguage.getInstance();
    }

    public static @NonNull Language defaultLanguage() {
        Language defaultLanguage = findLanguageByName(DEFAULT_LANGUAGE_NAME);
        return defaultLanguage != null ? defaultLanguage : plainTextLanguage();
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

    public static synchronized @Nullable LanguageVersion getLanguageVersionFromExtension(String filename) {
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
        return !StringUtils.containsIgnoreCase(lv.getLanguage().getName(), "dummy");
    }

    public static synchronized List<LanguageVersion> getSupportedLanguageVersions() {
        if (supportedLanguageVersions == null) {
            supportedLanguageVersions = getSupportedLanguages().flatMap(it -> it.getVersions().stream())
                                                               .filter(AuxLanguageRegistry::filterLanguageVersion)
                                                               .collect(Collectors.toList());
        }
        return supportedLanguageVersions;
    }

    public static @NonNull LanguageVersion getLanguageVersionByName(String name) {
        return getSupportedLanguageVersions().stream()
                                             .filter(it -> it.getName().equals(name))
                                             .findFirst()
                                             .orElse(defaultLanguageVersion());
    }

    public static @NonNull Stream<Language> getSupportedLanguages() {
        return AUX_LANG_REGISTRY.getLanguages().stream();
    }

    public static @NonNull Language findLanguageByShortName(String shortName) {
        return getSupportedLanguages().filter(it -> it.getShortName().equals(shortName))
                                      .findFirst()
                                      .orElse(defaultLanguage());
    }

    public static @Nullable Language findLanguageByName(String n) {
        return AUX_LANG_REGISTRY.getLanguageByFullName(n);
    }

    public static @NonNull Language findLanguageByNameOrDefault(String n) {
        Language lang = findLanguageByName(n);
        return lang == null ? defaultLanguage() : lang;
    }

    public static @Nullable Language findLanguageByTerseName(String name) {
        return AUX_LANG_REGISTRY.getLanguageById(name);
    }

}
