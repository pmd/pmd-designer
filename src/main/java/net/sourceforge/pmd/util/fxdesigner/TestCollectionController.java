/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import net.sourceforge.pmd.util.fxdesigner.app.AbstractController;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.model.testing.StashedTestCase;
import net.sourceforge.pmd.util.fxdesigner.model.testing.TestCollection;
import net.sourceforge.pmd.util.fxdesigner.util.controls.ToolbarTitledPane;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;

public class TestCollectionController extends AbstractController {

    @FXML
    private ToolbarTitledPane titledPane;
    @FXML
    private ListView<StashedTestCase> testsListView;
    @FXML
    private Button importTestsButton;
    @FXML
    private Button addTestButton;
    @FXML
    private Button exportTestsButton;
    
    private final TestCollection collection;

    protected TestCollectionController(DesignerRoot root, TestCollection collection) {
        super(root);
        this.collection = collection;
    }


    @Override
    protected void beforeParentInit() {

    }


    //    private

}
