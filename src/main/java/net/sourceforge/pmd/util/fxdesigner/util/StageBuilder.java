/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import static net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil.controllerFactoryKnowing;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil.customBuilderFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.NonNull;

import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * @author Clément Fournier
 */
public final class StageBuilder {


    private String title;
    private Modality modality;
    private Window owner;
    private StageStyle stageStyle;
    private Scene scene;
    private Object userData;

    public StageBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public StageBuilder withModality(Modality modality) {
        this.modality = modality;
        return this;
    }

    public StageBuilder withStyle(StageStyle stageStyle) {
        this.stageStyle = stageStyle;
        return this;
    }

    public StageBuilder withOwner(Window owner) {
        this.owner = owner;
        return this;
    }

    public StageBuilder withScene(Scene scene) {
        this.scene = scene;
        return this;
    }

    public StageBuilder withSceneRoot(Parent parent) {
        return withScene(new Scene(parent));
    }

    public StageBuilder withFxml(URL fxmlUrl, @NonNull DesignerRoot root, Object... controllers) {
        FXMLLoader loader = new FXMLLoader(fxmlUrl);
        loader.setBuilderFactory(customBuilderFactory(root));
        loader.setControllerFactory(controllerFactoryKnowing(controllers));
        Parent parent;
        try {
            parent = loader.load();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return withSceneRoot(parent);
    }

    public StageBuilder withUserData(Object userData) {
        this.userData = userData;
        return this;
    }

    public Stage configure(Stage stage) {

        if (owner != null) {
            stage.initOwner(owner);
        }
        if (modality != null) {
            stage.initModality(modality);
        }
        if (stageStyle != null) {
            stage.initStyle(stageStyle);
        }

        Objects.requireNonNull(title, "Untitled stage!");
        Objects.requireNonNull(scene, "No scene for stage!");

        stage.setTitle(title);
        stage.setScene(scene);
        stage.setUserData(userData);
        return stage;
    }

    public Stage newStage() {
        return configure(new Stage());
    }


}
