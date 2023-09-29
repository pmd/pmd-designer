/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.export;

import java.io.IOException;
import java.util.Map;

import org.reactfx.collection.LiveArrayList;
import org.reactfx.collection.LiveList;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.properties.PropertyDescriptor;
import net.sourceforge.pmd.properties.PropertySource;
import net.sourceforge.pmd.util.fxdesigner.app.ApplicationComponent;
import net.sourceforge.pmd.util.fxdesigner.app.DesignerRoot;
import net.sourceforge.pmd.util.fxdesigner.model.PropertyDescriptorSpec;
import net.sourceforge.pmd.util.fxdesigner.model.testing.PropertyMapModel;
import net.sourceforge.pmd.util.treeexport.TreeRendererDescriptor;

public class LiveTreeRenderer implements ApplicationComponent {

    private final DesignerRoot root;
    private final TreeRendererDescriptor descriptor;
    private final PropertyMapModel liveProperties = new PropertyMapModel(null);

    public LiveTreeRenderer(DesignerRoot root, TreeRendererDescriptor descriptor) {
        this.root = root;
        this.descriptor = descriptor;

        LiveList<PropertyDescriptorSpec> props = new LiveArrayList<>();
        descriptor.newPropertyBundle().getPropertiesByPropertyDescriptor().forEach((d, v) -> {
            PropertyDescriptorSpec spec = new PropertyDescriptorSpec();

            spec.setName(d.name());
            spec.setDescription(d.description());
            spec.setValue(getValueAsString(d));
            props.add(spec);
        });


        liveProperties.setKnownProperties(props);
    }

    @Override
    public DesignerRoot getDesignerRoot() {
        return root;
    }

    public String getDescription() {
        return descriptor.description();
    }

    public String getName() {
        return descriptor.id();
    }

    public TreeRendererDescriptor getDescriptor() {
        return descriptor;
    }

    public PropertyMapModel getLiveProperties() {
        return liveProperties;
    }

    public String dumpSubtree(Node n) throws Exception {
        PropertySource bundle = descriptor.newPropertyBundle();

        Map<String, String> props = getLiveProperties().getNonDefault();

        for (String name : props.keySet()) {
            PropertyDescriptor<?> d = bundle.getPropertyDescriptor(name);
            setProperty(bundle, d, props.get(name));
        }

        StringBuilder builder = new StringBuilder();
        try {
            descriptor.produceRenderer(bundle).renderSubtree(n, builder);
        } catch (IOException e) {
            logInternalException(e);
            throw e;
        }
        return builder.toString();
    }


    private static <T> String getValueAsString(PropertyDescriptor<T> d) {
        return d.serializer().toString(d.defaultValue());
    }

    private static <T> void setProperty(PropertySource bundle, PropertyDescriptor<T> d, String value) {
        bundle.setProperty(d, d.serializer().fromString(value));
    }
}
