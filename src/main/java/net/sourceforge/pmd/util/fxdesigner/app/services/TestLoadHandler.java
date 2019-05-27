/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app.services;

import org.checkerframework.checker.nullness.qual.NonNull;

import net.sourceforge.pmd.util.fxdesigner.model.testing.LiveTestCase;

public interface TestLoadHandler {


    /**
     * Handle a load request for a single test case.
     * Previously loaded test must be clearer or committed.
     *
     * @param liveTestCase Test to load
     */
    void handleTestOpenRequest(@NonNull LiveTestCase liveTestCase);


}
