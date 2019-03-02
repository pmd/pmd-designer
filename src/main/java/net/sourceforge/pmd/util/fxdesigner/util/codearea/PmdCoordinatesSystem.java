/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.codearea;

import java.util.Collection;
import java.util.Comparator;
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
 * @since 6.13.0
 * @author Clément Fournier
 */
public final class PmdCoordinatesSystem {
    private static final Pattern TAB_INDENT = Pattern.compile("^(\t*).*$");


    private PmdCoordinatesSystem() {

    }

    public static int getRtfxParIndexFromPmdLine(int line) {
        return line - 1;
    }

    public static int getPmdLineFromRtfxParIndex(int line) {
        return line + 1;
    }

    /**
     * Inverse of {@link #getOffsetFromPmdPosition(CodeArea, int, int)}. Converts an absolute offset
     * obtained from the given code area into the line and column a PMD parser would have assigned to
     * it.
     */
    public static TextPos2D getPmdLineAndColumnFromOffset(CodeArea codeArea, int absoluteOffset) {

        Position pos = codeArea.offsetToPosition(absoluteOffset, Bias.Backward);
        int indentationOffset = indentationOffset(codeArea, pos.getMajor());

        return new TextPos2D(getPmdLineFromRtfxParIndex(pos.getMajor()),
                             pos.getMinor() + indentationOffset + 1);
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
        return codeArea.getAbsolutePosition(getRtfxParIndexFromPmdLine(line), column)
            - indentationOffset(codeArea, line - 1);
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
    public static Optional<Node> findNodeAt(Node root, TextPos2D target) {
        return Optional.ofNullable(findNodeRec(root, target)).filter(it -> contains(it, target));
    }


    /**
     * Simple recursive search algo. Makes the same assumptions about text bounds
     * as {@link UniformStyleCollection#toSpans()}. Then:
     * - We only have to explore one node at each level of the tree, and we quickly
     * hit the bottom (average depth of a Java AST ~20-25, with 6.x.x grammar).
     * - At each level, the next node to explore is chosen via binary search.
     */
    private static Node findNodeRec(Node subject, TextPos2D target) {
        Node child = binarySearchInChildren(subject, target);
        return child == null ? subject : findNodeRec(child, target);
    }

    // returns the child of the [parent] that contains the target
    // it's assumed to be unique
    private static Node binarySearchInChildren(Node parent, TextPos2D target) {

        int low = 0;
        int high = parent.jjtGetNumChildren() - 1;

        while (low <= high) {
            int mid = (low + high) / 2;
            Node child = parent.jjtGetChild(mid);
            int cmp = startPosition(child).compareTo(target);

            if (cmp < 0) {
                // node start is before target
                low = mid + 1;
                if (endPosition(child).compareTo(target) >= 0) {
                    // node end is after target
                    return child;
                }
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                // target is node start position
                return child; // key found
            }
        }
        return null;  // key not found
    }


    /**
     * Returns true if the given node contains the position.
     */
    public static boolean contains(Node node, TextPos2D pos) {
        return startPosition(node).compareTo(pos) <= 0 && endPosition(node).compareTo(pos) >= 0;
    }


    public static TextPos2D startPosition(Node node) {
        return new TextPos2D(node.getBeginLine(), node.getBeginColumn());
    }


    public static TextPos2D endPosition(Node node) {
        return new TextPos2D(node.getEndLine(), node.getEndColumn());
    }


    /**
     * {@link Position} keeps a reference to the codearea we don't need.
     *
     * @author Clément Fournier
     */
    public static final class TextPos2D implements Comparable<TextPos2D> {

        public final int line;
        public final int column;


        public static final Comparator<TextPos2D> COMPARATOR =
            Comparator.<TextPos2D>comparingInt(o -> o.line).thenComparing(o -> o.column);


        public TextPos2D(int line, int column) {
            this.line = line;
            this.column = column;
        }


        @Override
        public int hashCode() {
            return Objects.hash(line, column);
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TextPos2D that = (TextPos2D) o;
            return line == that.line
                && column == that.column;
        }


        @Override
        public int compareTo(TextPos2D o) {
            return COMPARATOR.compare(this, o);
        }
    }
}
