/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;


/**
 * Deals with resource fetching and the hardcore details of when we're in a Jar
 * vs when we're exploded in the IDE.
 */
public final class ResourceUtil {


    private static final String BASE_RESOURCE_PREFIX = "/net/sourceforge/pmd/util/fxdesigner/";
    private static final Object FILE_SYSTEM_LOCK = new Object();

    private ResourceUtil() {

    }

    /**
     * Prepends a resource path with the root resource path of the designer.
     * The given string path should not start with "/".
     */
    public static String resolveResource(String relativeToDesignerDir) {
        return BASE_RESOURCE_PREFIX + relativeToDesignerDir;
    }

    /** Finds the classes in the given package by looking in the classpath directories. */
    public static Stream<Class<?>> getClassesInPackage(String packageName) {
        return pathsInResource(Thread.currentThread().getContextClassLoader(), packageName.replace('.', '/'))
            .map((Function<Path, Class<?>>) p -> toClass(p, packageName))
            .filter(Objects::nonNull);
    }

    private static Stream<Path> pathsInResource(ClassLoader classLoader,
                                                String resourcePath) {
        Stream<URL> resources;

        try {
            resources = DesignerIteratorUtil.enumerationAsStream(classLoader.getResources(resourcePath));
        } catch (IOException e) {
            return Stream.empty();
        }

        if (resourcePath.isEmpty()) {
            resources = resources.flatMap(url -> {
                if (url.toString().matches(".*META-INF/versions/\\d+/?")) {
                    try {
                        return Stream.of(url, new URL(url, "../../.."));
                    } catch (MalformedURLException ignored) {

                    }
                }
                return Stream.of(url);
            });
        }

        return resources.distinct().flatMap(resource -> {
            try {
                return getPathsInDir(resource, 1).stream();
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
                return Stream.empty();
            }
        });
    }


    /** Maps paths to classes. */
    private static Class<?> toClass(Path path, String packageName) {
        return Optional.of(path)
                       .filter(p -> "class".equalsIgnoreCase(FilenameUtils.getExtension(path.toString())))
            .<Class<?>>map(p -> {
                try {
                    return Class.forName(packageName + "." + FilenameUtils.getBaseName(path.getFileName().toString()));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    return null;
                }
            })
            .orElse(null);
    }

    private static String getJarRelativePath(URI uri) {
        if ("jar".equals(uri.getScheme())) {
            // we have to cut out the path to the jar + '!'
            // to get a path that's relative to the root of the jar filesystem
            // This is equivalent to a packageName.replace('.', '/') but more reusable
            String schemeSpecific = uri.getSchemeSpecificPart();
            return schemeSpecific.substring(schemeSpecific.indexOf('!') + 1);
        } else {
            return uri.getSchemeSpecificPart();
        }
    }

    /**
     * Returns an absolute path to the code location, ie the jar in which
     * the app is bundled, or the directory in which the classes are laid
     * out.
     */
    public static Path thisJarPathInHost() {
        try {
            return Paths.get(ResourceUtil.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }


    private static List<Path> getPathsInDir(URL url, int maxDepth) throws URISyntaxException, IOException {

        URI uri = url.toURI().normalize();

        if ("jar".equals(uri.getScheme())) {
            // we have to do this to look inside a jar
            try (FileSystem fs = getFileSystem(uri)) {
                Path path = fs.getPath(getJarRelativePath(uri));
                while (maxDepth < 0) {
                    path = path.resolve("..");
                    maxDepth++;
                }

                try (Stream<Path> pathStream = Files.walk(path, maxDepth)) {
                    return pathStream.collect(Collectors.toList()); // buffer everything, before closing the filesystem
                }
            }
        } else {
            Path path = toPath(url);
            while (maxDepth < 0) {
                path = path.resolve("..");
                maxDepth++;
            }
            try (Stream<Path> paths = Files.walk(path, maxDepth)) {
                return paths.collect(Collectors.toList()); // buffer everything, before closing the original stream
            }
        }
    }

    private static Path toPath(URL url) {
        return new File(url.getFile()).toPath();
    }


    public static FileSystem getFileSystem(URI uri) throws IOException {

        synchronized (FILE_SYSTEM_LOCK) {
            try {
                return FileSystems.getFileSystem(uri);
            } catch (FileSystemNotFoundException e) {
                return FileSystems.newFileSystem(uri, Collections.<String, String>emptyMap());
            }
        }
    }
}
