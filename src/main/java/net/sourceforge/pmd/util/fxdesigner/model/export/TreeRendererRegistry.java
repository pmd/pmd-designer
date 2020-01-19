/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.export;

import java.util.HashMap;
import java.util.Map;

import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.util.TextTreeRenderer;
import net.sourceforge.pmd.util.treeexport.TreeRenderers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class TreeRendererRegistry implements ApplicationComponent {

    private final DesignerRoot root;

    private Map<String, LiveTreeRenderer> registry = new HashMap<>();

    public TreeRendererRegistry(DesignerRoot root) {
        this.root = root;

        TreeRenderers.register(TextTreeRenderer.DESCRIPTOR);

        TreeRenderers.registeredRenderers().forEach(it -> registry.put(it.id(), new LiveTreeRenderer(root, it)));
    }

    @Override
    public DesignerRoot getDesignerRoot() {
        return root;
    }

    public Map<String, LiveTreeRenderer> getRegistry() {
        return registry;
    }

    public ObservableList<LiveTreeRenderer> getRenderers() {
        return FXCollections.observableArrayList(registry.values());
    }

    public LiveTreeRenderer fromId(String name) {
        return registry.get(name);
    }
}
