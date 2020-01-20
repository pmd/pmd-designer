/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import javax.swing.JOptionPane;

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


    private static MainCliArgs readParameters(String[] argv) {

        MainCliArgs argsObj = new MainCliArgs();

        try {
            JCommander jCommander = JCommander.newBuilder()
                                              .programName("designer")
                                              .addObject(argsObj)
                                              .build();
            jCommander.parse(argv);

            if (argsObj.help) {
                System.out.println(getHelpText(jCommander));
                System.exit(OK);
            }

            return argsObj;

        } catch (ParameterException e) {
            System.out.println(e.getMessage());
            System.out.println();
            System.out.println(getHelpText(e.getJCommander()));
            System.exit(OK);
            throw new AssertionError();
        }


    }


    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public static void main(String[] args) {

        readParameters(args);

        launchGui(args);
    }

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
    private static void launchGui(String[] args) {
        String message = null;
        if (!isJavaFxAvailable()) {
            message = MISSING_JAVAFX;
        }

        if (message != null) {
            System.err.println(message);
            JOptionPane.showMessageDialog(null, message);
            System.exit(ERROR_EXIT);
        }


        try {
            Application.launch(Designer.class, args);
        } catch (Throwable unrecoverable) {
            unrecoverable.printStackTrace();
            System.exit(ERROR_EXIT);
        }
    }
}
