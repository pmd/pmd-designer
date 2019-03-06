package net.sourceforge.pmd.util.fxdesigner.app.services;

import static net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil.countNotMatching;

import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;

/**
 * Logs events. Stores the whole log in case no view was open.
 *
 * @author Clément Fournier
 * @since 6.13.0
 */
public interface EventLogger {

    /** Number of log entries that were not yet examined by the user. */
    default Val<Integer> numNewLogEntriesProperty() {
        return countNotMatching(getLog().map(LogEntry::wasExaminedProperty));
    }


    /** Total number of log entries. */
    default Val<Integer> numLogEntriesProperty() {
        return getLog().sizeProperty();
    }


    /**
     * Logs a new event.
     */
    void logEvent(LogEntry event);


    /**
     * Returns the full log.
     */
    LiveList<LogEntry> getLog();
}