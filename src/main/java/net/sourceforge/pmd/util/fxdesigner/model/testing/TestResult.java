/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

public class TestResult {

    private final TestStatus status;
    private final String message;


    public TestResult(TestStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public TestStatus getStatus() {
        return status;
    }
}
