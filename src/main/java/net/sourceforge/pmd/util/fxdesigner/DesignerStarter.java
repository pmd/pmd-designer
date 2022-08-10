/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.swing.JOptionPane;

import org.apache.commons.lang3.SystemUtils;

import net.sourceforge.pmd.annotation.InternalApi;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
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

    private static final int MIN_JAVAFX_VERSION_ON_MAC_OSX = 14;

    private static final int ERROR_EXIT = 1;
    private static final int OK = 0;

    private DesignerStarter() {
    }

    private static boolean isJavaFxAvailable() {
        try {
            DesignerStarter.class.getClassLoader().loadClass("javafx.application.Application");
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    @Deprecated
    private static MainCliArgs readParameters(String[] argv) {

        MainCliArgs argsObj = new MainCliArgs();
        JCommander jCommander = new JCommander(argsObj);
        jCommander.setProgramName("designer");

        try {
            jCommander.parse(argv);

            if (argsObj.help) {
                System.out.println(getHelpText(jCommander));
                System.exit(OK);
            }

            return argsObj;

        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            System.out.println();
            System.out.println(getHelpText(jCommander));
            System.exit(OK);
            throw new AssertionError();
        }


    }

    /**
     * Starting from PMD 7.0.0 this method usage will be limited for development.
     * CLI support will be provided by pmd-cli
     */
    @Deprecated
    @InternalApi
    public static void main(String[] args) {

        readParameters(args);

        launchGui(args);
    }

    private static void setSystemProperties() {
        if (SystemUtils.IS_OS_LINUX) {
            // On Linux, JavaFX renders text poorly by default. These settings help to alleviate the problems.
            System.setProperty("prism.text", "t2k");
            System.setProperty("prism.lcdtext", "true");
        }
    }
    
    private static boolean isCompatibleJavaFxVersion() {
        if (SystemUtils.IS_OS_MAC_OSX) {
            final String javaFxVersion = getJavaFxVersion();
            if (javaFxVersion != null) {
                final int major = Integer.parseInt(javaFxVersion.split("\\.")[0]);
                if (major < MIN_JAVAFX_VERSION_ON_MAC_OSX) {
                    // Prior to JavaFx 14, text on Mac OSX was garbled and unreadable
                    return false;
                }
            }
        }

        return true;
    }

    private static String getJavaFxVersion() {
        try (InputStream is = DesignerStarter.class.getClassLoader().getResourceAsStream("javafx.properties")) {
            final Properties javaFxProperties = new Properties();
            javaFxProperties.load(is);
            return (String) javaFxProperties.get("javafx.version");
        } catch (IOException ignored) {
            // Can't determine the version
        }

        return null;
    }

    @Deprecated
    private static String getHelpText(JCommander jCommander) {

        StringBuilder sb = new StringBuilder();


        jCommander.usage(sb, " ");
        sb.append("\n");
        sb.append("\n");
        sb.append("PMD Rule Designer\n");
        sb.append("-----------------\n");
        sb.append("\n");
        sb.append("The Rule Designer is a graphical tool that helps PMD users develop their custom rules.\n");
        sb.append("\n");
        sb.append("\n");
        sb.append("Source & README: https://github.com/pmd/pmd-designer\n");
        sb.append("Usage documentation: https://pmd.github.io/latest/pmd_userdocs_extending_designer_reference.html");

        return sb.toString();
    }

    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public static int launchGui(String[] args) {
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
            return ERROR_EXIT;
        }

        try {
            Application.launch(Designer.class, args);
        } catch (Throwable unrecoverable) {
            unrecoverable.printStackTrace();
            return ERROR_EXIT;
        }

        return OK;
    }
}
