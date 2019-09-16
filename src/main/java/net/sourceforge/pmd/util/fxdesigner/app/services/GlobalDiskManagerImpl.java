/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.sourceforge.pmd.util.fxdesigner.Designer;
import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;

/**
 * Manages the root disk resource directory for the current user.
 *
 * @author Clément Fournier
 */
public class GlobalDiskManagerImpl implements GlobalDiskManager, ApplicationComponent {


    public static final String APPSTATE_NAME = "appstate.xml";
    private static final String STAMP_PREFIX = "version-";
    private final DesignerRoot root;
    private final Path settingsDirectory;

    public GlobalDiskManagerImpl(DesignerRoot root, Path settingsDirectory) {
        this.root = root;
        this.settingsDirectory = settingsDirectory;


        Path curVersionStamp = settingsDirectory.resolve("version-" + Designer.getCurrentVersion());


        List<Path> diskVersionStamps = getDiskVersionStamps();
        //        if (diskVersionStamps.stream().noneMatch(curVersionStamp::equals)) {
        //            // TODO you can now do something if we detected another version
        //        }

        try {
            Files.createDirectories(settingsDirectory);

            for (Path stamp : diskVersionStamps) {
                Files.deleteIfExists(stamp);
            }

            if (!Files.exists(curVersionStamp)) {
                Files.createFile(curVersionStamp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Path> getDiskVersionStamps() {
        try {
            return Files.list(settingsDirectory)
                        .filter(it -> !Files.isDirectory(it))
                        .filter(it -> it.getFileName().toString().startsWith(STAMP_PREFIX))
                        .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public Path defaultAppStateFile() {
        return settingsDirectory.resolve(APPSTATE_NAME);
    }

    @Override
    public Path getSettingsDirectory() {
        return settingsDirectory;
    }


    @Override
    public DesignerRoot getDesignerRoot() {
        return root;
    }
}
