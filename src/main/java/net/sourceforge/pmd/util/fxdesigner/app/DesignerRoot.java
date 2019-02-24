package net.sourceforge.pmd.util.fxdesigner.app;

import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.ast.Node;

import javafx.stage.Stage;


/**
 * @author Clément Fournier
 */
public interface DesignerRoot {
    /**
     * Gets the logger of the application.
     *
     * @return The logger
     */
    EventLogger getLogger();


    /**
     * Gets the main stage of the application.
     *
     * @return The main stage
     */
    Stage getMainStage();


    /**
     * If true, some more events are pushed to the event log, and
     * console streams are open. This is enabled by the -v or --verbose
     * option on command line for now.
     */
    boolean isDeveloperMode();


    /**
     * Channel used to transmit node selection events to all interested components.
     */
    MessageChannel<Node> getNodeSelectionChannel();


    Val<Node> currentCompilationUnitProperty();


    Val<Boolean> isCtrlDownProperty();
}
