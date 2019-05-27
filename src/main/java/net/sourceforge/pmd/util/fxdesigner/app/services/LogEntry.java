/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app.services;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.util.DataHolder;


/**
 * Log entry of an {@link EventLoggerImpl}.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public final class LogEntry implements Comparable<LogEntry> {


    private final Category category;
    private final Date timestamp;
    private static final String INDENT = "    ";
    private final Var<String> detailsText = Var.newSimpleVar("");
    private final Var<Boolean> wasExamined = Var.newSimpleVar(false);
    private final Var<String> shortMessage = Var.newSimpleVar("");
    private final boolean isTrace;
    private DataHolder holder = new DataHolder();


    public DataHolder getUserMap() {
        return holder;
    }

    public boolean isWasExamined() {
        return wasExamined.getValue();
    }


    public void setExamined(boolean wasExamined) {
        this.wasExamined.setValue(wasExamined);
    }

    public Var<Boolean> wasExaminedProperty() {
        return wasExamined;
    }


    private LogEntry(String detailsText, String shortMessage, Category cat, boolean isTrace) {
        this.category = cat;
        this.isTrace = isTrace;
        this.detailsText.setValue(detailsText);
        this.shortMessage.setValue(shortMessage);
        this.timestamp = new Date();
    }


    public Category getCategory() {
        return category;
    }


    public Date getTimestamp() {
        return timestamp;
    }

    public Var<String> messageProperty() {
        return shortMessage;
    }

    @Override
    public int compareTo(LogEntry o) {
        return getTimestamp().compareTo(o.getTimestamp());
    }

    public boolean isTrace() {
        return isTrace;
    }

    public Var<String> detailsProperty() {
        return detailsText;
    }

    public LogEntry appendMessage(LogEntry newer) {
        StringBuilder sb = new StringBuilder(detailsProperty().getValue());
        sb.append('\n');
        sb.append(formatDiff(timeDiff(this, newer))).append('\n');
        String otherDetails = newer.detailsProperty().getValue();
        sb.append(INDENT);
        sb.append(otherDetails.replaceAll("\\n", "\n" + INDENT));
        detailsProperty().setValue(sb.toString());
        return this;
    }

    public static LogEntry createUserExceptionEntry(Throwable thrown, Category cat) {
        return new LogEntry(ExceptionUtils.getStackTrace(thrown), thrown.getMessage(), cat, false);
    }

    /**
     * Just for the flag categories {@link Category#PARSE_OK} and {@link Category#XPATH_OK},
     * which are not rendered in the log.
     */
    public static LogEntry createUserFlagEntry(String details, Category flagCategory) {
        return new LogEntry(details, "", flagCategory, false);
    }

    public static <T> LogEntry serviceRegistered(AppServiceDescriptor<T> descriptor, T service) {
        return new LogEntry(service.toString(), descriptor.toString(), Category.SERVICE_REGISTERING, false);
    }

    public static LogEntry createInternalDebugEntry(String shortMessage,
                                                    String details,
                                                    ApplicationComponent component,
                                                    Category category,
                                                    boolean trace) {
        String richDetails = "In " + component.getDebugName() + (StringUtils.isBlank(details) ? "" : "\n\n" + details);
        return new LogEntry(richDetails, shortMessage, category, trace);
    }

    private static int timeDiff(LogEntry a, LogEntry b) {
        return (int) (b.getTimestamp().getTime() - a.getTimestamp().getTime());
    }

    private static String formatDiff(int diff) {
        return (diff > 0 ? "+" + diff : diff) + " ms";
    }

    public enum Category {
        // all of those are "user" categories, which are relevant to a regular user of the app

        PARSE_EXCEPTION("Parse exception"),
        TYPERESOLUTION_EXCEPTION("Type resolution exception"),
        QNAME_RESOLUTION_EXCEPTION("Qualified name resolution exception"),
        SYMBOL_FACADE_EXCEPTION("Symbol façade exception"),
        XPATH_EVALUATION_EXCEPTION("XPath evaluation exception"),

        TEST_LOADING_EXCEPTION("XML test loader exception"),

        // These are "flag" categories that signal that previous exceptions
        // thrown during code or XPath edition may be discarded as uninteresting
        // When in developer mode they're pushed to the event log too
        PARSE_OK("Parsing success", CategoryType.INTERNAL),
        XPATH_OK("XPath evaluation success", CategoryType.INTERNAL),

        // These are used for events that occurred internally to the app and are
        // only relevant to a developer of the app.
        INTERNAL("Internal event", CategoryType.INTERNAL),
        SERVICE_REGISTERING("Service registered", CategoryType.INTERNAL),
        RESOURCE_MANAGEMENT("Resource manager", CategoryType.INTERNAL),
        SELECTION_EVENT_TRACING("Selection event", CategoryType.INTERNAL),
        XPATH_EVENT_FORWARDING("XPath update", CategoryType.INTERNAL),
        ;

        public final String name;
        private final CategoryType type;


        Category(String name) {
            this(name, CategoryType.USER_EXCEPTION);
        }


        Category(String name, CategoryType type) {
            this.name = name;
            this.type = type;
        }


        @Override
        public String toString() {
            return name;
        }


        /** Internal categories are only logged if the app is in developer mode. */
        public boolean isInternal() {
            return type != CategoryType.USER_EXCEPTION;
        }


        public boolean isUserException() {
            return type == CategoryType.USER_EXCEPTION;
        }


        enum CategoryType {
            USER_EXCEPTION,
            INTERNAL
        }
    }

}
