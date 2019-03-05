package net.sourceforge.pmd.util.fxdesigner.util.codearea;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.sourceforge.pmd.util.fxdesigner.util.codearea.HighlightLayerCodeArea.LayerId;

/**
 * @author Clément Fournier
 */
class HighlightUpdate {

    final LayerId layerId;
    final boolean resetLayer;
    final Map<Set<String>, UniformStyleCollection> styleToCollection;

    private HighlightUpdate(LayerId layerId, boolean resetLayer, Map<Set<String>, UniformStyleCollection> change) {
        this.layerId = layerId;
        this.resetLayer = resetLayer;
        this.styleToCollection = change;
    }

    public static HighlightUpdate merge(HighlightUpdate first, HighlightUpdate next) {
        if (next.resetLayer) {
            return next;
        }

        Map<Set<String>, UniformStyleCollection> mergedChanges = new HashMap<>(first.styleToCollection);

        for (Entry<Set<String>, UniformStyleCollection> entry : next.styleToCollection.entrySet()) {
            mergedChanges.put(entry.getKey(), entry.getValue());
        }

        return new HighlightUpdate(first.layerId, first.resetLayer, mergedChanges);
    }

    static class CompositeHighlightUpdate {

        private final Map<LayerId, HighlightUpdate> aggregateChanges;


        private CompositeHighlightUpdate(Map<LayerId, HighlightUpdate> aggregateChanges) {
            this.aggregateChanges = aggregateChanges;
        }

        public CompositeHighlightUpdate(LayerId layerId, boolean resetLayer,
                                        Map<Set<String>, UniformStyleCollection> change) {

            this(Collections.singletonMap(layerId, new HighlightUpdate(layerId, resetLayer, change)));
        }

        public CompositeHighlightUpdate(LayerId layerId, boolean resetLayer,
                                        UniformStyleCollection change) {
            this(layerId, resetLayer, Collections.singletonMap(change.getStyle(), change));
        }

        public void apply(Map<? extends LayerId, StyleLayer> layersById) {

            for (Entry<LayerId, HighlightUpdate> updateEntry : aggregateChanges.entrySet()) {
                layersById.get(updateEntry.getKey()).accept(updateEntry.getValue());
            }

        }

        public static CompositeHighlightUpdate merge(CompositeHighlightUpdate first, CompositeHighlightUpdate next) {


            Map<LayerId, HighlightUpdate> aggregateChanges = new HashMap<>(first.aggregateChanges);

            for (Entry<LayerId, HighlightUpdate> updateEntry : next.aggregateChanges.entrySet()) {

                HighlightUpdate fstUpdate = aggregateChanges.get(updateEntry.getKey());

                if (fstUpdate == null) {
                    aggregateChanges.put(updateEntry.getKey(), updateEntry.getValue());
                } else {
                    aggregateChanges.put(updateEntry.getKey(),
                                         HighlightUpdate.merge(fstUpdate, updateEntry.getValue()));
                }
            }

            return new CompositeHighlightUpdate(aggregateChanges);
        }
    }
}
