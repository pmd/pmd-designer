package net.sourceforge.pmd.util.fxdesigner.util.codearea;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import org.fxmisc.richtext.model.TwoDimensional.Position;

import net.sourceforge.pmd.lang.ast.Node;


/**
 * Maps PMD's (line, column) coordinate system to and from the code
 * area's one-dimensional (absolute offset-based) system.
 *
 * @author Clément Fournier
 */
public final class PmdCoordinatesSystem {
    private static final Pattern TAB_INDENT = Pattern.compile("^(\t*).*$");


    private PmdCoordinatesSystem() {

    }


    /**
     * Inverse of {@link #getOffsetFromPmdPosition(CodeArea, int, int)}. Converts an absolute offset
     * obtained from the given code area into the line and column a PMD parser would have assigned to
     * it.
     */
    public static LineRelativeCoordinates getPmdLineAndColumnFromOffset(CodeArea codeArea, int absoluteOffset) {

        Position pos = codeArea.offsetToPosition(absoluteOffset, Bias.Backward);
        int indentationOffset = indentationOffset(codeArea, pos.getMajor());

        return new LineRelativeCoordinates(pos.getMajor() + 1, pos.getMinor() + indentationOffset);
    }


    /**
     * Returns the absolute offset of the given pair (line, column) as computed by
     * a PMD parser in the code area.
     *
     * CodeArea counts a tab as 1 column width but displays it as 8 columns width.
     * PMD counts it correctly as 8 columns, so the position must be offset.
     *
     * Also, PMD lines start at 1 but paragraph nums start at 0 in the code area.
     */
    public static int getOffsetFromPmdPosition(CodeArea codeArea, int line, int column) {
        return codeArea.getAbsolutePosition(line - 1, column) - indentationOffset(codeArea, line - 1);
    }


    private static int indentationOffset(CodeArea codeArea, int paragraph) {
        Paragraph<Collection<String>, String, Collection<String>> p = codeArea.getParagraph(paragraph);
        Matcher m = TAB_INDENT.matcher(p.getText());
        if (m.matches()) {
            return m.group(1).length() * 7;
        }
        return 0;
    }


    /**
     * Locates the innermost node in the given [root] that contains the
     * position at [textOffset] in the [codeArea].
     */
    public static Optional<Node> findNodeAt(Node root, LineRelativeCoordinates target) {
        return contains(root, target) ? findNodeRec(root, target) : Optional.empty();
    }


    private static Optional<Node> findNodeRec(Node subject, LineRelativeCoordinates target) {
        // This is a simple divide and conquer algo
        // Like UniformStyleCollection, we assume that the text bounds of a node
        // contains the bounds of any of its descendants.
        // Then we only have to explore one node at each level of the tree,
        // and we quickly hit the bottom.

        for (int i = 0; i < subject.jjtGetNumChildren(); i++) {
            Node child = subject.jjtGetChild(i);
            if (contains(child, target)) {
                return findNodeRec(child, target);
            }
        }

        return Optional.of(subject);
    }


    /**
     * Returns true if the given node contains the position.
     */
    public static boolean contains(Node node, LineRelativeCoordinates pos) {

        if (pos.line < node.getBeginLine() || pos.line > node.getEndLine()) {
            return false;
        }

        if (node.getBeginLine() == node.getEndLine()) {
            return pos.column >= node.getBeginColumn() && pos.column < node.getEndColumn();
        }

        if (pos.line == node.getBeginLine()) {
            return pos.column >= node.getBeginColumn();
        }

        if (pos.line == node.getEndLine()) {
            return pos.column <= node.getEndColumn();
        }

        return true;
    }


    /**
     * {@link Position} keeps a reference to the codearea we don't need.
     *
     * @author Clément Fournier
     */
    public static final class LineRelativeCoordinates {

        public final int line;
        public final int column;


        public LineRelativeCoordinates(int line, int column) {
            this.line = line;
            this.column = column;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            LineRelativeCoordinates that = (LineRelativeCoordinates) o;
            return line == that.line &&
                column == that.column;
        }


        @Override
        public int hashCode() {
            return Objects.hash(line, column);
        }
    }
}
