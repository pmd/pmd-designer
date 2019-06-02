/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app.services;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.reactfx.Subscription;

import net.sourceforge.pmd.util.fxdesigner.app.MessageChannel;
import net.sourceforge.pmd.util.fxdesigner.app.services.LogEntry.Category;
import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveTestCase;

public class TestCreatorService {

    private final MessageChannel<@NonNull LiveTestCase> additionRequests = new MessageChannel<>(Category.TEST_LOADING_EVENT);
    private final MessageChannel<Void> sourceFetchRequests = new MessageChannel<>(Category.TEST_LOADING_EVENT);


    /**
     * Messages going from the source controller to the open test controller,
     * asking to load some new test case. The test case should be deep copied
     * first *by the sender*.
     */
    public MessageChannel<@NonNull LiveTestCase> getAdditionRequests() {
        return additionRequests;
    }

    /**
     * Ticks emitted by the test controller, explicitly asking the source
     * controller to send back the current source in {@link #getAdditionRequests()}.
     */
    public MessageChannel<Void> getSourceFetchRequests() {
        return sourceFetchRequests;
    }

    /**
     * Assumes that this one is the local one.
     */
    public Subscription connect(TestCreatorService global) {
        return getAdditionRequests().connect(global.getAdditionRequests())
                                    .and(getSourceFetchRequests().connect(global.getSourceFetchRequests()));
    }
}
