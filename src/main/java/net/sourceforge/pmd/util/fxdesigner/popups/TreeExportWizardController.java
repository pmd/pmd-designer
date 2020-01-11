/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.popups;

import java.io.StringWriter;

import org.fxmisc.richtext.LineNumberFactory;
import org.reactfx.Subscription;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.app.AbstractController;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.services.ASTManager;
import net.sourceforge.pmd.util.fxdesigner.app.services.LogEntry.Category;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.StageBuilder;
import net.sourceforge.pmd.util.fxdesigner.util.XmlDumpUtil;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.AvailableSyntaxHighlighters;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.SyntaxHighlightingCodeArea;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.syntaxhighlighting.XmlSyntaxHighlighter;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ControlUtil;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ToolbarTitledPane;

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
    @FXML
    private ToolbarTitledPane titledPane;
    @FXML
    private Button saveToFileButton;
    @FXML
    private Button copyResultButton;
    @FXML
    private SyntaxHighlightingCodeArea exportResultArea;


    public TreeExportWizardController(DesignerRoot root) {
        super(root);
        this.myPopupStage = createStage(root.getMainStage());

        exportResultArea.setParagraphGraphicFactory(LineNumberFactory.get(exportResultArea));
    }

    private Stage createStage(Stage mainStage) {
        return new StageBuilder().withOwner(mainStage)
                                 .withFxml(DesignerUtil.getFxml("tree-export-wizard"), getDesignerRoot(), this)
                                 .withModality(Modality.WINDOW_MODAL)
                                 .withTitle("Export tree to XML")
                                 .newStage();
    }


    public Subscription bindToTree(ASTManager testCollection) {
        return testCollection.compilationUnitProperty().changes().subscribe(it -> update(it.getNewValue()));
    }

    private void update(Node value) {
        try {
            StringWriter write = new StringWriter();
            XmlDumpUtil.appendXml(write, value);
            titledPane.errorMessageProperty().setValue(null);
            exportResultArea.replaceText(write.toString());
        } catch (Exception e) {
            reportDumpException(e);
        }
    }

    /** Set the given subscription as close handler and show. */
    public void showYourself(Subscription parentBinding) {
        myPopupStage.setOnCloseRequest(e -> parentBinding.unsubscribe());
        exportResultArea.setSyntaxHighlighter(new XmlSyntaxHighlighter());
        myPopupStage.show();
        update(getService(DesignerRoot.AST_MANAGER).compilationUnitProperty().getValue());
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
    }

}
