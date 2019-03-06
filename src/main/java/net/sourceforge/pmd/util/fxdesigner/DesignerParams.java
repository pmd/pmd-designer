/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import java.io.File;
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
final class DesignerParams {

    private static final Path PMD_SETTINGS_DIR = Paths.get(System.getProperty("user.home"), ".pmd");
    private static final File DEFAULT_SETTINGS_FILE = PMD_SETTINGS_DIR.resolve("designer.xml").toFile();


    private static final String SETTINGS_INPUT = "load-from";
    private static final String SETTINGS_OUTPUT = "persist-to";


    private boolean isDeveloperMode;
    private File persistedInputFile;
    private File persistedOutputFile;


    public DesignerParams(String... args) {
        this(new ParametersImpl(args));
    }

    /**
     * Build from JavaFX parameters.
     */
    public DesignerParams(Parameters params) {
        List<String> raw = params.getRaw();
        // error output is disabled by default
        if (raw.contains("-v") || raw.contains("--verbose")) {
            isDeveloperMode = true;
        }

        params.getNamed().forEach(
            (name, value) -> {
                switch (name) {
                case SETTINGS_INPUT:
                    persistedInputFile = new File(value);
                    break;
                case SETTINGS_OUTPUT:
                    persistedOutputFile = new File(value);
                    break;
                default:
                    break;
                }
            }
        );

        processDefaults();
    }


    private void processDefaults() {
        if (persistedInputFile == null && persistedOutputFile == null) {
            persistedInputFile = DEFAULT_SETTINGS_FILE;
            persistedOutputFile = DEFAULT_SETTINGS_FILE;
        }
    }

    public boolean isDeveloperMode() {
        return isDeveloperMode;
    }

    public File getPersistedInputFile() {
        return persistedInputFile;
    }

    public File getPersistedOutputFile() {
        return persistedOutputFile;
    }
}
