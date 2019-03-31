/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.beans.converters;

import static net.sourceforge.pmd.util.fxdesigner.util.beans.converters.Serializer.stringConversion;

import java.io.File;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.lang3.reflect.Typed;

/**
 * @author Clément Fournier
 */
public class SerializerRegistrar {


    private static final SerializerRegistrar INSTANCE = new SerializerRegistrar();

    private final Map<Type, Serializer<?>> converters = new WeakHashMap<>();

    public SerializerRegistrar() {
        registerStandard();
    }

    /**
     * Registers a new serializer for type [toRegister], which is based
     * on an already registered serializer for [existing]. The new serializer
     * is obtained using {@link Serializer#map(Function, Function)}.
     */
    public final <T, U> void registerMapped(Class<T> toRegister, Class<U> existing,
                                            Function<U, T> fromU, Function<T, U> toU) {
        if (converters.get(existing) == null) {
            throw new IllegalStateException("No existing converter for " + existing);
        }
        register(getSerializer(existing).map(fromU, toU), toRegister);
    }

    private void registerStandard() {

        register(stringConversion(Function.identity(), Function.identity()), String.class);
        register(stringConversion(Integer::valueOf, i -> Integer.toString(i)), Integer.class, Integer.TYPE);
        register(stringConversion(Double::valueOf, d -> Double.toString(d)), Double.class, Double.TYPE);
        register(stringConversion(Boolean::valueOf, b -> Boolean.toString(b)), Boolean.class, Boolean.TYPE);
        register(stringConversion(Long::valueOf, b -> Long.toString(b)), Long.class, Long.TYPE);
        register(stringConversion(Float::valueOf, b -> Float.toString(b)), Float.class, Float.TYPE);
        register(stringConversion(Short::valueOf, b -> Short.toString(b)), Short.class, Short.TYPE);
        register(stringConversion(Byte::valueOf, b -> Byte.toString(b)), Byte.class, Byte.TYPE);
        register(stringConversion(s -> s.charAt(0), b -> Character.toString(b)), Character.class, Character.TYPE);


        register(getSerializer(Long.TYPE).map(Date::new, Date::getTime), Date.class);
        register(getSerializer(Long.TYPE).map(java.sql.Date::new, java.sql.Date::getTime), java.sql.Date.class);
        register(getSerializer(Long.TYPE).map(java.sql.Time::new, java.sql.Time::getTime), java.sql.Time.class);
        register(stringConversion(File::new, File::getPath), File.class);
        register(stringConversion(Paths::get, Path::toString), Path.class);
        register(stringConversion(className -> {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                return null;
            }
        }, Class::getName), Class.class);

    }


    @SafeVarargs
    public final <T> void register(Serializer<T> serializer, Typed<T>... type) {
        for (Typed<T> tClass : type) {
            converters.put(tClass.getType(), serializer);
        }
    }

    @SafeVarargs
    public final <T> void register(Serializer<T> serializer, Class<T>... type) {
        for (Class<T> it : type) {
            converters.put(it, serializer);
        }
    }

    public final <T> Serializer<T> getSerializer(Class<T> type) {
        @SuppressWarnings("unchecked")
        Serializer<T> t = (Serializer<T>) converters.get(type);
        return t;
    }

    public final <T> Serializer<T> getSerializer(Typed<T> typed) {
        @SuppressWarnings("unchecked")
        Serializer<T> serializer = (Serializer<T>) getSerializer(typed.getType());
        return serializer;
    }


    @SuppressWarnings("unchecked")
    public final Serializer<Object> getSerializer(Type genericType) {
        if (converters.containsKey(genericType)) {
            return (Serializer<Object>) converters.get(genericType);
        }

        if (genericType instanceof Class) {
            return getSerializer((Class) genericType);
        } else if (genericType instanceof ParameterizedType) {
            Class rawType = (Class) ((ParameterizedType) genericType).getRawType();


            Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();

            Supplier<Collection<Object>> emptyCollSupplier = null;
            if (rawType != null && Collection.class.isAssignableFrom(rawType)) {
                if (List.class.isAssignableFrom(rawType)) {
                    emptyCollSupplier = ArrayList::new;
                } else if (Set.class.isAssignableFrom(rawType)) {
                    emptyCollSupplier = HashSet::new;
                }
            }

            if (actualTypeArguments.length == 1 && emptyCollSupplier != null) {
                Serializer componentSerializer = getSerializer(actualTypeArguments[0]);
                if (componentSerializer != null) {
                    return (Serializer<Object>) componentSerializer.<Collection<Object>>toSeq(emptyCollSupplier);
                }
            }

            return getSerializer(rawType);
        }

        return null;
    }


    public static SerializerRegistrar getInstance() {
        return INSTANCE;
    }

}
