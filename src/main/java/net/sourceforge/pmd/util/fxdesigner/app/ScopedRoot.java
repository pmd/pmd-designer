/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app;

import java.util.HashMap;
import java.util.Map;

import org.reactfx.value.Val;

import net.sourceforge.pmd.util.fxdesigner.app.services.AppServiceDescriptor;
import net.sourceforge.pmd.util.fxdesigner.app.services.CloseableService;
import net.sourceforge.pmd.util.fxdesigner.app.services.LogEntry;

import javafx.stage.Stage;


/**
 * Interface for the singleton of the app.
 *
 * @author Clément Fournier
 * @since 6.0.0
 */
public final class ScopedRoot implements DesignerRoot {


    private final Map<AppServiceDescriptor<?>, Object> services = new HashMap<>();
    private final DesignerRoot parent;


    public ScopedRoot(DesignerRoot parent) {
        this.parent = parent;
    }


    @Override
    public Stage getMainStage() {
        return parent.getMainStage();
    }


    @Override
    public boolean isDeveloperMode() {
        return parent.isDeveloperMode();
    }


    @Override
    @SuppressWarnings("unchecked")
    public <T> T getService(AppServiceDescriptor<T> descriptor) {
        T t = (T) services.get(descriptor);
        return t == null ? parent.getService(descriptor) : t;
    }

    @Override
    public <T> void registerService(AppServiceDescriptor<T> descriptor, T component) {
        if (getService(LOGGER) != null) {
            // event the logger needs to be registered hehe
            getService(LOGGER).logEvent(LogEntry.serviceRegistered(descriptor, component));
        }
        services.put(descriptor, component);
    }

    @Override
    public Val<Boolean> isCtrlDownProperty() {
        return parent.isCtrlDownProperty();
    }

    @Override
    public void shutdownServices() {
        services.forEach((descriptor, component) -> {
            if (component instanceof CloseableService) {
                try {
                    ((CloseableService) component).close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
