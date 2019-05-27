package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.controlsfx.control.PopOver;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;

import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Wrapper around a popover, that remembers whether it's already shown
 * or not.
 *
 * @author Clément Fournier
 */
public final class PopOverWrapper<T> {

    private final Var<PopOver> myPopover = Var.newSimpleVar(null);
    private BiFunction<T, PopOver, PopOver> rebinder;
    private T identity;

    public PopOverWrapper(BiFunction<T, @Nullable PopOver, @Nullable PopOver> rebinder) {
        this.identity = null;
        this.rebinder = rebinder;
    }

    public void showOrFocus(Consumer<@NonNull PopOver> showMethod) {
        if (myPopover.isPresent() && myPopover.getValue().isShowing()) {
            myPopover.getValue().requestFocus();
        } else if (myPopover.isPresent()) {
            showMethod.accept(myPopover.getValue());
        } else {
            preload(() -> rebinder.apply(identity, null));
            if (myPopover.isEmpty()) {
                System.err.println("Wrong supplier, cannot rebind popover");
            } else {
                showMethod.accept(myPopover.getValue());
            }
        }
    }

    public void rebind(T identity) {
        preload(() -> rebinder.apply(identity, myPopover.getValue()));
    }

    private void preload(Supplier<PopOver> supplier) {
        if (supplier == null) {
            return;
        }
        PopOver popOver = supplier.get();
        if (popOver == null) {
            myPopover.setValue(null);
            return;
        }
        popOver.getRoot().getStylesheets().addAll(DesignerUtil.getCss("popover").toString());
        popOver.getRoot().applyCss();
        myPopover.setValue(popOver);
    }

    /**
     * This is a weird hack to preload the FXML and CSS, so that the
     * first opening of the popover doesn't look completely broken.
     */
    public void doFirstLoad(Stage stage) {
        myPopover.ifPresent(pop -> {
            pop.getRoot().setOpacity(0);
            pop.show(stage);
            Platform.runLater(() -> {
                pop.hide();
                pop.getRoot().setOpacity(1);
            });
        });
    }

    public T getIdentity() {
        return identity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PopOverWrapper<?> that = (PopOverWrapper<?>) o;
        return Objects.equals(identity, that.identity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identity);
    }

    public void hide() {
        myPopover.getOpt().filter(Window::isShowing).ifPresent(PopOver::hide);
    }
}
