/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.popups;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ClasspathEntry {
    private final String entry;

    public static List<ClasspathEntry> fromFiles(List<File> files) {
        return files.stream()
                .map(File::toString)
                .map(ClasspathEntry::new)
                .collect(Collectors.toList());
    }

    public static List<ClasspathEntry> fromFiles(File... files) {
        return fromFiles(Arrays.asList(files));
    }

    public ClasspathEntry(String entry) {
        this.entry = entry;
    }

    public String getEntry() {
        return entry;
    }

    public String getDisplay() {
        return toPath().getFileName().toString();
    }

    public String getTooltip() {
        return toPath().toAbsolutePath().toString();
    }

    private Path toPath() {
        return Paths.get(entry);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClasspathEntry that = (ClasspathEntry) o;
        return Objects.equals(entry, that.entry);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(entry);
    }

    @Override
    public String toString() {
        return "ClasspathEntry{"
                + "entry='" + entry + '\''
                + '}';
    }
}
