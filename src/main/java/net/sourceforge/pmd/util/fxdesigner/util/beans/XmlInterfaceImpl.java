/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.util.fxdesigner.util.beans;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sourceforge.pmd.util.fxdesigner.util.beans.converters.PropertyValue;
import net.sourceforge.pmd.util.fxdesigner.util.beans.converters.Serializer;
import net.sourceforge.pmd.util.fxdesigner.util.beans.converters.SerializerRegistrar;
import net.sourceforge.pmd.util.fxdesigner.util.beans.converters.TypedObject;


/**
 * Implementation of {@link XmlInterface}.
 *
 * @author Clément Fournier
 * @since 6.1.0
 * @since 6.14.0 (V2, no beanutils)
 */
public class XmlInterfaceImpl extends XmlInterface {

    private static final Logger LOGGER = Logger.getLogger(XmlInterface.class.getName());

    // names used in the Xml schema
    private static final String SCHEMA_NODE_ELEMENT = "node";
    private static final String SCHEMA_NODESEQ_ELEMENT = "nodeseq";
    private static final String SCHEMA_NODE_CLASS_ATTRIBUTE = "class";
    private static final String SCHEMA_PROPERTY_ELEMENT = "property";
    private static final String SCHEMA_PROPERTY_NAME = "property-name";
    private static final String SCHEMA_PROPERTY_VALUE = "value";


    XmlInterfaceImpl(int revisionNumber) {
        super(revisionNumber);
    }

    private final Serializer<TypedObject<?>> serializer = SerializerRegistrar.getInstance().compositeSerializer();


    private List<Element> getChildrenByTagName(Element element, String tagName) {
        NodeList children = element.getChildNodes();
        List<Element> elts = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE && tagName.equals(children.item(i).getNodeName())) {
                elts.add((Element) children.item(i));
            }
        }

        return elts;
    }


    @Override
    protected SimpleBeanModelNode parseSettingsOwnerNode(Element nodeElement) {
        Class<?> clazz;
        try {
            clazz = Class.forName(nodeElement.getAttribute(SCHEMA_NODE_CLASS_ATTRIBUTE));
        } catch (ClassNotFoundException e) {
            return null;
        }

        SimpleBeanModelNode node = new SimpleBeanModelNode(clazz);

        for (Element setting : getChildrenByTagName(nodeElement, SCHEMA_PROPERTY_ELEMENT)) {
            parseSingleProperty(setting, node);
        }

        for (Element child : getChildrenByTagName(nodeElement, SCHEMA_NODE_ELEMENT)) {
            try {
                Class<?> childType = Class.forName(child.getAttribute(SCHEMA_NODE_CLASS_ATTRIBUTE));
                if (node.getChildrenByType().get(childType) == null) { // FIXME
                    node.addChild(parseSettingsOwnerNode(child));
                }
            } catch (ClassNotFoundException e) {
                LOGGER.warning("Ignoring unknown settings node of type " + child.getAttribute(SCHEMA_NODE_CLASS_ATTRIBUTE));
            }
        }

        for (Element seq : getChildrenByTagName(nodeElement, SCHEMA_NODESEQ_ELEMENT)) {
            parseNodeSeq(seq, node);
        }

        return node;
    }


    private void parseSingleProperty(Element propertyElement, SimpleBeanModelNode owner) {
        String name = propertyElement.getAttribute(SCHEMA_PROPERTY_NAME);
        try {
            TypedObject<?> value = serializer.fromXml(getChildrenByTagName(propertyElement, SCHEMA_PROPERTY_VALUE).get(0));
            owner.addProperty(name, value.getObject(), value.getType());
        } catch (Exception e) {
            new RuntimeException(e).printStackTrace();
        }
    }


    private void parseNodeSeq(Element nodeSeq, SimpleBeanModelNode parent) {
        BeanModelNodeSeq<SimpleBeanModelNode> built = new BeanModelNodeSeq<>(nodeSeq.getAttribute(SCHEMA_PROPERTY_NAME));
        for (Element child : getChildrenByTagName(nodeSeq, SCHEMA_NODE_ELEMENT)) {
            built.addChild(parseSettingsOwnerNode(child));
        }
        parent.addChild(built);
    }


    @Override
    protected BeanNodeVisitor<Element> getDocumentMakerVisitor() {
        return new DocumentMakerVisitor();
    }


    public static class DocumentMakerVisitor extends BeanNodeVisitor<Element> {

        private final Serializer<TypedObject<?>> serializer = SerializerRegistrar.getInstance().compositeSerializer();

        @Override
        public void visit(SimpleBeanModelNode node, Element parent) {
            Element nodeElement = parent.getOwnerDocument().createElement(SCHEMA_NODE_ELEMENT);
            nodeElement.setAttribute(SCHEMA_NODE_CLASS_ATTRIBUTE, node.getNodeType().getCanonicalName());

            Map<String, Type> settingsTypes = node.getSettingsTypes();

            for (Entry<String, Object> keyValue : node.getSettingsValues().entrySet()) {


                try {

                    TypedObject object = new PropertyValue(keyValue.getKey(),
                                                           node.getNodeType().getName(),
                                                           keyValue.getValue(),
                                                           settingsTypes.get(keyValue.getKey()));

                    Element valueElt = serializer.toXml(object, () -> parent.getOwnerDocument().createElement(SCHEMA_PROPERTY_VALUE));
                    Element propertyElement = parent.getOwnerDocument().createElement(SCHEMA_PROPERTY_ELEMENT);
                    propertyElement.setAttribute(SCHEMA_PROPERTY_NAME, keyValue.getKey());
                    propertyElement.appendChild(valueElt);
                    nodeElement.appendChild(propertyElement);
                } catch (Exception e) {
                    // print it, but don't throw it
                    new RuntimeException(e).printStackTrace();
                }
            }

            parent.appendChild(nodeElement);
            super.visit(node, nodeElement);
        }


        @Override
        public void visit(BeanModelNodeSeq<?> node, Element parent) {
            Element nodeElement = parent.getOwnerDocument().createElement(SCHEMA_NODESEQ_ELEMENT);
            nodeElement.setAttribute(SCHEMA_PROPERTY_NAME, node.getPropertyName());
            parent.appendChild(nodeElement);
            super.visit(node, nodeElement);
        }
    }
}
