package com.crocs.oms.common.util;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

/*
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
*/
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class XMLUtil {

    private XMLUtil()
    {
        // prevents access default paramater-less constructor
    }

    /**
     * Creates a child element under the parent element with given child name. Returns the newly created child element.
     * This method returns null if either parent is null or child name is void.
     * @param parentEle parentElement
     * @param childName childName
     * @return Element
     */
    public static Element createChild(Element parentEle, String childName) {
        Element child = null;
        if (parentEle != null && !XMLUtil.isVoid(childName)) {
            child = parentEle.getOwnerDocument().createElement(childName);
            parentEle.appendChild(child);
        }
        return child;
    }


    /**
     * Gets the child element with the given name. If not found returns null.
     * This method returns null if either parent is null or child name is void.
     * @param parentEle parentEle
     * @param childName childName
     * @return Element
     */
	/*public static Element getChildElement(Element parentEle, String childName) {
		return XMLUtil.getChildElement(parentEle, childName, false);
	}*/



    /**
     * Gets the child element with the given name. If not found:
     *     1) a new element will be created if "createIfNotExists" is true.
     *     OR
     *     2) null will be returned if "createIfNotExists" is false.
     * This method returns null if either parent is null or child name is void.
     * 	@param parentEle parentEle
     *   @param childName childName
     *   @param createIfNotExists createIfNotExists flag
     *   @return Element
     */
	/*public static Element getChildElement(Element parentEle, String childName, boolean createIfNotExists) {

		Element child = null;
		if (parentEle != null && !XMLUtil.isVoid(childName)) {
			for (Node n = parentEle.getFirstChild(); n != null; n = n.getNextSibling()) {
				if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals(childName)) {
					return (Element) n;
				}
			}

			// Did not find the element, create it if createIfNotExists is true
			// else return null;
			if (createIfNotExists) {
				child = XMLUtil.createChild(parentEle, childName);

			}
		}
		return child;
	}*/

    /**
     * Utility method to check if a given object is void (just null check).
     * <p></p>
     * @param obj Object for void check.
     * @return true if the given object is null.
     * <p></p>
     */
    public static boolean isVoid(Object obj) {
        //return (obj == null) ? true : false;
        boolean retVal = false;
        if (obj == null) {
            retVal = true;

        }
        return retVal;
    }




    /**
     *
     * This method takes a document Element as input and returns the XML String.
     * @param element   a valid element object for which XML output in String form is required.
     * @return XML String of the given element
     */

	/*public static String getElementXMLString(Element element) {
		if (isVoid(element)){
			return null;
		} else {
			return XMLUtil.serialize(element);
		}
	}*/

    /**
     * This method takes Document as input and returns the XML String.
     * @param document   a valid document object for which XML output in String form is required.
     * @return String type
     */
    public static String getXMLString(Document docInput) {
        String strXMLString = null;
        if(null != docInput){
            strXMLString = XMLUtil.serialize(docInput);
        }
        return strXMLString;
    }
    /**
     * Return the sub elements with given name, as a List.
     * @param element element
     * @param nodeName nodeName
     * @return List
     */
    public static List getSubNodeList(Element element, String nodeName) {
        NodeList nodeList = element.getElementsByTagName(nodeName);
        List elemList = new ArrayList();
        for (int count = 0; count < nodeList.getLength(); count++) {
            elemList.add(nodeList.item(count));
        }
        return elemList;
    }


    /**
     *	Same as getSubNodeList().
     *	@see #getSubNodeList(Element, String).
     *  @param startElement  startElement
     *  @param elemName element Name
     *  @return List
     */
    public static List getElementsByTagName(Element startElement, String elemName) {
        NodeList nodeList = startElement.getElementsByTagName(elemName);
        List elemList = new ArrayList();
        for (int count = 0; count < nodeList.getLength(); count++) {
            elemList.add(nodeList.item(count));
        }
        return elemList;
    }
// Added some util method for Rest Client -Adyen
    public static List<Node> getElementListByXpath(Document docInXML, String sXPath) throws ParserConfigurationException, TransformerException {
        NodeList nodeList = null;
        List<Node> elementList = new ArrayList<>();
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            nodeList = (NodeList) xPath.evaluate(sXPath, docInXML, XPathConstants.NODESET);
        } catch (Exception e) {
            throw new TransformerException("Error evaluating XPath: " + sXPath, e);
        }
        int iNodeLength = nodeList.getLength();
        for (int iCount = 0; iCount < iNodeLength; iCount++) {
          Node node = nodeList.item(iCount);
          elementList.add(node);
        } 
        return elementList;
      }
    
    
    public static Element getElementByXPath(Document docInXML, String sXPath) throws TransformerException {
        Node node = null;
        Element eleNode = null;
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            node = (Node) xPath.evaluate(sXPath, docInXML, XPathConstants.NODE);
        } catch (Exception e) {
            throw new TransformerException("Error evaluating XPath: " + sXPath, e);
        }
        eleNode = (Element)node;
        return eleNode;
      }
 
    public static String getAttribute(Object oElement, String sAttributeName) {
        if (oElement != null) {
          String attributeValue = ((Element)oElement).getAttribute(sAttributeName);
          attributeValue = (attributeValue == null) ? attributeValue : attributeValue.trim();
          return attributeValue;
        } 
        return null;
      }

    
    public static Element getRootElement(Document docInXML) {
        return docInXML.getDocumentElement();
      }
    public static String getElementXMLString(Element element) {
        return serialize(element);
      }
    
    public static Document getDocumentFromElement(Element element) throws ParserConfigurationException, FactoryConfigurationError {
        Document doc = null;
        Node nImp = null;
        DocumentBuilder dbdr = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        doc = dbdr.newDocument();
        nImp = doc.importNode(element, true);
        doc.appendChild(nImp);
        return doc;
      }
    //Adyen end
    
    /**
     * Returns a formatted XML string for the Node, using encoding 'iso-8859-1'.
     *
     * @param node   a valid document object for which XML output in String form is required.
     *
     * @return the formatted XML string.
     */

    public static String serialize(Node node) {
        return XMLUtil.serialize(node, "iso-8859-1", true);
    }

    /**
     *	Return a XML string for a Node, with specified encoding and indenting flag.
     *	<p>
     *	<b>Note:</b> only serialize DOCUMENT_NODE, ELEMENT_NODE, and DOCUMENT_FRAGMENT_NODE
     *
     *	@param node the input node.
     *	@param encoding such as "UTF-8", "iso-8859-1"
     *	@param indenting indenting output or not.
     *
     *	@return the XML string
     */
    public static String serialize(Node node, String encoding, boolean indenting) {
        OutputFormat outFmt = null;
        StringWriter strWriter = null;
        XMLSerializer xmlSerializer = null;
        String retVal = null;

        try {
            outFmt = new OutputFormat("xml", encoding, indenting);
            outFmt.setOmitXMLDeclaration(true);
            strWriter = new StringWriter();

            xmlSerializer = new XMLSerializer(strWriter, outFmt);

            int ntype = node.getNodeType();

            switch(ntype) {
                case Node.DOCUMENT_FRAGMENT_NODE:
                    xmlSerializer.serialize((DocumentFragment) node);
                    break;
                case Node.DOCUMENT_NODE:
                    xmlSerializer.serialize((Document) node);
                    break;
                case Node.ELEMENT_NODE:
                    xmlSerializer.serialize((Element) node);
                    break;
                default: throw new IOException("Can serialize only Document, DocumentFragment and Element type nodes");
            }

            retVal = strWriter.toString();
        } catch (IOException e) {
            retVal = e.getMessage();
        } finally {
            try {
                //added for violation-Value is null and guaranteed to be dereferenced on exception path fix
                if(strWriter!=null)
                {
                    strWriter.close();
                }
            } catch (IOException ie) {
                retVal = ie.getMessage();

            }
        }

        return retVal;
    }
    
    public static Document nodeToDocument(Element element) throws Exception {
    	
    	Node rootOfNewDocument = element.getFirstChild();

    	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    	DocumentBuilder builder = factory.newDocumentBuilder();
    	Document responseDoc = builder.newDocument();
    	Node importedNode = responseDoc.importNode(rootOfNewDocument , true);
    	responseDoc.appendChild(importedNode);
    	
    	return responseDoc;
    	
    	
    }

}


