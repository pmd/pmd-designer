/**
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


    private final DesignerRoot root;
    private final Path settingsDirectory;

    public GlobalDiskManagerImpl(DesignerRoot root, Path settingsDirectory) {
        this.root = root;
        this.settingsDirectory = settingsDirectory;


        Path curVersionStamp = settingsDirectory.resolve("version-" + Designer.getCurrentVersion());


        List<Path> diskVersionStamps = getDiskVersionStamps();
        //        if (diskVersionStamps.stream().anyMatch(p -> p.getFileName().equals(curVersionStamp.getFileName()))) {
        //            // up2date
        //        } else {
        //            // TODO you can now do something if we detected another version
        //        }
        diskVersionStamps.forEach(path -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


        try {
            Files.createDirectories(settingsDirectory);
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
                        .filter(it -> it.getFileName().startsWith("version-"))
                        .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    public Path defaultAppStateFile() {
        return settingsDirectory.resolve("appstate.xml");
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
