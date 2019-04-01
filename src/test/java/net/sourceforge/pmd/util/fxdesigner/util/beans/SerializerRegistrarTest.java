/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.beans;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.commons.lang3.reflect.Typed;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.sourceforge.pmd.util.fxdesigner.util.beans.converters.Serializer;
import net.sourceforge.pmd.util.fxdesigner.util.beans.converters.SerializerRegistrar;

/**
 * @author Clément Fournier
 */
public class SerializerRegistrarTest {

    SerializerRegistrar testRegistrar;

    @Before
    public void setup() {
        testRegistrar = new SerializerRegistrar();
    }

    @Test
    public void testListSerializer() {
        roundTrip(new TypeLiteral<List<List<String>>>() {}, asList(asList("a", "b"), asList("c", "d")));
        roundTrip(new TypeLiteral<List<List<String>>>() {}, Collections.emptyList());
    }

    @Test
    public void testStringSerializerEscape() {
        roundTrip(String.class, "foo & <bar></bar>");
        roundTrip(String.class, "foo\" haha\"");
    }


    @Test
    public void testMappedSerializer() {

        class Foo {

            @Override
            public boolean equals(Object obj) {
                return super.equals(obj);
            }
        }

        assertNull(testRegistrar.getSerializer(Foo.class));

        testRegistrar.registerMapped(Foo.class, String.class, Object::toString, s -> new Foo());

        assertNotNull(testRegistrar.getSerializer(Foo.class));

        Serializer<Foo[]> serializer = testRegistrar.getSerializer(Foo[].class);

        roundTrip(serializer, new Foo[] {new Foo()}, Assert::assertArrayEquals);
    }
    @Test
    public void testArraySerializer() {

        roundTrip(int[].class, new int[0], Assert::assertArrayEquals);
        roundTrip(int[].class, new int[] {1, 2}, Assert::assertArrayEquals);
        roundTrip(int[][].class, new int[][] {}, Assert::assertArrayEquals);

    }

    @Test
    public void testListSerializerRegistrarOverride() {

        testRegistrar.register(nullSerializer(), new TypeLiteral<List<List<String>>>() { });

        Serializer<List<List<String>>> serializer =
            testRegistrar.getSerializer(new TypeLiteral<List<List<String>>>() { });

        Element element = serializer.toXml(
            asList(asList("a", "b"), asList("c", "d")),
            dummyElementFactory()
        );

        assertNull(serializer.fromXml(element));

    }


    private <T> void roundTrip(Typed<T> typed, T val) {
        roundTrip(testRegistrar.getSerializer(typed), val, Assert::assertEquals);
    }

    private <T> void roundTrip(Class<T> type, T val) {
        roundTrip(type, val, Assert::assertEquals);
    }

    private <T> void roundTrip(Class<T> type, T val, BiConsumer<T, T> asserter) {
        roundTrip(testRegistrar.getSerializer(type), val, asserter);
    }

    private <T> void roundTrip(Serializer<T> serializer, T expected, BiConsumer<T, T> asserter) {
        T actual = serializer.fromXml(serializer.toXml(expected, dummyElementFactory()));
        asserter.accept(expected, actual);
    }

    private Supplier<Element> dummyElementFactory() {
        Document result;
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            result = documentBuilderFactory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new AssertionError(e);
        }
        Document doc = result;
        return () -> doc.createElement("val");
    }


    private static <T> Serializer<T> nullSerializer() {
        return new Serializer<T>() {
            @Override
            public Element toXml(T t, Supplier<Element> eltFactory) {
                return eltFactory.get();
            }

            @Override
            public T fromXml(Element s) {
                return null;
            }
        };
    }

}
