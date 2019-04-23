/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app.services;

/**
 * Describes a service available to application components. If several
 * services are registered for the same descriptor, the last one is kept
 * (service registering events are logged).
 *
 * Some services are registered directly by DesignerImpl, others are
 * implemented in controls and registered while FXML is loading.
 *
 * @author Clément Fournier
 */
public final class AppServiceDescriptor<T> {

    private final Class<? super T> type;

    public AppServiceDescriptor(Class<? super T> type) {
        this.type = type;
    }

    public Class<? super T> getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.getSimpleName();
    }
}
