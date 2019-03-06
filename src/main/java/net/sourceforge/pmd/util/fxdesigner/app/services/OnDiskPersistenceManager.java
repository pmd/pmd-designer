package net.sourceforge.pmd.util.fxdesigner.app.services;

import java.io.File;
import java.io.IOException;

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
    private final File input;
    private final File output;

    public OnDiskPersistenceManager(DesignerRoot root, File input, File output) {
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
        if (input == null || !input.isFile()) {
            return;
        }
        try {
            SettingsPersistenceUtil.restoreProperties(settingsOwner, input);
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
            SettingsPersistenceUtil.persistProperties(settingsOwner, output);
        } catch (IOException ioe) {
            // nevermind
            ioe.printStackTrace();
        }
    }
}
