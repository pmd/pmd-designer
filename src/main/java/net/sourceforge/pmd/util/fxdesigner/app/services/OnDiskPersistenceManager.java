/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import net.sourceforge.pmd.util.fxdesigner.DesignerVersion;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil;

/**
 * Default persistence manager.
 *
 * @author Clément Fournier
 */
public class OnDiskPersistenceManager implements PersistenceManager {

    private final DesignerRoot root;
    private final Path input;
    private final Path output;

    public OnDiskPersistenceManager(DesignerRoot root, Path input, Path output) {
        this.root = root;
        this.input = input;
        this.output = output;
    }


    @Override
    public DesignerRoot getDesignerRoot() {
        return root;
    }

    @Override
    public void restoreSettings(SettingsOwner settingsOwner) {
        CompletableFuture<Path> extraction = null;
        if (input == null || !Files.isRegularFile(input) || !Files.exists(input)) {
            // TODO this should be kept around
            //            Path settingsDirectory = getService(DesignerRoot.DISK_MANAGER).getSettingsDirectory();
            //            ResourceManager manager = new ResourceManager(getDesignerRoot(), settingsDirectory);
            //            extraction = manager.extractResource("placeholders/appstate.xml", "appstate.xml");
        }

        try {
            Path realInput = extraction != null ? extraction.get() : input;

            SettingsPersistenceUtil.restoreProperties(settingsOwner, realInput.toFile());
        } catch (Exception e) {
            // shouldn't prevent the app from opening
            // in case the file is corrupted, it will be overwritten on shutdown
            logInternalException(e);
        }
    }

    @Override
    public void persistSettings(SettingsOwner settingsOwner) {
        if (output == null) {
            return;
        }

        try {
            SettingsPersistenceUtil.persistProperties(settingsOwner, output.toFile());
            commitAppState();
        } catch (Exception e) {
            // nevermind
            e.printStackTrace();
        }
    }


    private void commitAppState() throws IOException, InterruptedException {

        ProcessBuilder process = new ProcessBuilder();
        process.directory(getService(DesignerRoot.DISK_MANAGER).getSettingsDirectory().toFile());

        // if there's no git on the path then this probably fails
        // doesn't matter though
        process.command("git", "init");
        process.start().waitFor();
        process.command("git", "add", output.toString());
        process.start().waitFor();
        process.command("git", "commit", "-m", "\"On version " + DesignerVersion.getCurrentVersion() + "\"");
        process.start().waitFor();

    }
}
