package net.sourceforge.pmd.util.fxdesigner.app.services;

import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;

/**
 * Manages the persistence settings. Encapsulates output and input streams
 * to mock easily.
 *
 * @author Clément Fournier
 */
public interface PersistenceManager extends ApplicationComponent {


    void restoreSettings(SettingsOwner settingsOwner);


    void persistSettings(SettingsOwner settingsOwner);

}
