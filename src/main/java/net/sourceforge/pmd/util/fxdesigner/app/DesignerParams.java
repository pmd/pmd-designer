/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.sun.javafx.application.ParametersImpl;
import javafx.application.Application.Parameters;


/**
 * Parses the parameters of the app.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public final class DesignerParams {

    private static final Path PMD_SETTINGS_DIR = Paths.get(System.getProperty("user.home"), ".pmd");
    private static final Path THIS_DESIGNER_SETTINGS_DIR = PMD_SETTINGS_DIR.resolve("rule-designer");

    private static final String SETTINGS_INPUT = "load-from";
    private static final String SETTINGS_OUTPUT = "persist-to";


    private boolean isDeveloperMode;
    private Path persistedInputFile;
    private Path persistedOutputFile;


    public DesignerParams(String... args) {
        this(new ParametersImpl(args));
    }

    /**
     * Build from JavaFX parameters.
     */
    public DesignerParams(Parameters params) {
        List<String> raw = params.getRaw();
        // error output is disabled by default
        if (raw.contains("-v") || raw.contains("--verbose") || raw.contains("--debug") || raw.contains("-D")) {
            isDeveloperMode = true;
        }

        params.getNamed().forEach(
            (name, value) -> {
                switch (name) {
                case SETTINGS_INPUT:
                    persistedInputFile = Paths.get(value);
                    break;
                case SETTINGS_OUTPUT:
                    persistedOutputFile = Paths.get(value);
                    break;
                default:
                    break;
                }
            }
        );

    }

    public Path getSettingsDirectory() {
        return THIS_DESIGNER_SETTINGS_DIR;
    }

    void processDefaults(Path defaultAppStateFile) {
        if (persistedInputFile == null && persistedOutputFile == null) {
            persistedInputFile = defaultAppStateFile;
            persistedOutputFile = defaultAppStateFile;
        }
    }

    public boolean isDeveloperMode() {
        return isDeveloperMode;
    }

    public Path getPersistedInputFile() {
        return persistedInputFile;
    }

    public Path getPersistedOutputFile() {
        return persistedOutputFile;
    }
}
