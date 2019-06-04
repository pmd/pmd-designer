/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.reactfx.collection.LiveList;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.model.PropertyDescriptorSpec;
import net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil;

import com.github.oowekyala.rxstring.ReactfxExtensions;
import com.github.oowekyala.rxstring.ReactfxExtensions.RebindSubscription;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.util.Pair;

public class PropertyMapModel {

    private final ObservableMap<PropertyDescriptorSpec, Var<String>> mapping;
    private final ObservableList<PropertyDescriptorSpec> props;
    // properties that have no PropertyDescriptorSpec...
    private final Map<String, String> orphanProperties = new HashMap<>();

    public PropertyMapModel(ObservableList<PropertyDescriptorSpec> props) {
        mapping = localMapper(props);
        this.props = props;
    }

    public LiveList<Pair<PropertyDescriptorSpec, Var<String>>> asList() {
        return ReactfxUtil.mapBothWays(props, it -> new Pair<>(it, mapping.get(it)), Pair::getKey);
    }

    public ObservableMap<PropertyDescriptorSpec, Var<String>> asMap() {
        return mapping;
    }

    public Map<String, String> getNonDefault() {
        return mapping.entrySet()
                      .stream()
                      .filter(it -> !Objects.equals(it.getValue().getValue(), it.getKey().getValue()))
                      .collect(Collectors.toMap(it -> it.getKey().getName(), it -> it.getValue().getValue()));

    }

    public Map<String, String> getOrphanProperties() {
        return orphanProperties;
    }

    public void setProperty(String name, String value) {
        Optional<PropertyDescriptorSpec> p = props.stream().filter(it -> it.getName().equals(name)).findFirst();
        if (p.isPresent()) {
            mapping.get(p.get()).setValue(value);
        } else {
            orphanProperties.put(name, value);
        }
    }


    private static ObservableMap<PropertyDescriptorSpec, Var<String>> localMapper(ObservableList<PropertyDescriptorSpec> props) {

        ObservableMap<PropertyDescriptorSpec, Var<String>> mapping = FXCollections.observableHashMap();

        ReactfxExtensions.dynamicRecombine(props, (addedP, i) -> {
            mapping.put(addedP, ReactfxUtil.defaultedVar(addedP.valueProperty()));
            return makeDefault(addedP, mapping);
        });

        return mapping;
    }

    private static RebindSubscription<PropertyDescriptorSpec> makeDefault(PropertyDescriptorSpec p, ObservableMap<PropertyDescriptorSpec, Var<String>> mapping) {
        return RebindSubscription.make(
            () -> mapping.remove(p),
            addedP -> {
                if (addedP == p) {
                    return makeDefault(p, mapping);
                } else {
                    mapping.remove(p);
                    mapping.put(addedP, ReactfxUtil.defaultedVar(addedP.valueProperty()));
                    return makeDefault(addedP, mapping);
                }
            }
        );
    }


}
