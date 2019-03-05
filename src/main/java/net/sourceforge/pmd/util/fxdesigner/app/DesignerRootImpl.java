/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app;

import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.app.LogEntry.Category;
import net.sourceforge.pmd.util.fxdesigner.app.NodeSelectionSource.NodeSelectionEvent;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;

import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;


/**
 * Interface for the singleton of the app.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public final class DesignerRootImpl implements DesignerRoot {


    private final Stage mainStage;
    private final EventLogger logger;
    private final boolean developerMode;
    private final Var<Node> globalCompilationUnit = Var.newSimpleVar(null);
    private final Var<LanguageVersion> globalLanguageVersion = Var.newSimpleVar(DesignerUtil.defaultLanguageVersion());
    private final Var<Boolean> isCtrlDown = Var.newSimpleVar(false);

    private final MessageChannel<NodeSelectionEvent> nodeSelectionChannel = new MessageChannel<>(Category.SELECTION_EVENT_TRACING);


    public DesignerRootImpl(Stage mainStage, boolean developerMode) {
        this.mainStage = mainStage;
        this.developerMode = developerMode;
        this.logger = new EventLogger(this);

        mainStage.addEventHandler(KeyEvent.KEY_PRESSED, e -> isCtrlDown.setValue(e.isControlDown()));
        mainStage.addEventHandler(KeyEvent.KEY_RELEASED, e -> isCtrlDown.setValue(e.isControlDown()));
    }


    @Override
    public EventLogger getLogger() {
        return logger;
    }


    @Override
    public Stage getMainStage() {
        return mainStage;
    }


    @Override
    public boolean isDeveloperMode() {
        return developerMode;
    }


    @Override
    public MessageChannel<NodeSelectionEvent> getNodeSelectionChannel() {
        return nodeSelectionChannel;
    }


    @Override
    public Var<Node> globalCompilationUnitProperty() {
        return globalCompilationUnit;
    }


    @Override
    public Var<LanguageVersion> globalLanguageVersionProperty() {
        return globalLanguageVersion;
    }

    @Override
    public Val<Boolean> isCtrlDownProperty() {
        return isCtrlDown;
    }
}
