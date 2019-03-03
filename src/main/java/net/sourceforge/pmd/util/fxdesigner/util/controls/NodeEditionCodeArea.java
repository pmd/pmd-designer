/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.findNodeAt;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.getPmdLineAndColumnFromOffset;
import static net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.getRtfxParIndexFromPmdLine;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.event.MouseOverTextEvent;
import org.reactfx.EventSource;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.Language;
import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.symboltable.NameOccurrence;
import net.sourceforge.pmd.lang.symboltable.ScopedNode;
import net.sourceforge.pmd.util.fxdesigner.SourceEditorController;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.NodeSelectionSource;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.AvailableSyntaxHighlighters;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.HighlightLayerCodeArea;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextPos2D;
import net.sourceforge.pmd.util.fxdesigner.util.controls.NodeEditionCodeArea.StyleLayerIds;
import net.sourceforge.pmd.util.fxdesigner.util.datakeys.DataHolderUtil;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import javafx.application.Platform;
import javafx.beans.NamedArg;
import javafx.css.PseudoClass;


/**
 * A layered code area made to display nodes. Handles the presentation of nodes in place of {@link SourceEditorController}.
 *
 * <p>This type of area has a special "node selection mode", in which you can select any node by
 * hovering the mouse above its text.
 *
 * @since 6.12.0
 * @author Clément Fournier
 */
public class NodeEditionCodeArea extends HighlightLayerCodeArea<StyleLayerIds> implements NodeSelectionSource {

    /**
     * Minimum duration during which the CTRL key must be continually pressed before the code area
     * toggles node selection mode.
     */
    private static final Duration CTRL_SELECTION_VETO_PERIOD = Duration.ofMillis(1000);

    /**
     * Minimum hover duration to select a node.
     */
    private static final Duration NODE_SELECTION_HOVER_DELAY = Duration.ofMillis(100);

    private final Var<Node> currentFocusNode = Var.newSimpleVar(null);
    private final Var<List<Node>> currentRuleResults = Var.newSimpleVar(Collections.emptyList());
    private final Var<List<Node>> currentErrorNodes = Var.newSimpleVar(Collections.emptyList());
    private final Var<List<NameOccurrence>> currentNameOccurrences = Var.newSimpleVar(Collections.emptyList());
    private final DesignerRoot designerRoot;
    private final EventSource<Node> selectionEvts = new EventSource<>();



    /** Only provided for scenebuilder, not used at runtime. */
    public NodeEditionCodeArea() {
        super(StyleLayerIds.class);
        designerRoot = null;
    }

    public NodeEditionCodeArea(@NamedArg("designerRoot") DesignerRoot root) {
        super(StyleLayerIds.class);

        this.designerRoot = root;

        setParagraphGraphicFactory(lineNumberFactory());

        currentRuleResultsProperty().values().subscribe(this::highlightXPathResults);
        currentErrorNodesProperty().values().subscribe(this::highlightErrorNodes);
        currentNameOccurrences.values().subscribe(this::highlightNameOccurrences);

        initNodeSelectionHandling(designerRoot, selectionEvts, true);

        enableCtrlSelection();
    }


    /**
     * TODO does this need to be disableable? Maybe some keyboards use the CTRL key in ways I don't
     */
    private void enableCtrlSelection() {

        final Val<Boolean> isNodeSelectionMode =
            ReactfxUtil.vetoableYes(getDesignerRoot().isCtrlDownProperty(), CTRL_SELECTION_VETO_PERIOD);

        addEventHandler(
            MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN,
            ev -> {
                if (!isNodeSelectionMode.getValue()) {
                    return;
                }
                Node currentRoot = getDesignerRoot().currentCompilationUnitProperty().getValue();
                if (currentRoot == null) {
                    return;
                }

                TextPos2D target = getPmdLineAndColumnFromOffset(this, ev.getCharacterIndex());

                findNodeAt(currentRoot, target).ifPresent(selectionEvts::push);
            }
        );


        isNodeSelectionMode.values().distinct().subscribe(isSelectionMode -> {
            pseudoClassStateChanged(PseudoClass.getPseudoClass("is-node-selection"), isSelectionMode);
            setMouseOverTextDelay(isSelectionMode ? NODE_SELECTION_HOVER_DELAY : null);
        });
    }

    /** Scroll the editor to a node and makes it visible. */
    private void scrollToNode(Node node) {

        moveTo(node.getBeginLine() - 1, 0);

        if (getVisibleParagraphs().size() < 1) {
            return;
        }

        int visibleLength = lastVisibleParToAllParIndex() - firstVisibleParToAllParIndex();

        boolean fitsViewPort = node.getEndLine() - node.getBeginLine() <= visibleLength;
        boolean isStartVisible =
            getRtfxParIndexFromPmdLine(node.getBeginLine()) >= firstVisibleParToAllParIndex();
        boolean isEndVisible =
            getRtfxParIndexFromPmdLine(node.getEndLine()) <= lastVisibleParToAllParIndex();

        if (fitsViewPort) {
            if (!isStartVisible) {
                showParagraphAtTop(Math.max(node.getBeginLine() - 2, 0));
            }
            if (!isEndVisible) {
                showParagraphAtBottom(Math.min(node.getEndLine(), getParagraphs().size()));
            }
        } else if (!isStartVisible) {
            showParagraphAtTop(Math.max(node.getBeginLine() - 2, 0));
        }
    }


    private IntFunction<javafx.scene.Node> lineNumberFactory() {
        IntFunction<javafx.scene.Node> base = LineNumberFactory.get(this);
        Val<Integer> activePar = Val.wrap(currentParagraphProperty());

        return idx -> {

            javafx.scene.Node label = base.apply(idx);

            activePar.conditionOnShowing(label)
                     .values()
                     .subscribe(p -> label.pseudoClassStateChanged(PseudoClass.getPseudoClass("has-caret"), idx == p));

            // adds a pseudo class if part of the focus node appears on this line
            currentFocusNode.conditionOnShowing(label)
                            .values()
                            .subscribe(n -> label.pseudoClassStateChanged(PseudoClass.getPseudoClass("is-focus-node"),
                                                                          n != null && idx + 1 <= n.getEndLine() && idx + 1 >= n.getBeginLine()));

            return label;
        };
    }


    public final Var<List<Node>> currentRuleResultsProperty() {
        return currentRuleResults;
    }


    public final Var<List<Node>> currentErrorNodesProperty() {
        return currentErrorNodes;
    }


    public Var<List<NameOccurrence>> currentNameOccurrencesProperty() {
        return currentNameOccurrences;
    }


    /** Highlights xpath results (xpath highlight). */
    private void highlightXPathResults(Collection<? extends Node> nodes) {
        styleNodes(nodes, StyleLayerIds.XPATH_RESULT, true);
    }


    /** Highlights name occurrences (secondary highlight). */
    private void highlightNameOccurrences(Collection<? extends NameOccurrence> occs) {
        styleNodes(occs.stream().map(NameOccurrence::getLocation).collect(Collectors.toList()), StyleLayerIds.NAME_OCCURRENCE, true);
    }


    /** Highlights nodes that are in error (secondary highlight). */
    private void highlightErrorNodes(Collection<? extends Node> nodes) {
        styleNodes(nodes, StyleLayerIds.ERROR, true);
        if (!nodes.isEmpty()) {
            scrollToNode(nodes.iterator().next());
        }
    }


    /** Moves the caret to a position and makes the view follow it. */
    public void moveCaret(int line, int column) {
        moveTo(line, column);
        requestFollowCaret();
    }


    @Override
    public void setFocusNode(Node node) {


        // editor is always scrolled when re-selecting a node
        if (node != null && DataHolderUtil.getUserData(SHOULD_MOVE_CARET, node)) {
            scrollToNode(node);
        }

        if (node != null) {
            // reset
            DataHolderUtil.putUserData(SHOULD_MOVE_CARET, true, node);
        }

        if (Objects.equals(node, currentFocusNode.getValue())) {
            return;
        }

        currentFocusNode.setValue(node);

        // editor is only restyled if the selection has changed
        Platform.runLater(() -> styleNodes(node == null ? emptyList() : singleton(node), StyleLayerIds.FOCUS, true));

        if (node instanceof ScopedNode) {
            // not null as well
            Platform.runLater(() -> highlightNameOccurrences(DesignerUtil.getNameOccurrences((ScopedNode) node)));
        }
    }


    @Override
    public DesignerRoot getDesignerRoot() {
        return designerRoot;
    }


    public void updateSyntaxHighlighter(Language language) {
        setSyntaxHighlighter(AvailableSyntaxHighlighters.getHighlighterForLanguage(language).orElse(null));
    }


    /** Style layers for the code area. */
    enum StyleLayerIds implements HighlightLayerCodeArea.LayerId {
        // caution, the name of the constants are used as style classes

        /** For the currently selected node. */
        FOCUS,
        /** For declaration usages. */
        NAME_OCCURRENCE,
        /** For nodes in error. */
        ERROR,
        /** For xpath results. */
        XPATH_RESULT;

        private final String styleClass; // the id will be used as a style class


        StyleLayerIds() {
            this.styleClass = name().toLowerCase(Locale.ROOT).replace('_', '-') + "-highlight";
        }


        /** focus-highlight, xpath-result-highlight, error-highlight, name-occurrence-highlight */
        @Override
        public String getStyleClass() {
            return styleClass;
        }

    }

}
