/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextRange;

public final class TestCaseUtil {


    private TestCaseUtil() {
        // util
    }

    private static final Comparator<Node> LINE_COMP = Comparator.comparingInt(Node::getBeginLine);

    public static TestResult doTest(LiveTestCase testCase, List<Node> actual) {

        // TODO messages

        List<LiveViolationRecord> expected = testCase.getExpectedViolations();
        if (actual.size() != expected.size()) {
            return new TestResult(TestStatus.FAIL,
                                  "Expected " + expected.size() + " violations, actual " + actual.size());
        }

        if (expected.stream().noneMatch(it -> it.getRange() != null)) {
            return new TestResult(TestStatus.PASS, null);
        }


        actual = new ArrayList<>(actual);
        actual.sort(LINE_COMP);

        expected = new ArrayList<>(expected);
        expected.sort(Comparator.naturalOrder());

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
