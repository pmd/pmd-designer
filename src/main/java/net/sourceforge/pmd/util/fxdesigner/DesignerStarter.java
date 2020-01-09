/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import static net.sourceforge.pmd.util.fxdesigner.MainCliArgs.HL_AST_DUMP;
import static net.sourceforge.pmd.util.fxdesigner.util.AuxLanguageRegistry.findLanguageByTerseName;
import static net.sourceforge.pmd.util.fxdesigner.util.AuxLanguageRegistry.getSupportedLanguages;

import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Scanner;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageVersionHandler;
import net.sourceforge.pmd.lang.Parser;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.ParseException;
import net.sourceforge.pmd.lang.ast.TokenMgrError;
import net.sourceforge.pmd.util.fxdesigner.util.XmlDumpUtil;

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

        MainCliArgs cliArgs = readParameters(args);

        if (cliArgs.dumpXml != null) {
            doHeadlessRun(cliArgs);
        }


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
        sb.append("Unless the ").append(HL_AST_DUMP).append(" option is specified, this program launches a JavaFX application.");
        sb.append("\n");
        sb.append("\n");
        sb.append("Source & README: https://github.com/pmd/pmd-designer\n");
        sb.append("Usage documentation: https://pmd.github.io/latest/pmd_userdocs_extending_designer_reference.html");

        return sb.toString();
    }

    private static void doHeadlessRun(MainCliArgs cliArgs) {
        // headless run
        Language lang = findLanguageByTerseName(cliArgs.dumpXml);

        if (lang == null) {
            System.err.println("Unrecognised language '" + cliArgs.dumpXml + "', available languages: "
                                   + getSupportedLanguages().map(Language::getTerseName).collect(Collectors.joining(", ")));
            System.exit(ERROR_EXIT);
        }

        LanguageVersionHandler lvh = lang.getDefaultVersion().getLanguageVersionHandler();
        Parser parser = lvh.getParser(lvh.getDefaultParserOptions());

        System.err.println("Will perform AST dump for language " + lang.getName());
        System.err.println("Reading from standard input...");

        Scanner scanner = new Scanner(System.in);


        StringBuilder builder = new StringBuilder();
        while (scanner.hasNextLine()) {
            builder.append(scanner.nextLine()).append(System.lineSeparator());
        }

        Node root;
        try {
            root = parser.parse("STDIN", new StringReader(builder.toString()));
        } catch (ParseException | TokenMgrError e) {
            System.err.println(e.getMessage());
            System.exit(ERROR_EXIT);
            throw new AssertionError();
        }

        try {
            XmlDumpUtil.appendXml(new PrintWriter(System.out), root);
        } catch (TransformerException | ParserConfigurationException e) {
            System.err.println(e.getMessage());
            System.exit(ERROR_EXIT);
        }

        System.exit(OK);
    }

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
