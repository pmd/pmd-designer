/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.reactfx.Subscription;
import org.reactfx.collection.LiveArrayList;
import org.reactfx.value.Val;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.app.AbstractController;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.MessageChannel;
import net.sourceforge.pmd.util.fxdesigner.app.services.LogEntry.Category;
import net.sourceforge.pmd.util.fxdesigner.app.services.TestCreatorService;
import net.sourceforge.pmd.util.fxdesigner.model.ObservableXPathRuleBuilder;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveTestCase;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentSequence;
import net.sourceforge.pmd.util.fxdesigner.util.controls.MutableTabPane;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

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
 * <p>Each {@link XPathRuleEditorController} has its own {@link DesignerRoot}
 * with scopes some services down to it (see {@link #newScope()}).
 * This allows keeping several rule editors independent, this class being
 * the bridge between the selected one and the static top of the app.
 *
 * @author Clément Fournier
 */
public class RuleEditorsController extends AbstractController {

    private final ObservableSet<XPathRuleEditorController> currentlySelectedController = FXCollections.observableSet();
    @FXML
    private MutableTabPane<XPathRuleEditorController> mutableTabPane;

    private ObservableList<ObservableXPathRuleBuilder> xpathRuleBuilders = new LiveArrayList<>();
    private int restoredTabIndex = 0;


    public RuleEditorsController(DesignerRoot parent) {
        super(parent);
    }


    @Override
    protected void beforeParentInit() {

        mutableTabPane.setControllerSupplier(() -> new XPathRuleEditorController(newScope()));
        mutableTabPane.setDeepCopyFunction(t -> new XPathRuleEditorController(newScope(), t.getRuleBuilder().deepCopy()));

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
                mutableTabPane.addTabWithNewController();
            } else {
                for (ObservableXPathRuleBuilder builder : ruleSpecs) {
                    mutableTabPane.addTabWithController(new XPathRuleEditorController(newScope(), builder));
                }
            }

            mutableTabPane.getSelectionModel().select(restoredTabIndex);

            // after restoration they're read-only and got for persistence on closing
            xpathRuleBuilders = mutableTabPane.getControllers().map(XPathRuleEditorController::getRuleBuilder);


        });

    }

    @Override
    protected void afterChildrenInit() {

        ReactfxUtil.subscribeDisposable(
            selectedEditorProperty(),
            // connect the currently selected rule to the global state of the app
            x -> {
                TestCreatorService localCreator = x.getService(DesignerRoot.TEST_CREATOR);
                TestCreatorService globalCreator = getService(DesignerRoot.TEST_CREATOR);
                return Subscription.multi(
                    // it's downstream.connect(upstream)
                    getService(DesignerRoot.LATEST_XPATH).connect(x.getService(DesignerRoot.LATEST_XPATH)),
                    getService(DesignerRoot.TEST_LOADER).connect(x.getService(DesignerRoot.TEST_LOADER)),
                    // those two channels forward messages in opposite directions
                    localCreator.getAdditionRequests().connect(globalCreator.getAdditionRequests()),
                    globalCreator.getSourceFetchRequests().connect(localCreator.getSourceFetchRequests())
                );
            });

        selectedEditorProperty().values().filter(Objects::nonNull)
                                .subscribe(it -> getService(DesignerRoot.TEST_LOADER).pushEvent(this, it.selectedTestCaseProperty().getOpt().map(LiveTestCase::unfreeze).orElse(null)));
    }

    public DesignerRoot newScope() {
        // mock some services
        DesignerRoot scope = getDesignerRoot().spawnScope();
        scope.registerService(DesignerRoot.LATEST_XPATH, new MessageChannel<>(Category.XPATH_EVENT_FORWARDING));
        scope.registerService(DesignerRoot.TEST_LOADER, new MessageChannel<>(Category.TEST_LOADING_EVENT));
        scope.registerService(DesignerRoot.TEST_CREATOR, new TestCreatorService());
        return scope;
    }


    private Val<XPathRuleEditorController> selectedEditorProperty() {
        return mutableTabPane.currentFocusedController();
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
        return mutableTabPane.getSelectionModel().getSelectedIndex();
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
