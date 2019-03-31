/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.beans.converters;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
        return new Serializer<S>() {
            @Override
            public Element toXml(S s, Supplier<Element> eltFactory) {
                return Serializer.this.toXml(fromS.apply(s), eltFactory);
            }

            @Override
            public S fromXml(Element s) {
                return toS.apply(Serializer.this.fromXml(s));
            }
        };
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
                .map(l -> l.toArray(emptyArray), Arrays::asList);
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

        class MySerializer implements Serializer<C> {

            @Override
            public Element toXml(C t, Supplier<Element> eltFactory) {
                Element element = eltFactory.get();
                t.stream().map(v -> Serializer.this.toXml(v, eltFactory)).forEach(element::appendChild);
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
                        result.add(Serializer.this.fromXml(child));
                    }
                }

                return result;
            }
        }

        return new MySerializer();
    }


    /**
     * Simple serialization from and to a string.
     */
    static <T> Serializer<T> stringConversion(Function<String, T> fromString, Function<T, String> toString) {

        class MySerializer implements Serializer<T> {

            @Override
            public Element toXml(T t, Supplier<Element> eltFactory) {
                Element element = eltFactory.get();
                if (t != null) {
                    // todo escape ?
                    element.setAttribute("value", toString.apply(t));
                } else {
                    element.setAttribute("null", "true");
                }
                return element;
            }

            @Override
            public T fromXml(Element element) {
                if (element.hasAttribute("null")) {
                    return null;
                }
                return fromString.apply(element.getAttribute("value"));
            }
        }

        return new MySerializer();
    }


}
