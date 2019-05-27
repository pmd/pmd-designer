/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import java.io.File;

import org.reactfx.value.Val;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.util.fxdesigner.app.AbstractController;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.services.LogEntry.Category;
import net.sourceforge.pmd.util.fxdesigner.model.ObservableXPathRuleBuilder;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveTestCase;
import net.sourceforge.pmd.util.fxdesigner.model.testing.TestCollection;
import net.sourceforge.pmd.util.fxdesigner.model.testing.TestXmlParser;
import net.sourceforge.pmd.util.fxdesigner.util.controls.TestCaseListCell;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ToolbarTitledPane;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;

public class TestCollectionController extends AbstractController {

    @FXML
    private ToolbarTitledPane titledPane;
    @FXML
    private ListView<LiveTestCase> testsListView;
    @FXML
    private Button importTestsButton;
    @FXML
    private Button addTestButton;
    @FXML
    private Button exportTestsButton; // TODO

    private final ObservableXPathRuleBuilder builder;

    protected TestCollectionController(DesignerRoot root, ObservableXPathRuleBuilder builder) {
        super(root);
        this.builder = builder;
    }

    public Val<LanguageVersion> getDefaultLanguageVersion() {
        return builder.languageProperty().map(Language::getDefaultVersion);
    }

    @Override
    protected void beforeParentInit() {

        testsListView.setCellFactory(c -> new TestCaseListCell(this));
        testsListView.setEditable(true);
        this.testsListView.setItems(getTestCollection().getStash());

        importTestsButton.setOnAction(any -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Load source from file");
            File file = chooser.showOpenDialog(getMainStage());

            TestCollection coll = TestXmlParser.readXmlTestFile(file.toPath(), e -> logUserException(e, Category.TEST_LOADING_EXCEPTION));
            // TODO what if there's already test cases?
            getTestCollection().rebase(coll);
        });

        addTestButton.setOnAction(any -> {
            getTestCollection().getStash().add(new LiveTestCase());
            loadTestCase(getTestCollection().getStash().size() - 1);
        });

    }

    private TestCollection getTestCollection() {
        return builder.getTestCollection();
    }


    public void loadTestCase(int index) {
        LiveTestCase live = getTestCollection().export(index);
        if (live == null) {
            logInternalException(new RuntimeException("Wrong index in test list: " + index));
            return;
        }

        getService(DesignerRoot.TEST_LOADER).handleTestOpenRequest(live);
    }


}