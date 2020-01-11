/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util;

import static net.sourceforge.pmd.util.fxdesigner.util.DesignerIteratorUtil.toIterable;

import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.ast.xpath.Attribute;

public final class XmlDumpUtil {

    private XmlDumpUtil() {

    }


    public static void appendXml(Writer writer, Node node) throws TransformerException,
                                                                  ParserConfigurationException {
        Document document = toXml(node);

        TransformerFactory trf = TransformerFactory.newInstance();

        Logger.getLogger(Attribute.class.getName())
              .setLevel(Level.OFF);


        Transformer transformer = trf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        DOMSource source = new DOMSource(document);
        transformer.transform(source, new StreamResult(writer));
    }

    private static Document toXml(Node node) throws ParserConfigurationException {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final DocumentBuilder db = dbf.newDocumentBuilder();
        final Document document = db.newDocument();
        appendElement(node, document, document);
        return document;
    }

    static void appendElement(final Node node, Document doc, final org.w3c.dom.Node parentNode) {

        final Element element = doc.createElement(node.getXPathNodeName());
        parentNode.appendChild(element);

        for (Attribute attribute : toIterable(node.getXPathAttributesIterator())) {
            element.setAttribute(attribute.getName(), attribute.getStringValue());
        }

        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            appendElement(node.jjtGetChild(i), doc, element);
        }
    }

}
