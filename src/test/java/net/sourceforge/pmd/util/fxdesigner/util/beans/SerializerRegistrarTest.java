/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.beans;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.function.Supplier;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.reflect.TypeLiteral;
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
    public void testListSerializer() throws ParserConfigurationException {

        Serializer<List<List<String>>> serializer = testRegistrar.getSerializer(new TypeLiteral<List<List<String>>>() { });

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        Document document = documentBuilderFactory.newDocumentBuilder().newDocument();

        Element element = serializer.toXml(
            asList(asList("a", "b"), asList("c", "d")),
            () -> document.createElement("val")
        );

        assertEquals(2, element.getChildNodes().getLength());

    }

    @Test
    public void testArraySerializer() throws ParserConfigurationException {

        class Foo {}

        testRegistrar.registerMapped(Foo.class, String.class, Object::toString, s -> new Foo());

        assertNotNull(testRegistrar.getSerializer(Foo.class));

        Serializer<Foo[]> serializer = testRegistrar.getSerializer(Foo[].class);

        assertNotNull(serializer);

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        Document document = documentBuilderFactory.newDocumentBuilder().newDocument();

        Element element = serializer.toXml(
            new Foo[] { new Foo() },
            () -> document.createElement("val")
        );

        assertEquals(1, element.getChildNodes().getLength());
    }

    @Test
    public void testListSerializerRegistrarOverride() throws ParserConfigurationException {

        testRegistrar.register(nullSerializer(), new TypeLiteral<List<List<String>>>() { });

        Serializer<List<List<String>>> serializer =
            testRegistrar.getSerializer(new TypeLiteral<List<List<String>>>() { });

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        Document document = documentBuilderFactory.newDocumentBuilder().newDocument();

        Element element = serializer.toXml(
            asList(asList("a", "b"), asList("c", "d")),
            () -> document.createElement("val")
        );

        assertEquals(0, element.getChildNodes().getLength());

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
