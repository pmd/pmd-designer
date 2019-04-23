/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import javax.swing.JOptionPane;

import javafx.application.Application;

/**
 * Main class of the app, checking for prerequisites to launching {@link Designer}.
 */
public final class DesignerStarter {

    private static final String MISSING_JAVAFX = "You seem to be missing the JavaFX runtime. Please install JavaFX on your system and try again.";

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

    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public static void main(String[] args) {
        if (!isJavaFxAvailable()) {
            System.err.println(MISSING_JAVAFX);
            JOptionPane.showMessageDialog(null, MISSING_JAVAFX);
            System.exit(1);
        }



        try {
            Application.launch(Designer.class, args);
        } catch (Throwable unrecoverable) {
            unrecoverable.printStackTrace();
            System.exit(1);
        }
    }
}
