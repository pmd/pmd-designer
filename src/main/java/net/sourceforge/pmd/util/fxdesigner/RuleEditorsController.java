/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import java.util.Collections;
import java.util.List;

import org.reactfx.collection.LiveArrayList;
import org.reactfx.value.Val;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.app.AbstractController;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.model.ObservableXPathRuleBuilder;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentSequence;
import net.sourceforge.pmd.util.fxdesigner.util.controls.MutableTabPane;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.fxml.FXML;


/**
 * Controller for all rule editors. Interfaces between the main app and
 * the individual editors. Also handles persisting the editors (under
 * the form of rule builders).
 *
 * @author Clément Fournier
 */
public class RuleEditorsController extends AbstractController {

    private final ObservableSet<XPathRuleEditorController> currentlySelectedController = FXCollections.observableSet();
    @FXML
    private MutableTabPane<XPathRuleEditorController> xpathEditorsTabPane;

    private ObservableList<ObservableXPathRuleBuilder> xpathRuleBuilders = new LiveArrayList<>();
    private int restoredTabIndex = 0;


    public RuleEditorsController(DesignerRoot parent) {
        super(parent);
    }


    @Override
    protected void beforeParentInit() {

        xpathEditorsTabPane.setControllerSupplier(() -> new XPathRuleEditorController(getDesignerRoot()));

        selectedEditorProperty().changes()
                                .subscribe(ch -> {
                                    // only the results of the currently opened tab are displayed
                                    currentlySelectedController.clear();
                                    if (ch.getNewValue() != null) {
                                        currentlySelectedController.add(ch.getNewValue());
                                    }
                                });

    }


    @Override
    public void afterParentInit() {
        Platform.runLater(() -> {
            // those have just been restored
            ObservableList<ObservableXPathRuleBuilder> ruleSpecs = getRuleSpecs();

            if (ruleSpecs.isEmpty()) {
                // add at least one tab
                xpathEditorsTabPane.addTabWithNewController();
            } else {
                for (ObservableXPathRuleBuilder builder : ruleSpecs) {
                    xpathEditorsTabPane.addTabWithController(new XPathRuleEditorController(getDesignerRoot(), builder));
                }
            }

            xpathEditorsTabPane.getSelectionModel().select(restoredTabIndex);

            // after restoration they're read-only and got for persistence on closing
            xpathRuleBuilders = xpathEditorsTabPane.getControllers().map(XPathRuleEditorController::getRuleBuilder);
        });

    }

    private Val<XPathRuleEditorController> selectedEditorProperty() {
        return xpathEditorsTabPane.currentFocusedController();
    }

    public Val<List<Node>> currentRuleResults() {
        return selectedEditorProperty().flatMap(XPathRuleEditorController::currentResultsProperty)
                                       .orElseConst(Collections.emptyList());
    }
    /*
     *  Persisted properties
     */


    @PersistentProperty
    public int getSelectedTabIndex() {
        return xpathEditorsTabPane.getSelectionModel().getSelectedIndex();
    }


    public void setSelectedTabIndex(int i) {
        restoredTabIndex = i;
    }


    // Persist the rule builders
    // Tab creation on app restore is handled in afterParentInit
    @PersistentSequence
    public ObservableList<ObservableXPathRuleBuilder> getRuleSpecs() {
        return xpathRuleBuilders;
    }


}
