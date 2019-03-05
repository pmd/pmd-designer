/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.codearea;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;


/**
 * Represents a layer of styling in the text. Several layers are
 * aggregated into a {@link HighlightLayerCodeArea}, and can evolve
 * independently.
 */
class StyleLayer {

    private final Map<Set<String>, UniformStyleCollection> styleToCollection = new HashMap<>();


    /** Reset this layer to its empty state, clearing all the styles. */
    public void clearStyles() {
        styleToCollection.clear();
    }


    public Collection<UniformStyleCollection> getCollections() {
        return styleToCollection.values();
    }


    public void accept(HighlightUpdate update) {
        if (update.resetLayer) {
            clearStyles();
        }

        for (Entry<Set<String>, UniformStyleCollection> styleEntry : new HashSet<>(styleToCollection.entrySet())) {

            UniformStyleCollection updated = update.styleToCollection.get(styleEntry.getKey());

            if (updated != null) {
                styleToCollection.put(styleEntry.getKey(), styleEntry.getValue().merge(updated));
            }

            update.styleToCollection.remove(styleEntry.getKey());
        }

        styleToCollection.putAll(update.styleToCollection);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StyleLayer that = (StyleLayer) o;
        return Objects.equals(styleToCollection, that.styleToCollection);
    }


    @Override
    public int hashCode() {

        return Objects.hash(styleToCollection);
    }
}
