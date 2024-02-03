/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import static javafx.collections.FXCollections.emptyObservableList;
import static javafx.collections.FXCollections.observableHashMap;
import static javafx.collections.FXCollections.observableSet;
import static net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil.defaultedVar;
import static net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil.flattenList;
import static net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil.mapBothWays;
import static net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil.observableMapVal;
import static net.sourceforge.pmd.util.fxdesigner.util.reactfx.ReactfxUtil.withInvalidations;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.reactfx.EventStreams;
import org.reactfx.collection.LiveList;
import org.reactfx.value.Val;
import org.reactfx.value.Var;

import net.sourceforge.pmd.util.fxdesigner.model.PropertyDescriptorSpec;

import com.github.oowekyala.rxstring.ReactfxExtensions;
import com.github.oowekyala.rxstring.ReactfxExtensions.RebindSubscription;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.util.Pair;

public class PropertyMapModel {

    private final ObservableMap<PropertyDescriptorSpec, Var<String>> mapping;
    private final Var<ObservableList<PropertyDescriptorSpec>> knownPropsImpl = Var.newSimpleVar(emptyObservableList());

    private final ObservableList<PropertyDescriptorSpec> knownProps = flattenList(knownPropsImpl);

    // properties that have no PropertyDescriptorSpec...
    private final Map<String, String> orphanProperties = new HashMap<>();

    public PropertyMapModel(@Nullable ObservableList<PropertyDescriptorSpec> knownProps) {
        ObservableList<PropertyDescriptorSpec> safe = defaultToEmpty(knownProps);
        Pair<ObservableMap<PropertyDescriptorSpec, Var<String>>, RebindSubscription<ObservableList<PropertyDescriptorSpec>>> mappingAndSub = localMapper(safe);
        this.mapping = mappingAndSub.getKey();

        this.knownPropsImpl.setValue(knownProps);

        knownPropsImpl.orElseConst(emptyObservableList()).values().subscribe(ps -> mappingAndSub.getValue().rebind(ps));
    }

    public void setKnownProperties(@Nullable ObservableList<PropertyDescriptorSpec> props) {
        ObservableList<PropertyDescriptorSpec> safe = defaultToEmpty(props);
        getNonDefault().forEach((k, v) -> {
            if (safe.stream().noneMatch(it -> it.getName().equals(k))) {
                orphanProperties.put(k, v);
            }
        });
        knownPropsImpl.setValue(props); // we need to keep a null value in there for it to be replaced later on
        mapping.forEach((k, v) -> {
            String orphan = orphanProperties.get(k.getName());
            if (orphan != null) {
                orphanProperties.remove(k.getName());
                v.setValue(orphan);
            }
        });
    }

    @NonNull
    private ObservableList<PropertyDescriptorSpec> defaultToEmpty(@Nullable ObservableList<PropertyDescriptorSpec> props) {
        return props == null ? emptyObservableList() : props;
    }

    public LiveList<Pair<PropertyDescriptorSpec, Var<String>>> asList() {
        return mapBothWays(knownProps, it -> new Pair<>(it, mapping.computeIfAbsent(it, k -> defaultedVar(k.valueProperty()))), Pair::getKey);
    }

    public ObservableMap<PropertyDescriptorSpec, Var<String>> asMap() {
        return mapping;
    }

    public Val<Map<String, String>> nonDefaultProperty() {
        return withInvalidations(
            observableMapVal(mapping),
            map -> EventStreams.merge(map.values().stream().map(Val::values).collect(Collectors.toCollection(() -> observableSet(new HashSet<>()))))
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

        ObservableMap<PropertyDescriptorSpec, Var<String>> mapping = observableHashMap();

        RebindSubscription<ObservableList<PropertyDescriptorSpec>> lstSub = ReactfxExtensions.dynamicRecombine(props, (addedP, i) -> {
            mapping.put(addedP, defaultedVar(addedP.valueProperty()));
            return makeDefault(addedP, mapping);
        });

        return new Pair<>(mapping, lstSub);
    }

    private static RebindSubscription<PropertyDescriptorSpec> makeDefault(PropertyDescriptorSpec p, ObservableMap<PropertyDescriptorSpec, Var<String>> mapping) {
        return RebindSubscription.make(
            () -> mapping.remove(p),
            addedP -> {
                if (addedP.equals(p)) {
                    return makeDefault(p, mapping);
                } else {
                    mapping.remove(p);
                    mapping.put(addedP, defaultedVar(addedP.valueProperty()));
                    return makeDefault(addedP, mapping);
                }
            }
        );
    }


}
