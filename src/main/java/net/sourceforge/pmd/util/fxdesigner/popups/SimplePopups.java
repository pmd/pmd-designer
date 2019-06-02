/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.popups;

import static java.lang.Double.max;
import static java.lang.Math.min;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;

import net.sourceforge.pmd.PMDVersion;
import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.util.fxdesigner.Designer;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.LanguageRegistryUtil;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.Transition;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.util.Duration;


/**
 * @author Clément Fournier
 */
public final class SimplePopups {

    private static final String LICENSE_FILE_PATH = "/net/sourceforge/pmd/util/fxdesigner/LICENSE";


    private SimplePopups() {

    }


    public static void showActionFeedback(@NonNull Node owner, AlertType type, @NonNull String message) {

        @Nullable String iconLit;
        switch (type) {
        case ERROR:
            iconLit = "fas-times";
            break;
        case CONFIRMATION:
            iconLit = "fas-check";
            break;
        case INFORMATION:
            iconLit = "fas-info";
            break;
        case WARNING:
            iconLit = "fas-exclamation";
            break;
        default:
            iconLit = null;
            break;
        }

        Node icon = iconLit == null ? null : new FontIcon(iconLit);

        showActionFeedback(owner, icon, message);
    }

    public static void showActionFeedback(@NonNull Node owner, @NonNull String message) {
        showActionFeedback(owner, (Node) null, message);
    }

    /**
     * Show a transient popup with a message, to let the user know an action
     * was performed.
     *
     * @param owner Node next to which the popup will be shown
     */
    public static void showActionFeedback(@NonNull Node owner, @Nullable Node graphic, @NonNull String message) {

        Popup popup = new Popup();
        Label label = new Label(message, graphic);
        StackPane pane = new StackPane();

        DesignerUtil.addCustomStyleSheets(pane, "designer");
        pane.getStyleClass().addAll("action-feedback");

        pane.getChildren().addAll(label);
        popup.getContent().addAll(pane);

        Animation fadeTransition = bounceFadeAnimation(pane);
        fadeTransition.setOnFinished(e -> popup.hide());

        popup.setOnShowing(e -> fadeTransition.play());

        Bounds screenBounds = owner.localToScreen(owner.getBoundsInLocal());
        popup.show(owner, screenBounds.getMaxX(), screenBounds.getMinY());
    }


    private static Animation bounceFadeAnimation(Node owner) {

        return new Transition() {

            {
                setCycleDuration(Duration.millis(1500));
                setInterpolator(Interpolator.EASE_OUT);
            }


            @Override
            protected void interpolate(double frac) {
                double mapped = clamp(map(frac));
                owner.setOpacity(mapped);
            }

            private double map(double x) {
                double t = x - .5; // translate
                final double plateauWidth= .3;

                double plateau = x > .5 - plateauWidth && x < .5 + plateauWidth ? 1 : 0;
                return (.25 - t * t) * 4 + plateau;
            }

            private double clamp(double i) {
                return min(1, max(0, i));
            }
        };


    }

    public static boolean confirmAction(String question) {
        Alert licenseAlert = new Alert(AlertType.CONFIRMATION);

        licenseAlert.setContentText(question);
        return licenseAlert.showAndWait().map(it -> it == ButtonType.YES).orElse(false);
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

        String sb =
            "PMD core version:\t\t\t" + PMDVersion.VERSION + "\n"
                + "Designer version:\t\t\t" + Designer.getCurrentVersion()
                + " (supports PMD core " + Designer.getPmdCoreMinVersion() + ")\n"
                + "Designer settings dir:\t\t"
                + root.getService(DesignerRoot.DISK_MANAGER).getSettingsDirectory() + "\n"
                + "Available languages:\t\t"
                + LanguageRegistryUtil.getSupportedLanguages().map(Language::getTerseName).collect(Collectors.toList())
                + "\n";

        textArea.setText(sb);
        scroll.setContent(textArea);

        licenseAlert.getDialogPane().setContent(scroll);
        licenseAlert.showAndWait();
    }

}
