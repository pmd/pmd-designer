/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.reactfx.collection.LiveList;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import net.sourceforge.pmd.lang.document.TextDocument;
import net.sourceforge.pmd.lang.document.TextRegion;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.PlainTextLanguage;

public class TestXmlDumper {


    public static final String SCHEMA_LOCATION = "http://pmd.sourceforge.net/rule-tests http://pmd.sourceforge.net/rule-tests_1_0_0.xsd";
    private static final String NS = "http://pmd.sourceforge.net/rule-tests";


    private void appendTests(Document doc, List<LiveTestCase> descriptors) {
        Element root = doc.getDocumentElement();

        for (LiveTestCase descriptor : descriptors) {
            Element elt = doc.createElementNS(NS, "test-code");
            appendSingle(elt, descriptor, doc);
            root.appendChild(elt);
            root.appendChild(doc.createTextNode("\n\n"));
        }
    }


    private void appendSingle(Element testCode, LiveTestCase descriptor, Document doc) {

        if (descriptor.isIgnored()) {
            testCode.setAttributeNS(NS, "regressionTest", "false");
        }


        Element descriptionElt = doc.createElementNS(NS, "description");
        descriptionElt.setTextContent(descriptor.getDescription());
        testCode.appendChild(descriptionElt);

        Map<String, String> properties = descriptor.getLiveProperties().getNonDefault();

        if (!properties.isEmpty()) {
            properties.forEach((k, v) -> {
                Element element = doc.createElementNS(NS, "rule-property");
                element.setAttribute("name", k);
                element.setTextContent(v);
                testCode.appendChild(element);
            });

        }

        LiveList<LiveViolationRecord> expectedViolations = descriptor.getExpectedViolations();

        Element numViolations = doc.createElementNS(NS, "expected-problems");
        numViolations.setTextContent(expectedViolations.size() + "");
        testCode.appendChild(numViolations);

        if (expectedViolations.size() > 0 && expectedViolations.stream().allMatch(it -> it.getMessage() != null)) {
            Element messages = doc.createElementNS(NS, "expected-messages");
            for (LiveViolationRecord record : expectedViolations) {
                Element r = doc.createElementNS(NS, "message");
                r.setTextContent(record.getMessage());
                messages.appendChild(r);
            }
            testCode.appendChild(messages);
        }

        if (expectedViolations.size() > 0) {
            Element linenos = doc.createElementNS(NS, "expected-linenumbers");

            // create a text doc just to ask for line numbers
            TextDocument textDocument = TextDocument.readOnlyString(descriptor.getSource(), PlainTextLanguage.INSTANCE.getDefaultVersion());

            String joined = expectedViolations
                .stream()
                .mapToInt(it -> getLine(textDocument, it.getRegion()))
                .mapToObj(Integer::toString)
                .collect(Collectors.joining(","));
            linenos.setTextContent(joined);
            testCode.appendChild(linenos);

        }

        Element codeElement = doc.createElementNS(NS, "code");
        codeElement.appendChild(doc.createCDATASection("\n" + descriptor.getSource().trim() + "\n        "));
        testCode.appendChild(codeElement);


        boolean hasNonDefaultVersion = descriptor.languageVersionProperty()
                                                 .getOpt()
                                                 .filter(it -> it.getLanguage().getDefaultVersion() != it)
                                                 .isPresent();
        if (hasNonDefaultVersion) {
            Element sourceType = doc.createElementNS(NS, "source-type");
            sourceType.setTextContent(descriptor.getLanguageVersion().getTerseName());
            testCode.appendChild(sourceType);
        }
    }


    private int getLine(TextDocument textDocument, @NonNull TextRegion region) {
        return textDocument.lineColumnAtOffset(region.getStartOffset()).getLine();
    }


    public static String dumpXmlTests(TestCollection collection) throws Exception {
        StringWriter out = new StringWriter();
        dumpXmlTests(out, collection);
        return out.toString();
    }


    public static void dumpXmlTests(Path path, TestCollection collection) throws Exception {
        try (OutputStream is = Files.newOutputStream(path);
             Writer out = new OutputStreamWriter(is)) {

            dumpXmlTests(out, collection);
        }
    }


    public static void dumpXmlTests(Writer outWriter, TestCollection collection) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = schemaFactory.newSchema(DesignerUtil.getResource("testschema/rule-tests_1_0_0.xsd"));
            dbf.setSchema(schema);
            dbf.setNamespaceAware(true);
            DocumentBuilder builder = getDocumentBuilder(dbf);


            Document doc = builder.newDocument();
            Element root = doc.createElementNS(NS, "test-data");
            doc.appendChild(root);

            root.setAttribute("xmlns", NS);
            root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            root.setAttribute("xsi:schemaLocation", SCHEMA_LOCATION);

            new TestXmlDumper().appendTests(doc, collection.getStash());

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "{" + NS + "}code");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            // FIXME whatever i try this indents by 3 spaces which is not
            //  compatible with our style
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");


            transformer.transform(new DOMSource(doc), new StreamResult(outWriter));
        } finally {
            outWriter.close();
        }

    }


    private static DocumentBuilder getDocumentBuilder(DocumentBuilderFactory dbf) throws ParserConfigurationException {
        DocumentBuilder builder = dbf.newDocumentBuilder();
        builder.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) throws SAXException {
                throw exception;
            }


            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                throw exception;
            }


            @Override
            public void error(SAXParseException exception) throws SAXException {
                throw exception;
            }
        });
        return builder;
    }
}
