/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

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


    /**
     * Restore the persisted settings into the root settings owner.
     *
     * @param settingsOwner Root of the settings owner hierarchy
     */
    void restoreSettings(SettingsOwner settingsOwner);


    /**
     * Save the settings from the tree rooted at the [settingsOwner]
     * somewhere for the next runs.
     *
     * @param settingsOwner Root of the settings owner hierarchy
     */
    void persistSettings(SettingsOwner settingsOwner);

}
