/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.util.fxdesigner.model.ObservableRuleBuilder;
import net.sourceforge.pmd.util.fxdesigner.util.AuxLanguageRegistry;
import net.sourceforge.pmd.util.fxdesigner.util.DesignerUtil;
import net.sourceforge.pmd.util.fxdesigner.util.codearea.PmdCoordinatesSystem.TextRange;

public class TestXmlParser {


    private Map<LiveTestCase, Element> parseTests(Document doc, ObservableRuleBuilder owner) {
        Element root = doc.getDocumentElement();
        NodeList testCodes = root.getElementsByTagName("test-code");


        Map<LiveTestCase, Element> tests = new LinkedHashMap<>(testCodes.getLength());
        for (int i = 0; i < testCodes.getLength(); i++) {
            Element testCode = (Element) testCodes.item(i);

            LiveTestCase descriptor = parseSingle(testCode, root, owner);
            tests.put(descriptor, testCode);
        }
        return tests;
    }

    private LiveTestCase parseSingle(Element testCode, Element root, ObservableRuleBuilder owner) {
        //
        //        boolean reinitializeRule = true;
        //        Node reinitializeRuleAttribute = testCode.getAttributes().getNamedItem("reinitializeRule");
        //        if (reinitializeRuleAttribute != null) {
        //            String reinitializeRuleValue = reinitializeRuleAttribute.getNodeValue();
        //            if ("false".equalsIgnoreCase(reinitializeRuleValue) || "0".equalsIgnoreCase(reinitializeRuleValue)) {
        //                reinitializeRule = false;
        //            }
        //        }

        boolean isRegressionTest = true;
        Node regressionTestAttribute = testCode.getAttributes().getNamedItem("regressionTest");
        if (regressionTestAttribute != null) {
            String reinitializeRuleValue = regressionTestAttribute.getNodeValue();
            if ("false".equalsIgnoreCase(reinitializeRuleValue)) {
                isRegressionTest = false;
            }
        }
        //
        //        boolean isUseAuxClasspath = true;
        //        Node useAuxClasspathAttribute = testCode.getAttributes().getNamedItem("useAuxClasspath");
        //        if (useAuxClasspathAttribute != null) {
        //            String useAuxClasspathValue = useAuxClasspathAttribute.getNodeValue();
        //            if ("false".equalsIgnoreCase(useAuxClasspathValue)) {
        //                isUseAuxClasspath = false;
        //            }
        //        }

        NodeList ruleProperties = testCode.getElementsByTagName("rule-property");
        Properties properties = new Properties();
        for (int j = 0; j < ruleProperties.getLength(); j++) {
            Node ruleProperty = ruleProperties.item(j);
            String propertyName = ruleProperty.getAttributes().getNamedItem("name").getNodeValue();
            properties.setProperty(propertyName, parseTextNode(ruleProperty));
        }

        NodeList expectedMessagesNodes = testCode.getElementsByTagName("expected-messages");
        List<String> messages = new ArrayList<>();
        if (expectedMessagesNodes != null && expectedMessagesNodes.getLength() > 0) {
            Element item = (Element) expectedMessagesNodes.item(0);
            NodeList messagesNodes = item.getElementsByTagName("message");
            for (int j = 0; j < messagesNodes.getLength(); j++) {
                messages.add(parseTextNode(messagesNodes.item(j)));
            }
        }

        NodeList expectedLineNumbersNodes = testCode.getElementsByTagName("expected-linenumbers");
        List<Integer> expectedLineNumbers = new ArrayList<>();
        if (expectedLineNumbersNodes != null && expectedLineNumbersNodes.getLength() > 0) {
            Element item = (Element) expectedLineNumbersNodes.item(0);
            String numbers = item.getTextContent();
            for (String n : numbers.split(" *, *")) {
                expectedLineNumbers.add(Integer.valueOf(n));
            }
        }

        String code = getNodeValue(testCode, "code", false);
        if (code == null) {
            // Should have a coderef
            NodeList coderefs = testCode.getElementsByTagName("code-ref");
            if (coderefs.getLength() == 0) {
                throw new RuntimeException(
                    "Required tag is missing from the test-xml. Supply either a code or a code-ref tag");
            }
            Node coderef = coderefs.item(0);
            String referenceId = coderef.getAttributes().getNamedItem("id").getNodeValue();
            NodeList codeFragments = root.getElementsByTagName("code-fragment");
            for (int j = 0; j < codeFragments.getLength(); j++) {
                String fragmentId = codeFragments.item(j).getAttributes().getNamedItem("id").getNodeValue();
                if (referenceId.equals(fragmentId)) {
                    code = parseTextNode(codeFragments.item(j));
                }
            }

            if (code == null) {
                throw new RuntimeException("No matching code fragment found for coderef");
            }
        }

        String description = getNodeValue(testCode, "description", true);
        int expectedProblems = Integer.parseInt(getNodeValue(testCode, "expected-problems", true));

        String languageVersionString = getNodeValue(testCode, "source-type", false);
        LanguageVersion languageVersion = null;
        if (languageVersionString != null) {
            languageVersion = AuxLanguageRegistry.findLanguageVersionByTerseName(languageVersionString);
            if (languageVersion == null) {
                throw new RuntimeException("Unknown LanguageVersion for test: " + languageVersionString);
            }
        }

        return fromDescriptor(
            code,
            description,
            expectedProblems,
            languageVersion,
            !isRegressionTest,
            messages,
            expectedLineNumbers,
            properties,
            owner
        );
    }


    private static LiveTestCase fromDescriptor(
        String code,
        String description,
        int expectedProblems,
        @Nullable LanguageVersion version,
        boolean ignored,
        List<String> messages,
        List<Integer> lineNumbers,
        Properties properties,
        ObservableRuleBuilder owner
    ) {

        LiveTestCase live = new LiveTestCase();
        live.setRule(owner);
        live.setSource(code);
        live.setDescription(description);
        live.setLanguageVersion(version);
        live.setIgnored(ignored);

        List<String> lines = Arrays.asList(code.split("\\r?\\n"));

        for (int i = 0; i < expectedProblems; i++) {
            String m = messages.size() > i ? messages.get(i) : null;
            int line = lineNumbers.size() > i ? lineNumbers.get(i) : -1;

            TextRange tr = line >= 0
                           ? TextRange.fullLine(line, lines.get(line - 1).length())
                           : null;

            live.getExpectedViolations().add(new LiveViolationRecord(tr, m, false));
        }
        properties.forEach((k, v) -> live.setProperty(k.toString(), v.toString()));
        return live;
    }

    private String getNodeValue(Element parentElm, String nodeName, boolean required) {
        NodeList nodes = parentElm.getElementsByTagName(nodeName);
        if (nodes == null || nodes.getLength() == 0) {
            if (required) {
                throw new RuntimeException("Required tag is missing from the test-xml: " + nodeName);
            } else {
                return null;
            }
        }
        Node node = nodes.item(0);
        return parseTextNode(node);
    }

    private String parseTextNode(Node exampleNode) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < exampleNode.getChildNodes().getLength(); i++) {
            Node node = exampleNode.getChildNodes().item(i);
            if (node.getNodeType() == Node.CDATA_SECTION_NODE || node.getNodeType() == Node.TEXT_NODE) {
                buffer.append(node.getNodeValue());
            }
        }
        return buffer.toString().trim();
    }


    public static TestCollection parseXmlTests(String xml, ObservableRuleBuilder owner) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        return parseXmlTests(bis, owner);
    }


    public static TestCollection parseXmlTests(Path path, ObservableRuleBuilder owner) throws Exception {
        try (InputStream is = Files.newInputStream(path)) {
            return parseXmlTests(is, owner);
        }
    }

    private static TestCollection parseXmlTests(InputStream is, ObservableRuleBuilder owner) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        Schema schema = schemaFactory.newSchema(DesignerUtil.getResource("testschema/rule-tests_1_0_0.xsd"));
        dbf.setSchema(schema);
        dbf.setNamespaceAware(true);
        DocumentBuilder builder = getDocumentBuilder(dbf);

        Document doc = builder.parse(is);
        Map<LiveTestCase, Element> testDescriptors = new TestXmlParser().parseTests(doc, owner);
        List<LiveTestCase> tests = new ArrayList<>(testDescriptors.keySet());
        return new TestCollection(null, tests);


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
