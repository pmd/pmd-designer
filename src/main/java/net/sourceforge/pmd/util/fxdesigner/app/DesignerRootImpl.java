/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app;

import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.app.LogEntry.Category;

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
    private final Var<Node> currentCompilationUnit = Var.newSimpleVar(null);
    private final Var<Boolean> isCtrlDown = Var.newSimpleVar(false);

    private final MessageChannel<Node> nodeSelectionChannel = new MessageChannel<>(Category.SELECTION_EVENT_TRACING);


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
    public MessageChannel<Node> getNodeSelectionChannel() {
        return nodeSelectionChannel;
    }


    @Override
    public Var<Node> currentCompilationUnitProperty() {
        return currentCompilationUnit;
    }


    @Override
    public Val<Boolean> isCtrlDownProperty() {
        return isCtrlDown;
    }
}
