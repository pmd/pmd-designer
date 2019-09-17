/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.codearea;

import static java.lang.Integer.parseInt;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static net.sourceforge.pmd.util.fxdesigner.util.AstTraversalUtil.parentIterator;
import static net.sourceforge.pmd.util.fxdesigner.util.DesignerIteratorUtil.toIterable;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.TwoDimensional.Bias;
import org.fxmisc.richtext.model.TwoDimensional.Position;

import net.sourceforge.pmd.lang.ast.Node;


/**
 * Maps PMD's (line, column) coordinate system to and from the code
 * area's one-dimensional (absolute offset-based) system.
 *
 * @author Clément Fournier
 * @since 6.13.0
 */
public final class PmdCoordinatesSystem {


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

        Position pos = codeArea.offsetToPosition(absoluteOffset, Bias.Forward);

        return new TextPos2D(getPmdLineFromRtfxParIndex(pos.getMajor()),
                             getPmdColumnIndexFromRtfxColumn(codeArea, pos.getMajor(), pos.getMinor()));
    }


    /**
     * Returns the absolute offset of the given pair (line, column) as computed by
     * a PMD parser in the code area.
     *
     * CodeArea counts a tab as 1 column width but displays it as 8 columns width.
     * PMD counts it correctly as 8 columns, so the position must be offset.
     *
     * Also, PMD lines start at 1 but paragraph nums start at 0 in the code area,
     * same for columns.
     */
    public static int getOffsetFromPmdPosition(CodeArea codeArea, int line, int column) {
        int parIdx = getRtfxParIndexFromPmdLine(line);
        int raw = codeArea.getAbsolutePosition(parIdx, getRtfxColumnIndexFromPmdColumn(codeArea, parIdx, column));
        return clip(raw, 0, codeArea.getLength() - 1);
    }


    private static int getRtfxColumnIndexFromPmdColumn(CodeArea codeArea, int parIdx, int column) {
        String parTxt = codeArea.getParagraph(parIdx).getText();
        int end = column - 1;
        for (int i = 0; i < end && end > 0; i++) {
            char c = parTxt.charAt(i);
            if (c == '\t') {
                end = max(end - 7, 0);
            }
        }
        return end;
    }

    private static int getPmdColumnIndexFromRtfxColumn(CodeArea codeArea, int parIdx, int rtfxCol) {
        String parTxt = codeArea.getParagraph(parIdx).getText();
        int mapped = rtfxCol;
        for (int i = 0; i < rtfxCol && i < parTxt.length(); i++) {
            char c = parTxt.charAt(i);
            if (c == '\t') {
                mapped += 7;
            }
        }
        return mapped + 1;
    }


    private static int clip(int val, int min, int max) {
        return max(min, min(val, max));
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
     * Returns the innermost node that covers the entire given text range
     * in the given tree.
     *
     * @param root  Root of the tree
     * @param range Range to find
     * @param exact If true, will return the *outermost* node whose range
     *              is *exactly* the given text range, otherwise it may be larger.
     */
    public static Optional<Node> findNodeCovering(Node root, TextRange range, boolean exact) {
        return findNodeAt(root, range.startPos).map(innermost -> {
            for (Node parent : toIterable(parentIterator(innermost, true))) {
                TextRange parentRange = rangeOf(parent);
                if (!exact && parentRange.contains(range)) {
                    return parent;
                } else if (exact && parentRange.equals(range)) {
                    // previously this used node streams to get the highest node
                    // on a single child path
                    return parent;
                } else if (exact && parentRange.contains(range)) {
                    // if it isn't the same, then we can't find better so better stop looking
                    return null;
                }
            }
            return null;
        });
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

    public static TextRange rangeOf(Node node) {
        return new TextRange(startPosition(node), endPosition(node));
    }

    /**
     * Returns a {@link TextPos2D} that uses its coordinates as begin
     * and end offset of the [node] in the [area].
     */
    public static TextPos2D rtfxRangeOf(Node node, CodeArea area) {
        return new TextPos2D(
            getOffsetFromPmdPosition(area, node.getBeginLine(), node.getBeginColumn()),
            getOffsetFromPmdPosition(area, node.getEndLine(), node.getEndColumn())
        );
    }

    public static final class TextRange implements Serializable {

        public final TextPos2D startPos;
        public final TextPos2D endPos;


        public TextRange(TextPos2D startPos, TextPos2D endPos) {
            this.startPos = startPos;
            this.endPos = endPos;
        }

        public boolean contains(TextRange range) {
            return startPos.compareTo(range.startPos) <= 0 && endPos.compareTo(range.endPos) >= 0;
        }

        public boolean contains(TextPos2D pos) {
            return startPos.compareTo(pos) <= 0 && endPos.compareTo(pos) >= 0;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TextRange textRange = (TextRange) o;
            return startPos.equals(textRange.startPos)
                && endPos.equals(textRange.endPos);
        }

        @Override
        public int hashCode() {
            return Objects.hash(startPos, endPos);
        }

        @Override
        public String toString() {
            return "[" + startPos + " - " + endPos + ']';
        }

        public static TextRange fullLine(int line, int lineLength) {
            return new TextRange(new TextPos2D(line, 0), new TextPos2D(line, lineLength));
        }

        /** Compatible with {@link #toString()} */
        public static TextRange fromString(String str) {
            String[] split = str.split("-");
            return new TextRange(TextPos2D.fromString(split[0]), TextPos2D.fromString(split[1]));
        }
    }


    /**
     * {@link Position} keeps a reference to the codearea we don't need.
     *
     * @author Clément Fournier
     */
    public static final class TextPos2D implements Comparable<TextPos2D>, Serializable {

        public static final Comparator<TextPos2D> COMPARATOR =
            Comparator.<TextPos2D>comparingInt(o -> o.line).thenComparing(o -> o.column);
        public final int line;
        public final int column;


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
        public String toString() {
            return "(" + line + ", " + column + ')';
        }

        @Override
        public int compareTo(TextPos2D o) {
            return COMPARATOR.compare(this, o);
        }

        /** Compatible with {@link #toString()} */
        public static TextPos2D fromString(String str) {
            String[] split = str.replaceAll("[^,\\d]", "").split(",");
            return new TextPos2D(parseInt(split[0]), parseInt(split[1]));
        }
    }
}
