/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.document.TextRegion;

public final class TestCaseUtil {


    private TestCaseUtil() {
        // util
    }

    private static final Comparator<Node> LINE_COMP = Comparator.comparingInt(Node::getBeginLine);

    public static TestResult doTest(LiveTestCase testCase, List<Node> actual) {

        // TODO messages

        List<LiveViolationRecord> expectedViolations = testCase.getExpectedViolations();
        if (actual.size() != expectedViolations.size()) {
            return new TestResult(TestStatus.FAIL,
                                  "Expected " + expectedViolations.size() + " violations, actual " + actual.size());
        }

        actual = new ArrayList<>(actual);
        actual.sort(LINE_COMP);

        expectedViolations = new ArrayList<>(expectedViolations);
        expectedViolations.sort(Comparator.naturalOrder());

        for (int i = 0; i < actual.size(); i++) {
            Node node = actual.get(i);

            TextRegion actualRange = node.getTextRegion();
            LiveViolationRecord expected = expectedViolations.get(i);
            TextRegion expectedRange = expected.getRegion();

            if (!expectedRange.contains(actualRange.getStartOffset())
                || expected.getLine() > 0 && node.getBeginLine() != expected.getLine()) {
                return new TestResult(TestStatus.FAIL, "Wrong position for node at offset " + actualRange.getStartOffset());
            }
        }

        return new TestResult(TestStatus.PASS, null);
    }


}
