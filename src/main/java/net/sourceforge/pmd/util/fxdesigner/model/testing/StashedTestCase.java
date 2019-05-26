/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */


package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.testframework.TestDescriptor;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextPos2D;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextRange;

/**
 * Committed version of a test case.
 */
public class StashedTestCase {

    private final String source;
    private final String description;
    private final LanguageVersion languageVersion;
    private final Map<String, String> properties;
    private final List<ViolationRecord> expectedViolations;

    public StashedTestCase(@NonNull String source, @NonNull String description, @Nullable LanguageVersion languageVersion, Map<String, String> properties, @NonNull List<ViolationRecord> expectedViolations) {
        this.source = source;
        this.description = description;
        this.languageVersion = languageVersion;
        this.properties = new HashMap<>(properties);
        this.expectedViolations = new ArrayList<>(expectedViolations);
        expectedViolations.sort(Comparator.comparing(v -> v.getRange().startPos));
    }

    public TestDescriptor toDescriptor(Rule owner) {
        TestDescriptor test = new TestDescriptor(source, description, expectedViolations.size(), owner,
                                                 languageVersion == null
                                                 ? owner.getLanguage().getDefaultVersion()
                                                 : languageVersion);

        test.setExpectedLineNumbers(expectedViolations.stream().map(it -> it.getRange().startPos.line).collect(Collectors.toList()));
        test.setExpectedMessages(expectedViolations.stream().map(ViolationRecord::getMessage).collect(Collectors.toList()));

        return test;
    }

    public LiveTestCase unfreeze(Consumer<LiveTestCase> commitHandler) {
        LiveTestCase live = new LiveTestCase(commitHandler);
        live.setDescription(description);
        live.setLanguageVersion(languageVersion);
        live.setDirty(false);
        live.setSource(source);
        return live;
    }

    public String getSource() {
        return source;
    }

    public String getDescription() {
        return description;
    }

    @Nullable
    public LanguageVersion getLanguageVersion() {
        return languageVersion;
    }

    public List<ViolationRecord> getExpectedViolations() {
        return expectedViolations;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StashedTestCase that = (StashedTestCase) o;
        return Objects.equals(source, that.source) &&
            Objects.equals(description, that.description) &&
            Objects.equals(languageVersion, that.languageVersion) &&
            Objects.equals(expectedViolations, that.expectedViolations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, description, languageVersion, expectedViolations);
    }

    public static StashedTestCase fromDescriptor(TestDescriptor descriptor) {

        LiveTestCase live = new LiveTestCase(t -> {});
        live.setSource(descriptor.getCode());
        live.setDescription(descriptor.getDescription());

        List<String> lines = Arrays.asList(descriptor.getCode().split("\\Z"));

        for (int i = 0; i < descriptor.getNumberOfProblemsExpected(); i++) {
            String m = descriptor.getExpectedMessages().get(i);
            int line = descriptor.getExpectedLineNumbers().get(i);


            TextRange tr = new TextRange(new TextPos2D(line, 0), new TextPos2D(line, lines.get(line - 1).length()));

            live.getExpectedViolations().add(new ViolationRecord(tr, false, m).unfreeze());
        }

        descriptor.getProperties().forEach((k, v) -> live.getProperties().put(k.toString(), v.toString()));
        return live.freeze();
    }
}
