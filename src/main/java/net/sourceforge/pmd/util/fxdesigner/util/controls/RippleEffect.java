/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.function.Supplier;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class RippleEffect {

    private Circle circleRipple;
    private Rectangle rippleClip = new Rectangle();
    private Duration rippleDuration = Duration.millis(250);
    private double lastRippleHeight = 0;
    private double lastRippleWidth = 0;

    private EventHandler<MouseEvent> handler;

    public RippleEffect(ReadOnlyDoubleProperty containerWidth,
                        ReadOnlyDoubleProperty containerHeight,
                        Supplier<Background> containerBackground) {
        Color rippleColor = new Color(0, 0, 0, 0.11);
        circleRipple = new Circle(0.1, rippleColor);
        circleRipple.setOpacity(0.0);
        // Optional box blur on ripple - smoother ripple effect
        //circleRipple.setEffect(new BoxBlur(3, 3, 2));
        // Fade effect bit longer to show edges on the end of animation
        final FadeTransition fadeTransition = new FadeTransition(rippleDuration, circleRipple);
        fadeTransition.setInterpolator(Interpolator.EASE_OUT);
        fadeTransition.setFromValue(1.0);
        fadeTransition.setToValue(0.0);
        final Timeline scaleRippleTimeline = new Timeline();
        final SequentialTransition parallelTransition = new SequentialTransition();
        parallelTransition.getChildren().addAll(
            scaleRippleTimeline,
            fadeTransition
        );
        // When ripple transition is finished then reset circleRipple to starting point
        parallelTransition.setOnFinished(event -> {
            circleRipple.setOpacity(0.0);
            circleRipple.setRadius(0.1);
        });
        this.handler = event -> {
            parallelTransition.stop();
            // Manually fire finish event
            parallelTransition.getOnFinished().handle(null);
            circleRipple.setCenterX(event.getX());
            circleRipple.setCenterY(event.getY());
            // Recalculate ripple size if size of button from last time was changed
            if (containerWidth.get() != lastRippleWidth || containerHeight.get() != lastRippleHeight) {
                lastRippleWidth = containerWidth.get();
                lastRippleHeight = containerHeight.get();
                rippleClip.setWidth(lastRippleWidth);
                rippleClip.setHeight(lastRippleHeight);
                try {
                    rippleClip.setArcHeight(containerBackground.get().getFills().get(0).getRadii().getTopLeftHorizontalRadius());
                    rippleClip.setArcWidth(containerBackground.get().getFills().get(0).getRadii().getTopLeftHorizontalRadius());
                    circleRipple.setClip(rippleClip);
                } catch (Exception ignored) {
                    // try block because of possible null of Background, fills ...
                }
                // Getting 45% of longest button's length, because we want edge of ripple effect always visible
                double circleRippleRadius = Math.max(containerHeight.get(), containerWidth.get()) * 0.45;
                final KeyValue keyValue = new KeyValue(circleRipple.radiusProperty(), circleRippleRadius, Interpolator.EASE_OUT);
                final KeyFrame keyFrame = new KeyFrame(rippleDuration, keyValue);
                scaleRippleTimeline.getKeyFrames().clear();
                scaleRippleTimeline.getKeyFrames().add(keyFrame);
            }
            parallelTransition.playFromStart();
        };
    }

    public void setRippleColor(Color color) {
        circleRipple.setFill(color);
    }

    public static void attach(Region node, Supplier<ObservableList<Node>> getChildren) {
        RippleEffect effect = new RippleEffect(node.widthProperty(), node.heightProperty(), node::getBackground);
        // Adding circleRipple as fist node of button nodes to be on the bottom
        getChildren.get().add(0, effect.circleRipple);
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, effect.handler);
    }
}
