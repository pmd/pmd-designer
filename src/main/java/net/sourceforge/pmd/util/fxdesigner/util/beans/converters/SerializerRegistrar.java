/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.beans.converters;

import static net.sourceforge.pmd.util.fxdesigner.util.beans.converters.Serializer.stringConversion;

import java.io.File;
import java.lang.reflect.Array;
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
 * A collection of serializers. Once you register a serializer for a type T,
 * both array types and list/set types of any depth with that type T as element
 * type can be serialized without having to register anything else. You
 * can override their serialization routine explicitly if you want though.
 *
 * <p>Serializers for common types are registered implicitly.
 *
 * <p>Instead of creating serializer for new types from scratch, you can
 * instead map existing serializers to your new types. Eg if some value of
 * type {@code Foo} has a string id, then you can do:
 * <pre>
 *     {@code
 *      registrar.registerMapped(Foo.class, String.class, Foo::fromId, Foo::getId);
 *     }
 * </pre>
 *
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
                                            Function<T, U> toBase, Function<U, T> fromBase) {
        if (converters.get(existing) == null) {
            throw new IllegalStateException("No existing converter for " + existing);
        }
        register(getSerializer(existing).map(fromBase, toBase), toRegister);
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


        registerMapped(Date.class, Long.TYPE, Date::getTime, Date::new);
        registerMapped(java.sql.Time.class, Long.TYPE, java.sql.Time::getTime, java.sql.Time::new);
        registerMapped(java.sql.Date.class, Long.TYPE, java.sql.Date::getTime, java.sql.Date::new);

        registerMapped(File.class, String.class, File::getPath, File::new);
        registerMapped(Path.class, String.class, Path::toString, Paths::get);
        registerMapped(Class.class, String.class, Class::getName, className -> {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                return null;
            }
        });

    }


    /**
     * Registers a serializer suitable for one or more types. It can then
     * be accessed on this registrar instance with {@link #getSerializer(Typed)}.
     */
    @SafeVarargs
    public final <T> void register(Serializer<T> serializer, Typed<T> firstType, Typed<T>... type) {
        converters.put(firstType.getType(), serializer);
        for (Typed<T> tClass : type) {
            converters.put(tClass.getType(), serializer);
        }
    }

    @SafeVarargs
    public final <T> void register(Serializer<T> serializer, Class<T> firstType, Class<T>... type) {
        converters.put(firstType, serializer);
        for (Class<T> it : type) {
            converters.put(it, serializer);
        }
    }

    /**
     * Gets a registered serializer for an a type. If the class is an
     * array type and some component type has a registered serializer,
     * a new array serializer will be derived and returned transparently.
     *
     * <p>To get serializers for collection types, use rather {@link #getSerializer(Typed)}.
     *
     * @return A serializer, or null if none can be derived
     */
    public final <T> Serializer<T> getSerializer(Class<T> type) {
        @SuppressWarnings("unchecked")
        Serializer<T> t = (Serializer<T>) converters.get(type);
        if (t == null && type.isArray()) {
            Class<?> component = type.getComponentType();
            Serializer<T> tSerializer = arraySerializerCapture(component);
            converters.put(type, tSerializer);
            return tSerializer;
        }
        return t;
    }

    private <T, V> Serializer<T> arraySerializerCapture(Class<V> component) {
        Serializer<V> componentSerializer = getSerializer(component);
        if (componentSerializer != null) {
            @SuppressWarnings("unchecked")
            V[] emptyArr = (V[]) Array.newInstance(component, 0);
            @SuppressWarnings("unchecked")
            Serializer<T> serializer = (Serializer<T>) componentSerializer.toArray(emptyArr);
            return serializer;
        }
        return null;
    }

    /**
     * Typesafe version of {@link #getSerializer(Type)}.
     *
     * @param typed Type witness
     */
    public final <T> Serializer<T> getSerializer(Typed<T> typed) {
        @SuppressWarnings("unchecked")
        Serializer<T> serializer = (Serializer<T>) getSerializer(typed.getType());
        return serializer;
    }


    /**
     * Get a serializer for some generic type. If the type is generic and
     * has at most one type argument, a best effort strategy will try to
     * find a suitable serializer.
     *
     * @param genericType The type for which to get a serializer.
     *
     * @return A serializer, or null if none can be found
     */
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

            if (actualTypeArguments.length != 1) {
                return null;
            }

            Supplier<Collection<Object>> emptyCollSupplier = null;
            if (rawType != null && Collection.class.isAssignableFrom(rawType)) {
                if (List.class.isAssignableFrom(rawType)) {
                    emptyCollSupplier = ArrayList::new;
                } else if (Set.class.isAssignableFrom(rawType)) {
                    emptyCollSupplier = HashSet::new;
                }
            }

            if (emptyCollSupplier != null) {
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
