/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.Strings;

import net.sourceforge.pmd.annotation.InternalApi;
import net.sourceforge.pmd.util.fxdesigner.util.IncompatibleJavaFxVersion;
import net.sourceforge.pmd.util.fxdesigner.util.JavaFxUtil;

import javafx.application.Application;

/**
 * Main class of the app, checking for prerequisites to launching {@link Designer}.
 */
public final class DesignerStarter {

    private static final String MISSING_JAVAFX =
            "You seem to be missing the JavaFX runtime." + System.lineSeparator()
                    + "Please install JavaFX on your system and try again." + System.lineSeparator()
                    + "See https://gluonhq.com/products/javafx/" + System.lineSeparator()
                    + "and https://docs.pmd-code.org/latest/pmd_userdocs_extending_designer_reference.html#installing-running-updating" + System.lineSeparator();

    private static final String INCOMPATIBLE_JAVAFX =
            "You seem to be running an incompatible version of JavaFX runtime." + System.lineSeparator()
                    + "Please install a compatible JavaFX version on your system and try again." + System.lineSeparator()
                    + "See https://gluonhq.com/products/javafx/" + System.lineSeparator();

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

    public static ExitStatus launchGui(String[] args) {
        JavaFxUtil.setSystemProperties();

        try {
            Application.launch(Designer.class, args);
        } catch (RuntimeException unrecoverable) {
            if (isIncompatibleJavaFxVersion(unrecoverable)) {
                displayError(INCOMPATIBLE_JAVAFX + unrecoverable.getCause().getMessage());
            } else {
                // only print stacktrace for unknown errors
                unrecoverable.printStackTrace();
            }
            return ExitStatus.ERROR;
        } catch (NoClassDefFoundError classNotFound) {
            if (isJavaFxUnavailable(classNotFound)) {
                displayError(MISSING_JAVAFX);
                System.err.println(classNotFound);
            } else {
                // only print stacktrace for unknown errors
                classNotFound.printStackTrace();
            }
            return ExitStatus.ERROR;
        }

        return ExitStatus.OK;
    }

    private static boolean isJavaFxUnavailable(NoClassDefFoundError classNotFound) {
        return classNotFound.getCause() instanceof ClassNotFoundException
                && Strings.CI.startsWith(classNotFound.getCause().getMessage(), "javafx");
    }

    private static boolean isIncompatibleJavaFxVersion(RuntimeException exception) {
        return exception.getCause() instanceof IncompatibleJavaFxVersion
                || (exception.getCause() != null
                && exception.getCause().getCause() instanceof IncompatibleJavaFxVersion);
    }

    private static void displayError(String message) {
        System.err.println(message);
        JOptionPane.showMessageDialog(null, message);
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
