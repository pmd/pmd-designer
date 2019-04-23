/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;

import net.sourceforge.pmd.PMDVersion;
import net.sourceforge.pmd.lang.ast.xpath.Attribute;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerParams;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRootImpl;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;

import com.sun.javafx.fxml.builder.ProxyBuilder;
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
    public static final String VERSION;
    private static final String UNKNOWN_VERSION = "unknown";

    private static final Logger LOG = Logger.getLogger(Designer.class.getName());


    /**
     * Determines the version from maven's generated pom.properties file.
     */
    static {
        String pmdVersion = UNKNOWN_VERSION;
        try (InputStream stream = PMDVersion.class.getResourceAsStream("/META-INF/maven/net.sourceforge.pmd/pmd-ui/pom.properties")) {
            if (stream != null) {
                final Properties properties = new Properties();
                properties.load(stream);
                pmdVersion = properties.getProperty("version");
            }
        } catch (final IOException e) {
            LOG.log(Level.FINE, "Couldn't determine version of PMD", e);
        }

        VERSION = pmdVersion;
    }


    private long initStartTimeMillis;
    private DesignerRoot designerRoot;

    public Designer() {
        initStartTimeMillis = System.currentTimeMillis();
    }

    @Override
    public void start(Stage stage) throws IOException {
        DesignerParams params = getParameters() == null ? new DesignerParams() : new DesignerParams(getParameters());
        start(stage, new DesignerRootImpl(stage, params));
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

        FXMLLoader loader = new FXMLLoader(DesignerUtil.getFxml("designer.fxml"));

        MainDesignerController mainController = new MainDesignerController(owner);

        loader.setBuilderFactory(type -> {

            boolean needsRoot = Arrays.stream(type.getConstructors()).anyMatch(it -> ArrayUtils.contains(it.getParameterTypes(), DesignerRoot.class));

            if (needsRoot) {
                // Controls that need the DesignerRoot can declare a constructor
                // with a parameter w/ signature @NamedArg("designerRoot") DesignerRoot
                // to be injected with the relevant instance of the app.
                ProxyBuilder<Object> builder = new ProxyBuilder<>(type);
                builder.put("designerRoot", owner);
                return builder;
            } else {
                return null; //use default
            }
        });

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
