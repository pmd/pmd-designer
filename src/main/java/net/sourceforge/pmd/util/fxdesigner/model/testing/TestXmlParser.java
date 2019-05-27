/*
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.model.testing;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import net.sourceforge.pmd.Rule;
import net.sourceforge.pmd.RuleContext;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.rule.AbstractRule;
import net.sourceforge.pmd.testframework.RuleTst;
import net.sourceforge.pmd.testframework.TestDescriptor;

public class TestXmlParser {


    private List<TestDescriptor> parseTests(Rule rule, Document doc, Consumer<Exception> errorHandler) {
        Element root = doc.getDocumentElement();
        NodeList testCodes = root.getElementsByTagName("test-code");


        List<TestDescriptor> tests = new ArrayList<>(testCodes.getLength());
        for (int i = 0; i < testCodes.getLength(); i++) {
            Element testCode = (Element) testCodes.item(i);

            try {
                TestDescriptor descriptor = parseSingle(rule, testCode, root);
                descriptor.setNumberInDocument(i);
                tests.add(descriptor);
            } catch (Exception e) {
                errorHandler.accept(new RuntimeException("Exception while parsing test #" + i, e));
            }
        }
        return tests;
    }

    private TestDescriptor parseSingle(Rule rule, Element testCode, Element root) {

        boolean reinitializeRule = true;
        Node reinitializeRuleAttribute = testCode.getAttributes().getNamedItem("reinitializeRule");
        if (reinitializeRuleAttribute != null) {
            String reinitializeRuleValue = reinitializeRuleAttribute.getNodeValue();
            if ("false".equalsIgnoreCase(reinitializeRuleValue) || "0".equalsIgnoreCase(reinitializeRuleValue)) {
                reinitializeRule = false;
            }
        }

        boolean isRegressionTest = true;
        Node regressionTestAttribute = testCode.getAttributes().getNamedItem("regressionTest");
        if (regressionTestAttribute != null) {
            String reinitializeRuleValue = regressionTestAttribute.getNodeValue();
            if ("false".equalsIgnoreCase(reinitializeRuleValue)) {
                isRegressionTest = false;
            }
        }

        boolean isUseAuxClasspath = true;
        Node useAuxClasspathAttribute = testCode.getAttributes().getNamedItem("useAuxClasspath");
        if (useAuxClasspathAttribute != null) {
            String useAuxClasspathValue = useAuxClasspathAttribute.getNodeValue();
            if ("false".equalsIgnoreCase(useAuxClasspathValue)) {
                isUseAuxClasspath = false;
            }
        }

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

        TestDescriptor descriptor;

        String languageVersionString = getNodeValue(testCode, "source-type", false);
        LanguageVersion languageVersion = null;
        if (languageVersionString != null) {
            languageVersion = LanguageRegistry.findLanguageVersionByTerseName(languageVersionString);
            if (languageVersion == null) {
                throw new RuntimeException("Unknown LanguageVersion for test: " + languageVersionString);
            }
        }
        descriptor = new TestDescriptor(code, description, expectedProblems, rule, languageVersion);
        descriptor.setReinitializeRule(reinitializeRule);
        descriptor.setRegressionTest(isRegressionTest);
        descriptor.setUseAuxClasspath(isUseAuxClasspath);
        descriptor.setExpectedMessages(messages);
        descriptor.setExpectedLineNumbers(expectedLineNumbers);
        descriptor.setProperties(properties);
        return descriptor;
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


    public static TestCollection parseXmlTests(String xml, Consumer<Exception> errorHandler) {
        ByteArrayInputStream bis = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        return parseXmlTests(bis, errorHandler);
    }


    public static TestCollection parseXmlTests(Path path, Consumer<Exception> errorHandler) {
        try (FileInputStream is = new FileInputStream(path.toFile())) {
            return parseXmlTests(is, errorHandler);
        } catch (IOException e) {
            errorHandler.accept(e);
            return new TestCollection(Collections.emptyList());
        }
    }

    public static TestCollection parseXmlTests(InputStream is, Consumer<Exception> errorHandler) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            Schema schema = schemaFactory.newSchema(RuleTst.class.getResource("/rule-tests_1_0_0.xsd"));
            dbf.setSchema(schema);
            dbf.setNamespaceAware(true);
            DocumentBuilder builder = getDocumentBuilder(dbf);

            Document doc = builder.parse(is);
            Rule rule = new AbstractRule() {
                @Override
                public void apply(List<? extends net.sourceforge.pmd.lang.ast.Node> list, RuleContext ruleContext) {
                    // do nothing
                }
            };
            List<TestDescriptor> testDescriptors = new TestXmlParser().parseTests(rule, doc, errorHandler);
            List<LiveTestCase> tests = testDescriptors.stream().map(LiveTestCase::fromDescriptor).collect(Collectors.toList());
            return new TestCollection(tests);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            errorHandler.accept(e);
            return new TestCollection(Collections.emptyList());
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                errorHandler.accept(e);
            }
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
