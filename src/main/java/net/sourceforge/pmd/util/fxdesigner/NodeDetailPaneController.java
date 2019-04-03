/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.reactfx.EventStreams;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.xpath.Attribute;
import net.sourceforge.pmd.util.fxdesigner.app.AbstractController;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.NodeSelectionSource;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;

/**
 * The "Attributes" pane.
 *
 * @author Clément Fournier
 */
public class NodeDetailPaneController extends AbstractController implements NodeSelectionSource {

    /** List of attribute names that are ignored if {@link #isHideCommonAttributes()} is true. */
    private static final List<String> IGNORABLE_ATTRIBUTES =
        Arrays.asList("BeginLine", "EndLine", "BeginColumn", "EndColumn", "FindBoundary", "SingleLine");

    @FXML
    private ToggleButton hideCommonAttributesToggle;
    @FXML
    private ListView<String> xpathAttributesListView;


    protected NodeDetailPaneController(DesignerRoot root) {
        super(root);
    }

    @Override
    protected void beforeParentInit() {
        xpathAttributesListView.setPlaceholder(new Label("No available attributes"));

        Val<Node> currentSelection = initNodeSelectionHandling(getDesignerRoot(), EventStreams.never(), false);

        hideCommonAttributesProperty()
            .values()
            .distinct()
            .subscribe(show -> setFocusNode(currentSelection.getValue(), Collections.emptySet()));

    }

    @Override
    public void setFocusNode(Node node, Set<SelectionOption> options) {
        xpathAttributesListView.setItems(getAttributes(node));
    }

    /**
     * Gets the XPath attributes of the node for display within a listview.
     */
    private ObservableList<String> getAttributes(Node node) {
        if (node == null) {
            return FXCollections.emptyObservableList();
        }

        ObservableList<String> result = FXCollections.observableArrayList();
        Iterator<Attribute> attributeAxisIterator = node.getXPathAttributesIterator();
        while (attributeAxisIterator.hasNext()) {
            Attribute attribute = attributeAxisIterator.next();

            if (!(isHideCommonAttributes() && IGNORABLE_ATTRIBUTES.contains(attribute.getName()))) {
                // TODO the display should be handled in a ListCell
                result.add(attribute.getName() + " = "
                               + ((attribute.getValue() != null) ? attribute.getStringValue() : "null"));
            }
        }

        DesignerUtil.getResolvedType(node).map(t -> "typeIs() = " + t).ifPresent(result::add);

        Collections.sort(result);
        return result;
    }


    @PersistentProperty
    public boolean isHideCommonAttributes() {
        return hideCommonAttributesToggle.isSelected();
    }


    public void setHideCommonAttributes(boolean bool) {
        hideCommonAttributesToggle.setSelected(bool);
    }


    public Var<Boolean> hideCommonAttributesProperty() {
        return Var.fromVal(hideCommonAttributesToggle.selectedProperty(), hideCommonAttributesToggle::setSelected);
    }


}
