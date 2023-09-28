/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import static net.sourceforge.pmd.util.fxdesigner.util.JavaFxUtil.isCompatibleJavaFxVersion;
import static net.sourceforge.pmd.util.fxdesigner.util.JavaFxUtil.isJavaFxAvailable;
import static net.sourceforge.pmd.util.fxdesigner.util.JavaFxUtil.setSystemProperties;

import javax.swing.JOptionPane;

import net.sourceforge.pmd.annotation.InternalApi;

import javafx.application.Application;

/**
 * Main class of the app, checking for prerequisites to launching {@link Designer}.
 */
public final class DesignerStarter {

    private static final String MISSING_JAVAFX =
        "You seem to be missing the JavaFX runtime." + System.lineSeparator()
            + " Please install JavaFX on your system and try again." + System.lineSeparator()
            + " See https://gluonhq.com/products/javafx/";

    private static final String INCOMPATIBLE_JAVAFX =
        "You seem to be running an older version of JavaFX runtime." + System.lineSeparator()
            + " Please install the latest JavaFX on your system and try again." + System.lineSeparator()
            + " See https://gluonhq.com/products/javafx/";

    private DesignerStarter() {
    }

    /**
     * Starting from PMD 7.0.0 this method usage will be limited for development.
     * CLI support will be provided by pmd-cli
     */
    @InternalApi
    public static void main(String[] args) {
        final ExitStatus ret = launchGui(args);
        System.exit(ret.getCode());
    }

    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public static ExitStatus launchGui(String[] args) {
        setSystemProperties();

        String message = null;
        if (!isJavaFxAvailable()) {
            message = MISSING_JAVAFX;
        } else if (!isCompatibleJavaFxVersion()) {
            message = INCOMPATIBLE_JAVAFX;
        }

        if (message != null) {
            System.err.println(message);
            JOptionPane.showMessageDialog(null, message);
            return ExitStatus.ERROR;
        }

        try {
            Application.launch(Designer.class, args);
        } catch (Throwable unrecoverable) {
            unrecoverable.printStackTrace();
            return ExitStatus.ERROR;
        }

        return ExitStatus.OK;
    }

    public enum ExitStatus {
        OK(0),
        ERROR(1);

        private final int code;

        ExitStatus(final int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }
}
