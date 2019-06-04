/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.reactfx.EventStreams;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;
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
    private final Var<ObservableList<PropertyDescriptorSpec>> knownPropsImpl = Var.newSimpleVar(FXCollections.emptyObservableList());

    private final ObservableList<PropertyDescriptorSpec> knownProps = ReactfxUtil.flattenList(knownPropsImpl);

    // properties that have no PropertyDescriptorSpec...
    private final Map<String, String> orphanProperties = new HashMap<>();

    public PropertyMapModel(ObservableList<PropertyDescriptorSpec> knownProps) {
        Pair<ObservableMap<PropertyDescriptorSpec, Var<String>>, RebindSubscription<ObservableList<PropertyDescriptorSpec>>> mappingAndSub = localMapper(knownProps);
        this.mapping = mappingAndSub.getKey();

        this.knownPropsImpl.setValue(knownProps);

        knownPropsImpl.values().subscribe(ps -> mappingAndSub.getValue().rebind(ps));
    }

    public void setKnownProperties(ObservableList<PropertyDescriptorSpec> props) {
        getNonDefault().forEach((k, v) -> {
            if (props.stream().noneMatch(it -> it.getName().equals(k))) {
                orphanProperties.put(k, v);
            }
        });
        knownPropsImpl.setValue(props);
        mapping.forEach((k, v) -> {
            String orphan = orphanProperties.get(k.getName());
            if (orphan != null) {
                orphanProperties.remove(k.getName());
                v.setValue(orphan);
            }
        });
    }

    public LiveList<Pair<PropertyDescriptorSpec, Var<String>>> asList() {
        return ReactfxUtil.mapBothWays(knownProps, it -> new Pair<>(it, mapping.computeIfAbsent(it, k -> ReactfxUtil.defaultedVar(k.valueProperty()))), Pair::getKey);
    }

    public ObservableMap<PropertyDescriptorSpec, Var<String>> asMap() {
        return mapping;
    }

    public Val<Map<String, String>> nonDefaultProperty() {
        return ReactfxUtil.withInvalidations(
            ReactfxUtil.observableMapVal(mapping),
            map -> EventStreams.merge(map.values().stream().map(Val::values).collect(Collectors.toCollection(() -> FXCollections.observableSet(new HashSet<>()))))
        ).map(this::computeNonDefault);
    }


    public Map<String, String> getNonDefault() {
        return computeNonDefault(mapping);

    }

    // TODO report those
    public Map<String, String> getOrphanProperties() {
        return orphanProperties;
    }

    public void setProperty(String name, String value) {
        Optional<PropertyDescriptorSpec> p = knownProps.stream().filter(it -> it.getName().equals(name)).findFirst();
        if (p.isPresent()) {
            mapping.get(p.get()).setValue(value);
        } else {
            orphanProperties.put(name, value);
        }
    }

    @NonNull
    private Map<String, String> computeNonDefault(Map<PropertyDescriptorSpec, Var<String>> map) {
        return map.entrySet()
                  .stream()
                  .filter(it -> !Objects.equals(it.getValue().getValue(), it.getKey().getValue()))
                  .collect(Collectors.toMap(it -> it.getKey().getName(), it -> it.getValue().getValue()));
    }


    private static Pair<ObservableMap<PropertyDescriptorSpec, Var<String>>, RebindSubscription<ObservableList<PropertyDescriptorSpec>>> localMapper(ObservableList<PropertyDescriptorSpec> props) {

        ObservableMap<PropertyDescriptorSpec, Var<String>> mapping = FXCollections.observableHashMap();

        RebindSubscription<ObservableList<PropertyDescriptorSpec>> lstSub = ReactfxExtensions.dynamicRecombine(props, (addedP, i) -> {
            mapping.put(addedP, ReactfxUtil.defaultedVar(addedP.valueProperty()));
            return makeDefault(addedP, mapping);
        });

        return new Pair<>(mapping, lstSub);
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
