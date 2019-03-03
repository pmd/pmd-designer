package net.sourceforge.pmd.util.fxdesigner.util.datakeys;

import java.util.HashMap;
import java.util.Map;

import net.sourceforge.pmd.lang.ast.Node;

/**
 * @author Clément Fournier
 */
public final class DataHolderUtil {


    private DataHolderUtil() {

    }


    public static <T> T getUserData(DataKey<T> key, Node node) {
        if (node.getUserData() instanceof Map) {

            @SuppressWarnings("unchecked")
            T o = (T) ((Map) node.getUserData()).get(key);

            return o == null ? key.getDefaultValue() : o;
        }

        return key.getDefaultValue();
    }

    public static <T> T putUserData(DataKey<T> key, T data, Node node) {
        if (node.getUserData() instanceof Map) {

            @SuppressWarnings("unchecked")
            T o = (T) ((Map) node.getUserData()).put(key, data);

            return o;
        } else {
            node.setUserData(new HashMap<>());
            return putUserData(key, data, node);
        }
    }


}
