/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.app.services;

/**
 * Describes a service available to application components. This doesn't
 * support duplicate services. Probably, DesignerRoot can be split into
 * a collection of services: the logger, the message channels, the global
 * state, etc.
 *
 * TODO add shutdown hooks
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
        return type.toString();
    }
}