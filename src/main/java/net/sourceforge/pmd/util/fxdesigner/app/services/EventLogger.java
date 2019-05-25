/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app.services;

import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;

import com.github.oowekyala.rxstring.ReactfxExtensions;

/**
 * Logs events. Stores the whole log in case no view was open.
 *
 * @author Clément Fournier
 * @since 6.13.0
 */
public interface EventLogger {

    /** Number of log entries that were not yet examined by the user. */
    default Val<Integer> numNewLogEntriesProperty() {
        return LiveList.sizeOf(ReactfxExtensions.flattenVals(getLog().map(LogEntry::wasExaminedProperty))
                                                .filtered(read -> !read));
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
