/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.beans;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.function.Supplier;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.commons.lang3.reflect.Typed;
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
        roundTrip(new TypeLiteral<List<List<String>>>() { }, asList(asList("a", "b"), asList("c", "d")));
        roundTrip(new TypeLiteral<List<List<String>>>() { }, emptyList());
    }

    @Test
    public void testStringSerializerEscape() {
        roundTrip(String.class, "foo & <bar></bar>");
        roundTrip(String.class, "foo\" haha\"");
    }

    @Test
    public void testWildcardSerializer() {
        roundTrip(new TypeLiteral<List<? extends List<String>>>() { }, asList(emptyList(), asList("", "foo & <bar></bar>")));
        roundTrip(new TypeLiteral<List<? extends List<String>>>() { }, emptyList());
    }

    @Test
    public void testNullValue() {
        roundTrip(String.class, null);
        roundTrip(new TypeLiteral<List<? extends List<String>>>() { }, null);
        roundTrip(new TypeLiteral<List<? extends List<String>>>() { }, asList(null, asList(null, "")));
    }

    @Test
    public void testMappedSerializer() {

        assertNull(testRegistrar.getSerializer(TestEnum.class));

        testRegistrar.registerMapped(TestEnum.class, String.class, TestEnum::name, s -> EnumUtils.getEnum(TestEnum.class, s));

        roundTrip(TestEnum.class, TestEnum.A);
    }

    private <T> void roundTrip(Typed<T> typed, T val) {
        roundTrip(testRegistrar.getSerializer(typed), val);
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

    private <T> void roundTrip(Class<T> type, T val) {
        roundTrip(testRegistrar.getSerializer(type), val);
    }

    private <T> void roundTrip(Serializer<T> serializer, T expected) {
        T actual = serializer.fromXml(serializer.toXml(expected, dummyElementFactory()));
        assertEquals(expected, actual);
    }


    private enum TestEnum {
        A, B
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
