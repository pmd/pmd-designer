package net.sourceforge.pmd.util.fxdesigner.util.controls;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.controlsfx.control.PopOver;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;

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

    public PopOverWrapper() {
        this.identity = null;
        this.rebinder = (t, f) -> null;
    }

    public void showOrFocus(Consumer<PopOver> showMethod) {
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

    public void rebindIfDifferent(T identity, BiFunction<T, PopOver, PopOver> rebinder) {
        if (!Objects.equals(this.identity, identity)) {
            this.identity = identity;
            this.rebinder = rebinder;
            preload(() -> rebinder.apply(identity, myPopover.getValue()));
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
