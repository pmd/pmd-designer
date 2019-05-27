/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.reactfx;

import java.util.Map;
import java.util.function.Function;

import org.reactfx.Subscription;
import org.reactfx.collection.LiveArrayList;
import org.reactfx.collection.LiveList;
import org.reactfx.value.ValBase;

import com.github.oowekyala.rxstring.ReactfxExtensions;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

public class GroupByLiveList<K, V> extends ValBase<Map<K, LiveList<V>>> {

    private final ObservableList<? extends V> base;
    private final Function<? super V, ? extends K> selector;

    private ObservableMap<K, LiveList<V>> value = FXCollections.observableHashMap();

    public GroupByLiveList(ObservableList<? extends V> base, Function<? super V, ? extends K> keyExtractor) {
        this.base = base;
        this.selector = keyExtractor;

    }

    @Override
    protected Subscription connect() {
        return ReactfxExtensions.dynamic(
            base,
            (v, idx) -> {
                K k = selector.apply(v);
                LiveList<V> vs = value.computeIfAbsent(k, kk -> new LiveArrayList<>());
                vs.add(v);
                notifyObservers(value);
                return () -> vs.remove(v);
            });
    }

    @Override
    protected Map<K, LiveList<V>> computeValue() {
        return value;
    }
}
