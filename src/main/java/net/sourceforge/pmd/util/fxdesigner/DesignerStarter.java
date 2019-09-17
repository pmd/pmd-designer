/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import javax.swing.JOptionPane;

import net.sourceforge.pmd.util.fxdesigner.util.LanguageRegistryUtil;

import javafx.application.Application;

/**
 * Main class of the app, checking for prerequisites to launching {@link Designer}.
 */
public final class DesignerStarter {

    private static final String MISSING_JAVAFX = "You seem to be missing the JavaFX runtime. Please install JavaFX on your system and try again.";
    private static final String MISSING_LANGUAGE_MODULES = "No PMD language modules can be found. Please add some to your classpath and restart the app.";

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

    private static boolean areLanguageModulesAvailable() {
        return LanguageRegistryUtil.defaultLanguageVersion() != null;
    }

    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public static void main(String[] args) {

        String message = null;
        if (!isJavaFxAvailable()) {
            message = MISSING_JAVAFX;
        } else if (!areLanguageModulesAvailable()) {
            message = MISSING_LANGUAGE_MODULES;
        }

        if (message != null) {
            System.err.println(message);
            JOptionPane.showMessageDialog(null, message);
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
