/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.popups;

import org.controlsfx.control.PopOver;
import org.fxmisc.richtext.LineNumberFactory;
import org.reactfx.EventStreams;
import org.reactfx.Subscription;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.app.AbstractController;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.services.ASTManager;
import net.sourceforge.pmd.util.fxdesigner.app.services.LogEntry.Category;
import net.sourceforge.pmd.util.fxdesigner.model.export.LiveTreeRenderer;
import net.sourceforge.pmd.util.fxdesigner.model.export.TreeRendererRegistry;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.StageBuilder;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.AvailableSyntaxHighlighters;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.SyntaxHighlightingCodeArea;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.XmlSyntaxHighlighter;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ControlUtil;
import net.sourceforge.pmd.util.fxdesigner.util.controls.DynamicWidthChoicebox;
import net.sourceforge.pmd.util.fxdesigner.util.controls.PopOverWrapper;
import net.sourceforge.pmd.util.fxdesigner.util.controls.PropertyMapView;
import net.sourceforge.pmd.util.fxdesigner.util.controls.RippleButton;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ToolbarTitledPane;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.stage.Modality;
import javafx.stage.Stage;


/**
 * Controller for the "Export tests to XML" dialog.
 *
 * @author Clément Fournier
 */
public final class TreeExportWizardController extends AbstractController {

    private final Stage myPopupStage;
    private final PopOverWrapper<LiveTreeRenderer> propertiesPopover;
    @FXML
    private RippleButton propertiesMapButton;
    @FXML
    private DynamicWidthChoicebox<LiveTreeRenderer> rendererChoicebox;
    @FXML
    private ToolbarTitledPane titledPane;
    @FXML
    private Button saveToFileButton;
    @FXML
    private Button copyResultButton;
    @FXML
    private SyntaxHighlightingCodeArea exportResultArea;

    private Runnable updater = () -> {};


    public TreeExportWizardController(DesignerRoot root) {
        super(root);
        propertiesPopover = new PopOverWrapper<>(this::rebindPropertiesPopover);

        this.myPopupStage = createStage(root.getMainStage());
    }

    private Stage createStage(Stage mainStage) {
        return new StageBuilder().withOwner(mainStage)
                                 .withFxml(DesignerUtil.getFxml("tree-export-wizard"), getDesignerRoot(), this)
                                 .withModality(Modality.WINDOW_MODAL)
                                 .withTitle("Export tree to text")
                                 .newStage();
    }


    public Subscription bindToTree(ASTManager astManager) {
        update(astManager.compilationUnitProperty().getValue());
        updater = () -> update(astManager.compilationUnitProperty().getValue());
        return astManager.compilationUnitProperty().changes().subscribe(it -> update(it.getNewValue()));
    }

    public Subscription bindToNode(Node node) {
        update(node);
        updater = () -> update(node);
        return Subscription.EMPTY;
    }

    private void update(Node value) {
        try {
            LiveTreeRenderer renderer = rendererChoicebox.getSelectionModel().getSelectedItem();
            String dump = renderer.dumpSubtree(value);
            titledPane.errorMessageProperty().setValue(null);
            exportResultArea.replaceText(dump);
        } catch (Exception e) {
            reportDumpException(e);
        }
    }

    /** Set the given subscription as close handler and show. */
    public void showYourself(Subscription parentBinding) {
        myPopupStage.setOnCloseRequest(e -> parentBinding.unsubscribe());
        exportResultArea.setSyntaxHighlighter(new XmlSyntaxHighlighter());
        myPopupStage.show();
    }

    private void reportDumpException(Throwable e) {
        logUserException(e, Category.TEST_EXPORT_EXCEPTION);
        titledPane.errorMessageProperty().setValue(e.getMessage());
    }

    @Override
    protected void beforeParentInit() {
        exportResultArea.setSyntaxHighlighter(AvailableSyntaxHighlighters.XML);

        ControlUtil.copyToClipboardButton(copyResultButton, exportResultArea::getText);
        ControlUtil.saveToFileButton(saveToFileButton, myPopupStage, exportResultArea::getText, this);


        TreeRendererRegistry rendererRegistry = getService(DesignerRoot.TREE_RENDERER_REGISTRY);

        rendererChoicebox.setConverter(DesignerUtil.stringConverter(LiveTreeRenderer::getName, rendererRegistry::fromId));
        rendererChoicebox.setItems(rendererRegistry.getRenderers());
        rendererChoicebox.getSelectionModel().select(0);

        EventStreams.valuesOf(rendererChoicebox.getSelectionModel().selectedItemProperty()).subscribe(propertiesPopover::rebind);
        ReactfxUtil.subscribeDisposable(
            rendererChoicebox.getSelectionModel().selectedItemProperty(),
            renderer -> renderer.getLiveProperties().nonDefaultProperty().values().subscribe(it -> updater.run())
        );

        EventStreams.valuesOf(rendererChoicebox.getSelectionModel().selectedItemProperty())
                    .map(LiveTreeRenderer::getLiveProperties)
                    .subscribe(props -> propertiesMapButton.setDisable(props.asList().isEmpty()));
        propertiesMapButton.setOnAction(e -> propertiesPopover.showOrFocus(p -> p.show(propertiesMapButton)));

        exportResultArea.setParagraphGraphicFactory(LineNumberFactory.get(exportResultArea));

    }


    private PopOver rebindPropertiesPopover(LiveTreeRenderer testCase, PopOver existing) {
        if (testCase == null && existing != null) {
            existing.hide();
            PropertyMapView view = (PropertyMapView) existing.getUserData();
            view.unbind();
            return existing;
        }

        if (testCase != null) {
            if (existing == null) {
                return PropertyMapView.makePopOver(testCase, getDesignerRoot());
            } else {
                PropertyMapView view = (PropertyMapView) existing.getUserData();
                view.unbind();
                view.bind(testCase);
                return existing;
            }
        }
        return null;
    }

}
