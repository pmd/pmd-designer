/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.util.List;

import org.reactfx.collection.LiveList;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextRange;

public final class TestCaseUtil {


    private TestCaseUtil() {
        // util
    }

    public static TestResult doTest(LiveTestCase testCase, List<Node> actual) {

        // TODO messages

        LiveList<LiveViolationRecord> expected = testCase.getExpectedViolations();
        if (actual.size() != expected.size()) {
            return new TestResult(TestStatus.FAIL,
                                  "Expected " + expected.size() + " violations, actual " + actual.size());
        }

        if (expected.stream().noneMatch(it -> it.getRange() != null)) {
            return new TestResult(TestStatus.PASS, null);
        }

        for (int i = 0; i < actual.size(); i++) {
            Node node = actual.get(i);

            TextRange actualRange = PmdCoordinatesSystem.rangeOf(node);
            TextRange expectedRange = expected.get(i).getRange();

            if (expectedRange != null && !expectedRange.contains(actualRange.startPos)) {
                return new TestResult(TestStatus.FAIL, "Wrong position for node at line " + actualRange.startPos.line);
            }
        }

        return new TestResult(TestStatus.PASS, null);
    }


}
