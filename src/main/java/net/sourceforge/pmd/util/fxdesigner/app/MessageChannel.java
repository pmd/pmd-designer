/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

import org.reactfx.EventSource;
import org.reactfx.EventStream;
import org.reactfx.value.Val;

import net.sourceforge.pmd.util.fxdesigner.MainDesignerController;
import net.sourceforge.pmd.util.fxdesigner.app.services.AppServiceDescriptor;
import net.sourceforge.pmd.util.fxdesigner.app.services.LogEntry;
import net.sourceforge.pmd.util.fxdesigner.app.services.LogEntry.Category;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;


/**
 * Implements some kind of messenger pattern. Conceptually just a globally accessible
 * {@link EventSource} with some logging logic.
 *
 * <p>This patterns allows us to reduce coupling between controllers. The mediator pattern
 * implemented by {@link MainDesignerController} was starting to become very obnoxious,
 * every controller had to keep a reference to the main controller, and we had to implement
 * several levels of delegation for deeply nested controllers. Centralising message passing
 * into a few message channels also improves debug logging.
 *
 * <p>This abstraction is not sufficient to remove the mediator. The missing pieces are the
 * following:
 * <ul>
 * <li>Global state of the app: that's exposed through Vals on the {@link DesignerRoot}</li>
 * <li>Transformation requests: that's exposed through an {@link AppServiceDescriptor}</li>
 * </ul>
 *
 * @param <T> Type of the messages of this channel
 *
 * @author Clément Fournier
 * @since 6.12.0
 */
public class MessageChannel<T> {

    private final EventSource<Message<T>> channel = new EventSource<>();
    private final Val<Message<T>> latestMessage = ReactfxUtil.latestValue(channel);
    private final Category logCategory;


    MessageChannel(Category logCategory) {
        this.logCategory = logCategory;
        latestMessage.pin();
    }


    /**
     * Returns a stream of messages to be processed by the given component.
     *
     * @param component Component listening to the channel
     *
     * @return A stream of messages
     */
    public EventStream<T> messageStream(boolean alwaysHandle,
                                        ApplicationComponent component) {
        // Eliminate duplicate messages in close succession.
        // TreeView selection is particularly shitty in that regard because
        // it emits many events for what corresponds to one click

        // This relies on the equality of two messages, so equals and hashcode
        // must be used correctly.
        return ReactfxUtil.distinctBetween(channel, Duration.ofMillis(100))
                          .hook(message -> logMessageTrace(component, message, () -> ""))
                          .filter(message -> alwaysHandle || !component.equals(message.getOrigin()))
                          .map(Message::getContent);
    }

    public Val<T> latestMessage() {
        return latestMessage.map(Message::getContent);
    }

    /**
     * Notifies the listeners of this channel with the given payload.
     * In developer mode, all messages are logged. The content may be
     * null.
     *
     * @param origin  Origin of the message
     * @param content Message to transmit
     */
    public void pushEvent(ApplicationComponent origin, T content) {
        channel.push(new Message<>(origin, logCategory, content));
    }

    /** Traces a message. */
    private static <T> void logMessageTrace(ApplicationComponent component, Message<T> event, Supplier<String> details) {
        if (component.isDeveloperMode()) {
            LogEntry entry = LogEntry.createInternalDebugEntry(event.toString(),
                                                               details.get(),
                                                               component,
                                                               event.getCategory(),
                                                               true);
            component.getLogger().logEvent(entry);
        }
    }


    /**
     * A message transmitted through a {@link MessageChannel}.
     * It's a pure data class.
     */
    public static final class Message<T> {

        private final T content;
        private final Category category;
        private final ApplicationComponent origin;


        Message(ApplicationComponent origin, Category category, T content) {
            this.content = content;
            this.category = category;
            this.origin = origin;
        }


        public Category getCategory() {
            return category;
        }


        /** Payload of the message. */
        public T getContent() {
            return content;
        }


        /** Component that pushed the message. */
        public ApplicationComponent getOrigin() {
            return origin;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Message that = (Message) o;
            return Objects.equals(content, that.content)
                && Objects.equals(origin, that.origin);
        }


        @Override
        public int hashCode() {
            return Objects.hash(content, origin);
        }


        @Override
        public String toString() {
            return getContent() + "(" + hashCode() + ") from " + getOrigin().getDebugName();
        }
    }
}
