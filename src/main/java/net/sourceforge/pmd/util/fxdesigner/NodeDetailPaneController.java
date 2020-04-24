/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.reactfx.EventStreams;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.xpath.Attribute;
import net.sourceforge.pmd.util.designerbindings.DesignerBindings;
import net.sourceforge.pmd.util.designerbindings.DesignerBindings.AdditionalInfo;
import net.sourceforge.pmd.util.designerbindings.DesignerBindings.DefaultDesignerBindings;
import net.sourceforge.pmd.util.fxdesigner.app.AbstractController;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.app.NodeSelectionSource;
import net.sourceforge.pmd.util.fxdesigner.util.DataHolder;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.beans.SettingsPersistenceUtil.PersistentProperty;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;

/**
 * The "Attributes" pane.
 *
 * @author Clément Fournier
 */
public class NodeDetailPaneController extends AbstractController implements NodeSelectionSource {

    /**
     * List of attribute names that are ignored if {@link #isHideCommonAttributes()} is true.
     */
    private static final List<String> IGNORABLE_ATTRIBUTES =
        Arrays.asList("BeginLine", "EndLine", "BeginColumn", "EndColumn", "FindBoundary", "SingleLine");

    @FXML
    private TableView<Attribute> xpathAttributesTreeView;
    @FXML
    private TableColumn<Attribute, String> attrValueColumn;
    @FXML
    private TableColumn<Attribute, String> attrNameColumn;
    @FXML
    private ToggleButton hideCommonAttributesToggle;
    @FXML
    private ListView<String> additionalInfoListView;


    protected NodeDetailPaneController(DesignerRoot root) {
        super(root);
    }

    @Override
    protected void beforeParentInit() {
        additionalInfoListView.setPlaceholder(new Label("No additional info"));

        Val<Node> currentSelection = initNodeSelectionHandling(getDesignerRoot(), EventStreams.never(), false);

        // pin to see updates
        currentSelection.pin();

        hideCommonAttributesProperty()
            .values()
            .distinct()
            .subscribe(show -> setFocusNode(currentSelection.getValue(), new DataHolder()));


        attrValueColumn.setCellValueFactory(param -> Val.constant(DesignerUtil.attrToXpathString(param.getValue())));
        attrNameColumn.setCellValueFactory(param -> Val.constant("@" + param.getValue().getName()));
    }

    @Override
    public void setFocusNode(final Node node, DataHolder options) {
        xpathAttributesTreeView.setItems(getAttributes(node));
        if (node == null) {
            additionalInfoListView.setItems(FXCollections.emptyObservableList());
            return;
        }
        DesignerBindings bindings = languageBindingsProperty().getOrElse(DefaultDesignerBindings.getInstance());
        ObservableList<AdditionalInfo> additionalInfo = FXCollections.observableArrayList(bindings.getAdditionalInfo(node));
        additionalInfo.sort(Comparator.comparing(AdditionalInfo::getSortKey));
        additionalInfoListView.setItems(LiveList.map(additionalInfo, AdditionalInfo::getDisplayString));
    }

    /**
     * Gets the XPath attributes of the node for display within a listview.
     */
    private ObservableList<Attribute> getAttributes(Node node) {
        if (node == null) {
            xpathAttributesTreeView.setPlaceholder(new Label("Select a node to show its attributes"));
            return FXCollections.emptyObservableList();
        }

        ObservableList<Attribute> result = FXCollections.observableArrayList();
        Iterator<Attribute> attributeAxisIterator = node.getXPathAttributesIterator();
        while (attributeAxisIterator.hasNext()) {
            Attribute attribute = attributeAxisIterator.next();

            if (!(isHideCommonAttributes() && IGNORABLE_ATTRIBUTES.contains(attribute.getName()))) {

                try {
                    // TODO the display should be handled in a ListCell
                    result.add(attribute);
                } catch (Exception ignored) {
                    // some attributes throw eg numberformat exceptions
                }

            }
        }

        xpathAttributesTreeView.setPlaceholder(new Label("No available attributes"));
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
