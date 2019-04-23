/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.popups;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import net.sourceforge.pmd.PMDVersion;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.util.fxdesigner.Designer;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.util.LanguageRegistryUtil;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;


/**
 * @author Clément Fournier
 */
public final class SimplePopups {

    private static final String LICENSE_FILE_PATH = "/net/sourceforge/pmd/util/fxdesigner/LICENSE";


    private SimplePopups() {

    }


    public static void showLicensePopup() {
        Alert licenseAlert = new Alert(AlertType.INFORMATION);
        licenseAlert.setWidth(500);
        licenseAlert.setHeaderText("License");

        ScrollPane scroll = new ScrollPane();
        try {
            scroll.setContent(new TextArea(IOUtils.toString(SimplePopups.class.getResourceAsStream(LICENSE_FILE_PATH), StandardCharsets.UTF_8)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        licenseAlert.getDialogPane().setContent(scroll);
        licenseAlert.showAndWait();
    }

    public static void showAboutPopup(DesignerRoot root) {
        Alert licenseAlert = new Alert(AlertType.INFORMATION);
        licenseAlert.setWidth(500);
        licenseAlert.setHeaderText("About");

        ScrollPane scroll = new ScrollPane();
        TextArea textArea = new TextArea();

        String sb = "PMD core version: " + PMDVersion.VERSION + "\n"
            + "Available languages: "
            + LanguageRegistryUtil.getSupportedLanguages().map(Language::getTerseName).collect(Collectors.toList())
            + "\n"
            + "Designer version: " + Designer.VERSION + "\n"
            + "Designer settings dir: " + root.getService(DesignerRoot.DISK_MANAGER).getSettingsDirectory()
            + "\n";

        textArea.setText(sb);
        scroll.setContent(textArea);

        licenseAlert.getDialogPane().setContent(scroll);
        licenseAlert.showAndWait();
    }

}
