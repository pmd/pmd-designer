package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.controlsfx.control.PopOver;
import org.reactfx.value.Var;

import javafx.scene.Node;
import javafx.stage.Window;

/**
 * Wrapper around a popover, that remembers whether it's already shown
 * or not.
 *
 * @author Clément Fournier
 */
public final class PopOverWrapper<T> {

    private final Var<PopOver> myPopover = Var.newSimpleVar(null);
    private Supplier<PopOver> supplier;
    private T identity;

    public PopOverWrapper(T identity, Supplier<PopOver> supplier) {
        this.identity = identity;
        this.supplier = supplier;
    }

    public void showOrFocus(Consumer<PopOver> showMethod) {
        if (myPopover.isPresent() && myPopover.getValue().isShowing()) {
            myPopover.getValue().requestFocus();
        } else {
            if (supplier == null) {
                throw new IllegalStateException("Unitialized");
            }
            PopOver popOver = supplier.get();
            if (popOver == null) {
                throw new IllegalStateException("Improper supplier");
            }
            myPopover.setValue(popOver);
            showMethod.accept(popOver);
            PopOverUtil.fixStyleSheets(popOver);
        }
    }

    public void rebindIfDifferent(T identity, Supplier<PopOver> supplier) {
        if (!Objects.equals(this.identity, identity)) {
            this.identity = identity;
            this.supplier = supplier;
        }
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
}
