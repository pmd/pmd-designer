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
 * @author Clément Fournier
 */
public final class AppServiceDescriptor<T> {

    private final Class<T> type;

    public AppServiceDescriptor(Class<T> type) {
        this.type = type;
    }

    public Class<T> getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.toString();
    }
}
