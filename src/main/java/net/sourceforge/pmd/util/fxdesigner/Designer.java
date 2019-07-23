/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.sourceforge.pmd.PMDVersion;
import net.sourceforge.pmd.lang.ast.xpath.Attribute;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerParams;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRootImpl;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.ResourceUtil;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;


/**
 * Main class for the designer, launched only if {@link DesignerStarter} detected JavaFX support.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public class Designer extends Application {

    /**
     * Constant that contains always the current version of the designer.
     */
    private static final String VERSION;
    private static final String PMD_CORE_MIN_VERSION;
    private static final String UNKNOWN_VERSION = "unknown";


    /**
     * Determines the version from maven's generated pom.properties file.
     */
    static {
        VERSION = readProperty("/META-INF/maven/net.sourceforge.pmd/pmd-ui/pom.properties", "version").orElse(UNKNOWN_VERSION);
        PMD_CORE_MIN_VERSION = readProperty(ResourceUtil.resolveResource("designer.properties"), "pmd.core.version").orElse(UNKNOWN_VERSION);
    }


    public static String getCurrentVersion() {
        return VERSION;
    }

    public static String getPmdCoreMinVersion() {
        return PMD_CORE_MIN_VERSION;
    }

    private static Optional<String> readProperty(String resourcePath, String key) {
        try (InputStream stream = PMDVersion.class.getResourceAsStream(resourcePath)) {
            if (stream != null) {
                final Properties properties = new Properties();
                properties.load(stream);
                return Optional.ofNullable(properties.getProperty(key));
            }
        } catch (final IOException ignored) {
            // fallthrough
        }
        return Optional.empty();
    }

    private long initStartTimeMillis;
    private DesignerRoot designerRoot;

    public Designer() {
        initStartTimeMillis = System.currentTimeMillis();
    }

    @Override
    public void start(Stage stage) throws IOException {
        DesignerParams params = getParameters() == null ? new DesignerParams() : new DesignerParams(getParameters());
        start(stage, new DesignerRootImpl(stage, params, getHostServices()));
    }


    @Override
    public void stop() {
        designerRoot.shutdownServices();
    }

    public void start(Stage stage, DesignerRoot owner) throws IOException {
        this.designerRoot = owner;

        stage.setTitle("PMD Rule Designer (v " + Designer.VERSION + ')');
        setIcons(stage);

        Logger.getLogger(Attribute.class.getName()).setLevel(Level.OFF);

        System.out.print(stage.getTitle() + " initializing... ");

        FXMLLoader loader = new FXMLLoader(DesignerUtil.getFxml("designer"));

        MainDesignerController mainController = new MainDesignerController(owner);

        loader.setBuilderFactory(DesignerUtil.customBuilderFactory(owner));

        loader.setControllerFactory(DesignerUtil.controllerFactoryKnowing(
            mainController,
            new MetricPaneController(owner),
            new ScopesPanelController(owner),
            new NodeDetailPaneController(owner),
            new RuleEditorsController(owner),
            new SourceEditorController(owner)
        ));

        stage.setOnCloseRequest(e -> {
            owner.getService(DesignerRoot.PERSISTENCE_MANAGER).persistSettings(mainController);
            Platform.exit();
            // VM sometimes fails to exit for no apparent reason
            // all our threads are killed so it's not our fault
            System.exit(0);
        });

        Parent root = loader.load();
        Scene scene = new Scene(root);

        stage.setScene(scene);

        if (!owner.isDeveloperMode()) {
            // only close after initialization succeeded.
            // but before stage.show to reduce unwanted noise
            System.err.close();
        }

        stage.show();

        long initTime = System.currentTimeMillis() - initStartTimeMillis;

        System.out.println("done in " + initTime + "ms.");
        if (!owner.isDeveloperMode()) {
            System.out.println("Run with --verbose parameter to enable error output.");
        }
    }

    /**
     * Only set after {@link #start(Stage)} is called.
     */
    public DesignerRoot getDesignerRoot() {
        return designerRoot;
    }

    private void setIcons(Stage primaryStage) {
        ObservableList<Image> icons = primaryStage.getIcons();
        final String dirPrefix = "icons/app/";
        List<String> imageNames = Arrays.asList("designer_logo.jpeg");

        // TODO make more icon sizes

        List<Image> images = imageNames.stream()
                                       .map(s -> dirPrefix + s)
                                       .map(s -> getClass().getResourceAsStream(s))
                                       .filter(Objects::nonNull)
                                       .map(Image::new)
                                       .collect(Collectors.toList());

        icons.addAll(images);
    }
}
