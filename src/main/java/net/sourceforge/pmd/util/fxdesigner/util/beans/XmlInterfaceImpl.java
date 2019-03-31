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

import net.sourceforge.pmd.util.fxdesigner.util.beans.converters.Serializer;
import net.sourceforge.pmd.util.fxdesigner.util.beans.converters.SerializerRegistrar;


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
    private static final String SCHEMA_PROPERTY_NAME = "name";
    private static final String SCHEMA_PROPERTY_VALUE = "value";
    private static final String SCHEMA_PROPERTY_TYPE = "type";
    private static final String SCHEMA_NULL_VALUE_FLAG = "nullValue";


    XmlInterfaceImpl(int revisionNumber) {
        super(revisionNumber);
    }


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
        String typeName = propertyElement.getAttribute(SCHEMA_PROPERTY_TYPE);
        String name = propertyElement.getAttribute(SCHEMA_PROPERTY_NAME);
        Type type = PropertyUtils.parseType(typeName);
        if (type == null) {
            System.out.println("Unable to parse " + typeName);
            return;
        }

        try {


            Serializer<Object> serializer = SerializerRegistrar.getInstance().getSerializer(type);
            if (serializer == null) {
                throw new IllegalStateException("Null serializer for type " + typeName);
            }
            Object value = serializer.fromXml(getChildrenByTagName(propertyElement, SCHEMA_PROPERTY_VALUE).get(0));

            owner.addProperty(name, value, type);
        } catch (Exception e) {
            String message = "Unable to parse property " + name + " for " + typeName;
            new IllegalStateException(message, e).printStackTrace();
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


        @Override
        public void visit(SimpleBeanModelNode node, Element parent) {
            Element nodeElement = parent.getOwnerDocument().createElement(SCHEMA_NODE_ELEMENT);
            nodeElement.setAttribute(SCHEMA_NODE_CLASS_ATTRIBUTE, node.getNodeType().getCanonicalName());

            Map<String, Type> settingsTypes = node.getSettingsTypes();

            for (Entry<String, Object> keyValue : node.getSettingsValues().entrySet()) {

                Type propertyType = settingsTypes.get(keyValue.getKey());
                @SuppressWarnings("unchecked")
                Serializer<Object> serializer = (Serializer<Object>) SerializerRegistrar.getInstance().getSerializer(propertyType);

                if (serializer == null) {
                    throw new IllegalStateException("No serializer registered for type " + propertyType);
                }

                Element valueElt;
                try {
                    valueElt = serializer.toXml(keyValue.getValue(), () -> parent.getOwnerDocument().createElement(SCHEMA_PROPERTY_VALUE));
                } catch (Exception e) {
                    String message = "Unable to serialize property "
                        + keyValue.getKey() + " for " + node.getNodeType().getName();
                    new IllegalStateException(message, e).printStackTrace();
                    continue;
                }

                Element setting = parent.getOwnerDocument().createElement(SCHEMA_PROPERTY_ELEMENT);
                setting.setAttribute(SCHEMA_PROPERTY_NAME, keyValue.getKey());
                setting.setAttribute(SCHEMA_PROPERTY_TYPE, propertyType.getTypeName());
                setting.appendChild(valueElt);
                nodeElement.appendChild(setting);
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
