/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.beans.converters;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Converts a value of type {@code <T>} to and from an XML element.
 *
 * @param <T> Type of value handled
 */
public interface Serializer<T> {


    /**
     * Produce an XML element that represents the value [t]. The parameter
     * [eltFactory] can be used to produce a new element to add children.
     * The returned element must be understood by {@link #fromXml(Element)}.
     */
    Element toXml(T t, Supplier<Element> eltFactory);


    /**
     * Parses the given XML element into a value of type {@code <T>}. This
     * method must be kept in sync with {@link #toXml(Object, Supplier)}.
     */
    T fromXml(Element s);


    /**
     * Returns a new serializer that can handle another type {@code <S>},
     * provided {@code <T>} can be mapped to and from {@code <S>}.
     */
    default <S> Serializer<S> map(Function<T, S> toS, Function<S, T> fromS) {
        Serializer<T> nullable = nullable();

        return new Serializer<S>() {
            @Override
            public Element toXml(S s, Supplier<Element> eltFactory) {
                return nullable.toXml(fromS.apply(s), eltFactory);
            }

            @Override
            public S fromXml(Element s) {
                return toS.apply(nullable.fromXml(s));
            }
        }.nullable();
    }


    /**
     * Builds a new serializer that can serialize arrays of component type
     * {@code <T>}.
     *
     * @param emptyArray Empty array supplier
     *
     * @return A new serializer
     */
    default Serializer<T[]> toArray(T[] emptyArray) {
        return
            this.<List<T>>toSeq(ArrayList::new)
                .map(l -> l.toArray(emptyArray), Arrays::asList).nullable();
    }


    /**
     * Builds a new serializer that can serialize maps with key type {@code <T>}.
     *
     * @param emptyMapSupplier Supplier for a collection of the correct
     *                          type, to which the deserialized elements
     *                          are added.
     * @param <M>               Map type to serialize
     *
     * @return A new serializer
     */
    default <V, M extends Map<T, V>> Serializer<M> toMap(Supplier<M> emptyMapSupplier, Serializer<V> valueSerializer) {

        Serializer<T> nullableKey = nullable();
        Serializer<V> nullableValue = valueSerializer.nullable();

        class MyDecorator implements Serializer<M> {

            @Override
            public Element toXml(M c, Supplier<Element> eltFactory) {
                Element mapRoot = eltFactory.get();
                c.forEach((t, v) -> {
                    Element entry = eltFactory.get();
                    entry.appendChild(nullableKey.toXml(t, eltFactory));
                    entry.appendChild(nullableValue.toXml(v, eltFactory));
                    mapRoot.appendChild(entry);
                });
                return mapRoot;
            }

            @Override
            public M fromXml(Element element) {
                M result = emptyMapSupplier.get();

                NodeList children = element.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node item = children.item(i);
                    if (item.getNodeType() == Element.ELEMENT_NODE) {
                        Element entry = (Element) item;
                        Element key = ((Element) entry.getFirstChild());
                        Element value = ((Element) entry.getLastChild());
                        result.put(nullableKey.fromXml(key), nullableValue.fromXml(value));
                    }
                }

                return result;
            }
        }

        return new MyDecorator().nullable();
    }


    /**
     * Builds a new serializer that can serialize arbitrary collections
     * with element type {@code <T>}.
     *
     * @param emptyCollSupplier Supplier for a collection of the correct
     *                          type, to which the deserialized elements
     *                          are added.
     * @param <C>               Collection type to serialize
     *
     * @return A new serializer
     */
    default <C extends Collection<T>> Serializer<C> toSeq(Supplier<C> emptyCollSupplier) {

        Serializer<T> nullable = nullable();

        class MyDecorator implements Serializer<C> {

            @Override
            public Element toXml(C t, Supplier<Element> eltFactory) {
                Element element = eltFactory.get();
                t.stream().map(v -> nullable.toXml(v, eltFactory)).forEach(element::appendChild);
                return element;
            }

            @Override
            public C fromXml(Element element) {
                C result = emptyCollSupplier.get();

                NodeList children = element.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node item = children.item(i);
                    if (item.getNodeType() == Element.ELEMENT_NODE) {
                        Element child = (Element) item;
                        result.add(nullable.fromXml(child));
                    }
                }

                return result;
            }
        }

        return new MyDecorator().nullable();
    }


    /**
     * Returns a decorated serializer that can handle null values. Standard
     * serializer combinators already all return a nullable serializer. This
     * method returns this if it's already nullable.
     */
    default Serializer<T> nullable() {
        class MyDecorator implements Serializer<T> {

            @Override
            public Element toXml(T t, Supplier<Element> eltFactory) {
                if (t != null) {
                    return Serializer.this.toXml(t, eltFactory);
                } else {
                    Element element = eltFactory.get();
                    element.setAttribute("null", "true");
                    return element;
                }
            }

            @Override
            public T fromXml(Element element) {
                return element.hasAttribute("null") ? null : Serializer.this.fromXml(element);
            }
        }

        return this.getClass().equals(MyDecorator.class) ? this : new MyDecorator(); // NOPMD
    }


    /**
     * Simple serialization from and to a string.
     */
    static <T> Serializer<T> stringConversion(Function<String, T> fromString, Function<T, String> toString) {

        class MyDecorator implements Serializer<T> {

            @Override
            public Element toXml(T t, Supplier<Element> eltFactory) {
                Element element = eltFactory.get();
                element.setAttribute("value", toString.apply(t));
                return element;
            }

            @Override
            public T fromXml(Element element) {
                return fromString.apply(element.getAttribute("value"));
            }
        }

        return new MyDecorator().nullable();
    }


}
