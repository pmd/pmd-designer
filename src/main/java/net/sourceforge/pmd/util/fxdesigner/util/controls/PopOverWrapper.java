package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.controlsfx.control.PopOver;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;

import javafx.application.Platform;

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
        } else if (myPopover.isPresent()) {
            showMethod.accept(myPopover.getValue());
        } else {
            if (supplier == null) {
                throw new IllegalStateException("Unitialized");
            }
            PopOver popOver = supplier.get();
            if (popOver == null) {
                throw new IllegalStateException("Improper supplier");
            }
            myPopover.setValue(popOver);
            popOver.getRoot().getStylesheets().addAll(DesignerUtil.getCss("popover").toString());
            showOrFocus(showMethod); // fall into the above branch
        }
    }

    public void rebindIfDifferent(T identity, Supplier<PopOver> supplier) {
        if (!Objects.equals(this.identity, identity)) {
            this.identity = identity;
            this.supplier = supplier;
            preload(supplier);
        }
    }

    private void preload(Supplier<PopOver> supplier) {
        if (supplier == null) {
            return;
        }
        PopOver popOver = supplier.get();
        if (popOver == null) {
            return;
        }
        popOver.getRoot().getStylesheets().addAll(DesignerUtil.getCss("popover").toString());
        myPopover.setValue(popOver);
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
