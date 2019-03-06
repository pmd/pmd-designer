/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.kordamp.ikonli.javafx.FontIcon;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;


/**
 * A Titled pane that has a toolbar in its header region.
 * Supported by some CSS in designer.less.
 *
 * @author Clément Fournier
 * @since 6.11.0
 */
public final class ToolbarTitledPane extends TitledPane {


    private final ToolBar toolBar = new ToolBar();
    private final Var<String> title = Var.newSimpleVar("Title");
    private final Var<String> errorMessage = Var.newSimpleVar("");

    public ToolbarTitledPane() {

        getStyleClass().add("tool-bar-title");

        // change the default
        setCollapsible(false);

        toolBar.setPadding(Insets.EMPTY);

        setGraphic(toolBar);

        // should be an empty string, binding prevents to set it
        textProperty().bind(Val.constant(""));


        Label errorLabel = buildErrorLabel();

        toolBar.getItems().add(buildTitleLabel());
        // the runlater allows adding those items after the FXMLLoader
        // added stuff in the list
        Platform.runLater(() -> toolBar.getItems().addAll(buildSpacer(), errorLabel));


        // The toolbar is too large for the title region and is not
        // centered unless we bind the height, like follows

        Val.wrap(toolBar.parentProperty())
            .values()
            .filter(Objects::nonNull)
            .subscribe(parent -> {
                // The title region is provided by the skin,
                // this is the only way to access it outside of css
                StackPane titleRegion = (StackPane) parent;

                DesignerUtil.rewire(toolBar.maxHeightProperty(), titleRegion.heightProperty());
                DesignerUtil.rewire(toolBar.minHeightProperty(), titleRegion.heightProperty());
                DesignerUtil.rewire(toolBar.prefHeightProperty(), titleRegion.heightProperty());

                // fill available width, for the spacer to be useful
                DesignerUtil.rewire(toolBar.minWidthProperty(),
                                    Val.wrap(titleRegion.widthProperty())
                                       // This "10" is the padding, I couldn't find a reliable way to
                                       // bind cleanly without hardcoding it
                                       .map(it -> it.doubleValue() - errorLabel.getPrefWidth() - 10)
                );
            });


    }

    private Label buildTitleLabel() {
        Label titleLabel = new Label("Title");
        titleLabel.textProperty().bind(title);
        titleLabel.getStyleClass().add("title-label");
        return titleLabel;
    }

    private Label buildErrorLabel() {
        Label errorLabel = new Label();

        errorLabel.getStyleClass().addAll("error-label");

        FontIcon errorIcon = new FontIcon("fas-exclamation-triangle");
        errorLabel.setGraphic(errorIcon);
        errorLabel.tooltipProperty().bind(
            errorMessage.map(message -> StringUtils.isBlank(message) ? null : new Tooltip(message))
        );
        errorLabel.visibleProperty().bind(errorMessage.map(StringUtils::isNotBlank));
        // makes the label zero-width when it's not visible
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        return errorLabel;
    }

    private Pane buildSpacer() {
        Pane pane = new Pane();
        pane.getStyleClass().addAll("spacer-pane");
        HBox.setHgrow(pane, Priority.ALWAYS);
        return pane;
    }


    public ObservableList<Node> getToolbarItems() {
        return toolBar.getItems();
    }


    public String getTitle() {
        return title.getValue();
    }


    public void setTitle(String title) {
        this.title.setValue(title);
    }


    /**
     * If non-blank, an error icon with this message as the tooltip
     * will appear.
     */
    public Var<String> errorMessageProperty() {
        return errorMessage;
    }

    /** Title of the pane, not equivalent to {@link #textProperty()}. */
    public Var<String> titleProperty() {
        return title;
    }
}
