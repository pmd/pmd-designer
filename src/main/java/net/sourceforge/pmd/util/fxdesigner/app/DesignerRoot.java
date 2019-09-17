/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.util.fxdesigner.app.NodeSelectionSource.NodeSelectionEvent;
import net.sourceforge.pmd.util.fxdesigner.app.services.ASTManager;
import net.sourceforge.pmd.util.fxdesigner.app.services.AppServiceDescriptor;
import net.sourceforge.pmd.util.fxdesigner.app.services.CloseableService;
import net.sourceforge.pmd.util.fxdesigner.app.services.EventLogger;
import net.sourceforge.pmd.util.fxdesigner.app.services.GlobalDiskManager;
import net.sourceforge.pmd.util.fxdesigner.app.services.PersistenceManager;
import net.sourceforge.pmd.util.fxdesigner.app.services.RichTextMapper;
import net.sourceforge.pmd.util.fxdesigner.app.services.TestCreatorService;
import net.sourceforge.pmd.util.fxdesigner.model.VersionedXPathQuery;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveTestCase;

import javafx.application.HostServices;
import javafx.stage.Stage;


/**
 * Provides access to the globals of the app.
 *
 * @author Clément Fournier
 */
public interface DesignerRoot {

    // Those are shared by all the app

    /** JavaFX host services. */
    AppServiceDescriptor<HostServices> HOST_SERVICES = new AppServiceDescriptor<>(HostServices.class);

    /** Manages settings persistence. */
    AppServiceDescriptor<PersistenceManager> PERSISTENCE_MANAGER = new AppServiceDescriptor<>(PersistenceManager.class);
    /** Logger of the app. */
    AppServiceDescriptor<EventLogger> LOGGER = new AppServiceDescriptor<>(EventLogger.class);

    AppServiceDescriptor<GlobalDiskManager> DISK_MANAGER = new AppServiceDescriptor<>(GlobalDiskManager.class);

    // Those are local to one edit session

    /** Maps a node to its rich text representation. */
    AppServiceDescriptor<RichTextMapper> RICH_TEXT_MAPPER = new AppServiceDescriptor<>(RichTextMapper.class);
    /** Channel used to transmit node selection events to all interested components. */
    AppServiceDescriptor<MessageChannel<NodeSelectionEvent>> NODE_SELECTION_CHANNEL = new AppServiceDescriptor<>(MessageChannel.class);
    /** AST manager of the current code. */
    AppServiceDescriptor<ASTManager> AST_MANAGER = new AppServiceDescriptor<>(ASTManager.class);

    /** Requests to load a test case in the editor. */
    AppServiceDescriptor<MessageChannel<@Nullable LiveTestCase>> TEST_LOADER = new AppServiceDescriptor<>(MessageChannel.class);

    AppServiceDescriptor<Val<Language>> APP_GLOBAL_LANGUAGE = new AppServiceDescriptor<>(Val.class);

    /**
     *  Requests to create a test case for the currently open rule.
     *  The test case should be deep copied first *by the sender*.
     */
    AppServiceDescriptor<TestCreatorService> TEST_CREATOR = new AppServiceDescriptor<>(TestCreatorService.class);

    AppServiceDescriptor<MessageChannel<VersionedXPathQuery>> LATEST_XPATH = new AppServiceDescriptor<>(MessageChannel.class);

    AppServiceDescriptor<Var<Boolean>> IS_NODE_BEING_DRAGGED = new AppServiceDescriptor<>(Var.class);


    /**
     * Gets the instance of a service shared by the app.
     *
     * @param descriptor Service descriptor
     */
    <T> T getService(AppServiceDescriptor<T> descriptor);


    /**
     * Register a service for the given descriptor.
     *
     * @throws IllegalStateException if the service was already registered to some other component
     */
    <T> void registerService(AppServiceDescriptor<T> descriptor, T component);


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
     * Returns true if the ctrl key is being pressed.
     * Vetoed by any other key press.
     */
    Val<Boolean> isCtrlDownProperty(); // TODO this may also be extracted


    /**
     * Shutdown all registered service components that
     * implement {@link CloseableService}. Called when
     * the app exits.
     */
    void shutdownServices();


    /**
     * Returns a new designer root that can delegates to
     * this one, can register services independently to
     * override them.
     */
    default DesignerRoot spawnScope() {
        return new ScopedRoot(this);
    }

}
