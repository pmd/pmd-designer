/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app;

import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.reactfx.value.Val;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.util.fxdesigner.SourceEditorController;
import net.sourceforge.pmd.util.fxdesigner.app.services.AppServiceDescriptor;
import net.sourceforge.pmd.util.fxdesigner.app.services.EventLogger;
import net.sourceforge.pmd.util.fxdesigner.app.services.LogEntry;
import net.sourceforge.pmd.util.fxdesigner.app.services.LogEntry.Category;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsOwner;
import net.sourceforge.pmd.util.fxdesigner.util.controls.AstTreeView;

import javafx.application.Platform;
import javafx.scene.control.Control;
import javafx.stage.Stage;


/**
 * Some part of the application, e.g. a controller. Components in an instance of the app are all linked
 * to the same {@link DesignerRoot}, which hosts utilities globally available to the app, e.g. the logger.
 *
 * <p>Components that are not controllers, e.g. {@link Control}s, should be injected with the designer
 * root at initialization time, eg what {@link SourceEditorController} does with {@link AstTreeView}.
 *
 * <p>Some more specific cross-cutting structures for the internals of the app are the {@link SettingsOwner}
 * tree, which is more or less identical to the {@link AbstractController} tree.
 *
 * @author Clément Fournier
 */
@FunctionalInterface
public interface ApplicationComponent {


    DesignerRoot getDesignerRoot();


    default <T> T getService(AppServiceDescriptor<T> descriptor) {
        return getDesignerRoot().getService(descriptor);
    }


    default LanguageVersion getGlobalLanguageVersion() {
        return getService(DesignerRoot.AST_MANAGER).languageVersionProperty().getValue();
    }


    /**
     * The language is now global to the app.
     */
    default Val<@NonNull Language> globalLanguageProperty() {
        return getService(DesignerRoot.APP_GLOBAL_LANGUAGE);
    }


    /**
     * Gets the logger of the application. Events pushed to the logger
     * are filtered then forwarded to the Event Log control.
     *
     * @return The logger
     */
    default EventLogger getLogger() {
        return getService(DesignerRoot.LOGGER);
    }


    /**
     * A debug name for this component, used in developer mode to e.g. trace events
     * handling paths.
     */
    default String getDebugName() {
        return getClass().getSimpleName();
    }


    /**
     * A default category for exceptions coming from this component.
     */
    default Category getLogCategory() {
        return Category.INTERNAL;
    }


    /**
     * Gets the main stage of the application.
     */
    default Stage getMainStage() {
        return getDesignerRoot().getMainStage();
    }


    /**
     * If true, some more events are pushed to the event log, and
     * console streams are open. This is enabled by the -v or --verbose
     * option on command line for now.
     */
    default boolean isDeveloperMode() {
        return getDesignerRoot().isDeveloperMode();
    }


    /**
     * Notify the logger of an exception that somewhere in PMD logic. Exceptions raised
     * by the app logic are considered internal and should be forwarded to the logger
     * using {@link #logInternalException(Throwable)}. If we're not in developer mode
     * they will be ignored.
     */
    default void logUserException(Throwable throwable, Category category) {
        getLogger().logEvent(LogEntry.createUserExceptionEntry(throwable, category));
    }


    /**
     * Notify the logger that XPath parsing succeeded and that the last recent failure may be thrown away.
     * Only logged in developer mode.
     */
    default void raiseParsableXPathFlag() {
        getLogger().logEvent(LogEntry.createUserFlagEntry("", Category.XPATH_OK));
    }


    /**
     * Notify the logger that source code parsing succeeded and that the last recent failure may be thrown away.
     * Only logged in developer mode.
     */
    default void raiseParsableSourceFlag(Supplier<String> details) {
        String realDetails = isDeveloperMode() ? details.get() : "";
        getLogger().logEvent(LogEntry.createUserFlagEntry(realDetails, Category.PARSE_OK));
    }

    // Internal log handlers


    /** Logs an exception that occurred somewhere in the app logic. */
    default void logInternalException(Throwable throwable) {
        if (isDeveloperMode()) {
            System.err.println("Exception in " + this.getDebugName() + ": " + throwable.getMessage());
            System.err.println("  See the event log for more info");
            getLogger().logEvent(LogEntry.createUserExceptionEntry(throwable, getLogCategory()));
        }
    }


    /** Logs an exception that occurred somewhere in the app logic. */
    default void logInternalDebugInfo(Supplier<String> shortMessage, Supplier<String> details) {
        logInternalDebugInfo(shortMessage, details, false);
    }


    /** Logs an exception that occurred somewhere in the app logic. */
    default void logInternalDebugInfo(Supplier<String> shortMessage, Supplier<String> details, boolean trace) {
        if (isDeveloperMode()) {
            Platform.runLater(() -> getLogger().logEvent(LogEntry.createInternalDebugEntry(shortMessage.get(),
                                                                                           details.get(),
                                                                                           this,
                                                                                           getLogCategory(),
                                                                                           trace)));
        }
    }


}
