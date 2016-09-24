/*	KeyMinder
 * Copyright (C) 2015-2016 Bastian Kraemer
 *
 * XML.java
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.akubix.keyminder.core.io;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.management.modelmbean.XMLParseException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import de.akubix.keyminder.util.Utilities;
import javafx.util.Pair;

/**
 * This class represents a collection of static methods and functions to deal with XML files, documents and nodes
 * @see Document
 * @see Node
 */
public final class XML{
	private XML(){}

	/**
	 * Creates a new, empty XML Document
	 * @param rootNodeText the name of the root element
	 * @return the new XML document
	 * @throws ParserConfigurationException (Should never occur)
	 */
	public static Document createXmlDocument(String rootNodeName) throws ParserConfigurationException {

		DocumentBuilder builder = getDocumentBuilder();

		Document xmldoc = builder.newDocument();
		xmldoc.appendChild(xmldoc.createElement(rootNodeName));
		return xmldoc;
	}

	/* XML Parser
	 * ==========================================================================================================================================================================
	 */

	/**
	 * Loads an XML-Document from a file
	 * @param xmlFile the XML document file
	 * @return the parsed XML document
	 * @throws ParserConfigurationException if the XML document could not be parsed
	 * @throws SAXException if the XML document could not be parsed
	 * @throws IOException if an IO error occurred
	 */
	public static Document loadXmlDocument(File xmlFile) throws SAXException, IOException {
		return loadXmlDocument(new FileInputStream(xmlFile));
	}

	/**
	 * Loads an XML-Document from an UTF-8 encoded string
	 * @param xml the XML document as string
	 * @return the parsed XML document
	 * @throws ParserConfigurationException if the XML document could not be parsed
	 * @throws SAXException if the XML document could not be parsed
	 * @throws IOException if an IO error occurred
	 */
	public static Document loadXmlDocument(String xml) throws SAXException, IOException {
		return loadXmlDocument(new ByteArrayInputStream(xml.getBytes("UTF8")));
	}

	/**
	 * Loads an XML-Document from an input stream
	 * @param in the input stream
	 * @return the parsed XML document
	 * @throws ParserConfigurationException if the XML document could not be parsed
	 * @throws SAXException if the XML document could not be parsed
	 * @throws IOException if an IO error occurred
	 */
	public static Document loadXmlDocument(InputStream in) throws SAXException, IOException {

		try{
			return getDocumentBuilder().parse(in);
		}
		catch(ParserConfigurationException e){
			throw new SAXException(e.getMessage(), e);
		}
		finally {
			closeStream(in);
		}
	}

	private static void closeStream(Closeable stream){
		try {
			if(stream != null){
				stream.close(); // try to close the stream in every case
			}
		} catch (IOException ioex) {}
	}

	private static DocumentBuilder getDocumentBuilder() throws ParserConfigurationException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		return factory.newDocumentBuilder();
	}

	/* XML Writer
	 * ==========================================================================================================================================================================
	 */

	/**
	 * Saves an XML-Document to an file.
	 * @param xmlfile the file the XML document will be stored in
	 * @param xmldoc the XML document that will be saved
	 * @throws TransformerException if the XML file can't be saved
	 * @throws IOException if there is any IO error
	 */
	public static void writeXmlDocumentToFile(File xmlfile, Document xmldoc) throws TransformerException, IOException {

		OutputStreamWriter outputStream = null;

		try {
			outputStream = new OutputStreamWriter(new FileOutputStream(xmlfile), Charset.forName("UTF-8").newEncoder());
			transformDocument(xmldoc, new StreamResult(outputStream));
		}
		finally {
			closeStream(outputStream);
		}
	}

	/**
	 * Converts an XML-Document to a String
	 * @param xmldoc The document, that should be converted
	 * @return XML-Document as String
	 * @throws TransformerException if the XML file can't be saved
	 */
	public static String writeXmlDocumentToString(Document xmldoc) throws TransformerException {

		StringWriter stringWriter =  new StringWriter();
		transformDocument(xmldoc, new StreamResult(stringWriter));
		return stringWriter.toString();
	}

	private static void transformDocument(Document xmldoc, Result result) throws TransformerException {

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();

		//transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

		xmldoc.setXmlStandalone(true);
		transformer.transform(new DOMSource(xmldoc), result);
	}

	/* Working with XML documents or nodes
	 * ==========================================================================================================================================================================
	 */

	/**
	 * Returns a HashMap containing all XML Attributes of this node as value key pair
	 * @param xmlnode the XML node for this operations
	 * @return the Map with all attributes
	 */
	public static Map<String, String> getXmlAttributesAsMap(org.w3c.dom.Node xmlnode) {

		Map<String, String> attribs = new HashMap<>();
		for(int i = 0; i < xmlnode.getAttributes().getLength(); i++){
			attribs.put(xmlnode.getAttributes().item(i).getNodeName(), xmlnode.getAttributes().item(i).getNodeValue());
		}
		return attribs;
	}

	/**
	 * Checks if a node has real child nodes and not just something like a {@link Node#TEXT_NODE}
	 * @param xmlnode the XML node that will be checked
	 * @return {@code true} it´has child nodes, {@code false} if not
	 */
	public static boolean xmlElementHasChildNodes(org.w3c.dom.Node xmlnode){
		for(int i = 0; i < xmlnode.getChildNodes().getLength(); i++){
			if(xmlnode.getChildNodes().item(i).getNodeType() == Node.ELEMENT_NODE){return true;}
		}

		return false;
	}

	/* Transform a hash (e.g. a HashMap) to XML
	 * ==========================================================================================================================================================================
	 */

	/**
	 * Converts a Map to a flat XML-Document with only one level and saves it in an specified file.
	 * @param source the data source
	 * @param parentXMLNode the parent XML node
	 */
	public static void convertMapToFlatXml(Map<String, String> source, org.w3c.dom.Node parentXmlNode){

		for(String key: source.keySet()){
			String val = source.get(key);
			if(!val.equals("")){
				org.w3c.dom.Node xmlNode = parentXmlNode.getOwnerDocument().createElement(key);
				xmlNode.setTextContent(source.get(key));
				parentXmlNode.appendChild(xmlNode);
			}
		}
	}

	/**
	 * Converts a Map to an XML-Document
	 * @param source the data source
	 * @param rootElementName the name of the root element
	 * @return the new XML document
	 */
	public static Document convertMapToXmlDocument(Map<String, String> source, String rootElementName){

		try{
			Document xmldoc = getDocumentBuilder().newDocument();
			xmldoc.appendChild(xmldoc.createElement(rootElementName));

			convertMapToXmlNodes(source, xmldoc.getDocumentElement());

			return xmldoc;
		} catch (ParserConfigurationException e) {
			return null;
		}
	}

	/**
	 * Converts a Map to into multiple XML-Nodes which will be added to the 'parentXmlNode'
	 * @param source the data source
	 * @param parentXMLNode the parent XML node
	 */
	public static void convertMapToXmlNodes(Map<String, String> source, org.w3c.dom.Node parentXmlNode){

		Map<String, org.w3c.dom.Node> nodeMap = new HashMap<>();

		for(String key: source.keySet()){
			StringBuilder prefix = new StringBuilder("");
			org.w3c.dom.Node xmlNode = parentXmlNode;
			for(String part: key.split("\\.")){
				xmlNode = getOrCreateXmlNode(nodeMap, prefix.toString(), part, xmlNode);
				prefix.append(part + ".");
			}

			xmlNode.setTextContent(source.get(key));
		}
	}

	private static org.w3c.dom.Node getOrCreateXmlNode(Map<String, org.w3c.dom.Node> nodeMap, String prefix, String nodeName, org.w3c.dom.Node parentXmlNode)	{
		if(nodeMap.containsKey(prefix + nodeName + ".")){
			return nodeMap.get(prefix + nodeName + ".");
		}
		else {

			org.w3c.dom.Node xmlnode;
			if(nodeName.contains(":")){

				Pair<String, String> p = Utilities.splitKeyAndValue(nodeName, ".+", ":", ".+");

				xmlnode = parentXmlNode.getOwnerDocument().createElement(p.getKey());
				org.w3c.dom.Attr attrib = parentXmlNode.getOwnerDocument().createAttribute("name");
				attrib.setNodeValue(p.getValue());
				xmlnode.getAttributes().setNamedItem(attrib);
			}
			else{
				xmlnode = parentXmlNode.getOwnerDocument().createElement(nodeName);
			}

			nodeMap.put(prefix + nodeName + ".", xmlnode);
			parentXmlNode.appendChild(xmlnode);
			return xmlnode;
		}
	}

	/* Transform an XML-File/XML-Document to a HashMap
	 * ==========================================================================================================================================================================
	 */

	/**
	 * Restores an hash from an XML-File, which has been created with on of the 'convertMapToXmlNodes()' functions
	 * @param parentXmlNode The parent XML node
	 * @param targetMap the target map
	 * @param enableOverwrite overwrite existing keys?
	 * @throws XMLParseException if the XML
	 */
	public static void convertXmlToMap(org.w3c.dom.Node parentXmlNode, Map<String, String> targetMap, boolean enableOverwrite) {
		extractNodesFromXml(parentXmlNode, "", targetMap, enableOverwrite ? (hash, key, value) -> hash.put(key, value) : (hash, key, value) -> hash.putIfAbsent(key, value));
	}

	/**
	 * Restores an hash from an XML-File, which has been created with on of the 'convertMapToXmlNodes()' functions
	 * @param Document the XML document
	 * @param targetMap the Map the information will be stored in
	 * @param prefix this prefix will be added to all map entries
	 * @param enableOverwrite use {@code true} if you want to enable overwriting of values
	 * @throws XMLParseException if something went wrong
	 */
	public static void convertXmlToMap(Document document, Map<String, String> targetMap, boolean enableOverwrite) {
		convertXmlToMap(document.getDocumentElement(), targetMap, enableOverwrite);
	}

	private static void extractNodesFromXml(org.w3c.dom.Node parentXMLNode, String prefix, Map<String, String> targetHash, HashStorageFunction<String> storefunction) {
		for(int i = 0; i < parentXMLNode.getChildNodes().getLength(); i++){
			Node currentNode = parentXMLNode.getChildNodes().item(i);
			if(currentNode.getNodeType() == Node.ELEMENT_NODE){
				if(xmlElementHasChildNodes(currentNode)){
					org.w3c.dom.Node attrib = currentNode.getAttributes().getNamedItem("name");
					if(attrib != null){
						extractNodesFromXml(currentNode, prefix + currentNode.getNodeName() + ":" + attrib.getNodeValue() + ".", targetHash, storefunction);
					}
					else{
						extractNodesFromXml(currentNode, prefix + currentNode.getNodeName() + ".", targetHash, storefunction);
					}
				}
				else{
					org.w3c.dom.Node attrib = currentNode.getAttributes().getNamedItem("name");
					storefunction.store(targetHash,
										(attrib == null) ? prefix + currentNode.getNodeName() : prefix + currentNode.getNodeName() + ":" + attrib.getNodeValue(),
										currentNode.getTextContent());
				}
			}
		}
	}
}

interface HashStorageFunction<T>
{
	public void store(Map<T, T> hash, T key, T value);
}
