package net.sourceforge.pmd.util.fxdesigner.util.controls;

import static javafx.geometry.Pos.CENTER;

import java.util.List;
import java.util.Objects;

import org.kordamp.ikonli.javafx.FontIcon;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * Placeholder for an empty control. Can suggest actions to take to make the area non-empty.
 *
 * @author Clément Fournier
 */
public class HelpfulPlaceholder extends HBox {

    private final Var<String> message = Var.newSimpleVar("Placeholder");

    public HelpfulPlaceholder(String initialMessage,
                              FontIcon leftColumn,
                              List<Hyperlink> actions) {

        getStyleClass().addAll("helpful-placeholder");

        this.message.setValue(initialMessage);

        VBox messageVBox = new VBox();

        Text messageLabel = new Text();
        messageLabel.textProperty().bind(message);

        Val<Number> messageWidthProp = Val.wrap(
            leftColumn == null
            ? Val.wrap(widthProperty())
            : Bindings.createDoubleBinding(() -> getWidth() - messageVBox.getPrefWidth(),
                                           messageVBox.prefWidthProperty(),
                                           widthProperty()
            )
        );

        messageLabel.wrappingWidthProperty().bind(messageVBox.widthProperty());

        messageVBox.getChildren().addAll(messageLabel);
        messageVBox.getChildren().addAll(actions);


        if (leftColumn != null) {
            getChildren().add(leftColumn);
        }

        getChildren().add(messageVBox);

        // TODO move to css
        setSpacing(10);
        setAlignment(CENTER);
        messageVBox.setAlignment(CENTER);
    }

    public Var<String> messageProperty() {
        return message;
    }

    public static PlaceHolderBuilder withMessage(String message) {
        return new PlaceHolderBuilder().withMessage(message);
    }

    public static class PlaceHolderBuilder {


        private String myMessage = "This looks empty...";
        private ObservableList<Hyperlink> myActions = FXCollections.observableArrayList();
        private FontIcon leftColumn;

        public PlaceHolderBuilder withMessage(String message) {
            myMessage = message;
            return this;
        }

        public PlaceHolderBuilder withSuggestedAction(String message, Runnable action) {
            Hyperlink hyperlink = new Hyperlink(message);
            hyperlink.setOnAction(e -> action.run());
            return this;
        }

        public PlaceHolderBuilder withLeftColumn(FontIcon leftColumn) {
            this.leftColumn = leftColumn;
            return this;
        }

        public HelpfulPlaceholder build() {
            Objects.requireNonNull(myMessage);
            return new HelpfulPlaceholder(myMessage, leftColumn, myActions);
        }
    }
}
