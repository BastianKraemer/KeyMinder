/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	XMLCore.java

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.akubix.keyminder.lib;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.management.modelmbean.XMLParseException;
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
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javafx.util.Pair;

/**
 * This class represents a collection of static methods and functions to simply deal with XML files, documents and nodes
 * @see Document
 * @see Node
 */
public class XMLCore{
	private XMLCore(){}

	/* Create an XML-Document
	 * ==========================================================================================================================================================================
	 */

	/**
	 * Creates a new, empty XML Document
	 * @param rootNodeText the name of the root element
	 * @return the new XML document
	 * @throws ParserConfigurationException (Should never occur)
	 */
	public static Document createEmptyXMLDocument(String rootNodeText) throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;

		builder = factory.newDocumentBuilder();

		Document xmldoc = builder.newDocument();
		xmldoc.appendChild(xmldoc.createElement(rootNodeText));
		return xmldoc;
	}

	/* XML Parser
	 * ==========================================================================================================================================================================
	 */

	/**
	 * Load an XML-Document from a file
	 * @param xmlFile the XML document file
	 * @return the parsed XML document
	 * @throws ParserConfigurationException if the XML document could not be parsed
	 * @throws SAXException if the XML document could not be parsed
	 * @throws IOException if there is any IO error
	 */
	public static Document loadDocumentFromFile(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
		FileInputStream fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(xmlFile);

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder;

			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(fileInputStream);
			fileInputStream.close();
			return doc;

		} catch (ParserConfigurationException | IOException | SAXException e) {

			try {
				if(fileInputStream != null){fileInputStream.close();} //try to close the stream
			} catch (IOException ioex) {}

			throw e;
		}
	}

	/**
	 * Load an XML-Document from an input stream
	 * @param in the input stream
	 * @return the parsed XML document
	 * @throws ParserConfigurationException if the XML document could not be parsed
	 * @throws SAXException if the XML document could not be parsed
	 * @throws IOException if there is any IO error
	 */
	public static Document loadDocumentFromStream(InputStream in) throws ParserConfigurationException, SAXException, IOException
	{
		try{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder;

			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(in);
			in.close();
			return doc;

		}
		catch (ParserConfigurationException | IOException | SAXException e) {
			try{
				if(in != null){in.close();} //try to close the stream
			} catch (IOException ioex) {}

			throw e;
		}
	}

	/**
	 * Load an XML-Document from a String
	 * @param xml the XML document as String
	 * @return the parsed XML document
	 * @throws ParserConfigurationException if the XML document could not be parsed
	 * @throws SAXException if the XML document could not be parsed
	 * @throws IOException if there is any IO error
	 */
	public static Document loadDocumentFromString(String xml) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();

		return builder.parse(new ByteArrayInputStream(xml.getBytes("UTF8")));
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
	public static void saveXMLDocument(File xmlfile, Document xmldoc) throws TransformerException, IOException {
		// Use a Transformer for output
		TransformerFactory tFactory =TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();

		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

		DOMSource source = new DOMSource(xmldoc);
		OutputStreamWriter outputStream = new OutputStreamWriter(new FileOutputStream(xmlfile), Charset.forName("UTF-8").newEncoder());

		StreamResult result = new StreamResult(outputStream);
		transformer.transform(source, result);

		outputStream.close();
	}

	/**
	 * Converts an XML-Document to a String
	 * @param xmldoc The document, that should be converted
	 * @return XML-Document as String
	 * @throws TransformerException if the XML file can't be saved
	 */
	public static String transfromXMLDocumentToString(Document xmldoc) throws TransformerException {
		DOMSource domSource = new DOMSource(xmldoc);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		//transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
		java.io.StringWriter sw = new java.io.StringWriter();
		StreamResult sr = new StreamResult(sw);
		transformer.transform(domSource, sr);
		return sw.toString();
	}

	/* Working with XML documents or nodes
	 * ==========================================================================================================================================================================
	 */

	/**
	 * Returns a HashMap containing all XML Attributes of this node as value key pair
	 * @param xmlnode the XML node for this operations
	 * @return the Map with all attributes
	 */
	public static Map<String, String> xmlAttributes2Hash(org.w3c.dom.Node xmlnode) {
		Map<String, String> attribs = new HashMap<String, String>();
		for(int i = 0; i < xmlnode.getAttributes().getLength(); i++){
			attribs.put(xmlnode.getAttributes().item(i).getNodeName(), xmlnode.getAttributes().item(i).getNodeValue());
		}
		return attribs;
	}

	/**
	 * CHecks if a node has real child nodes and not just something like a {@link Node#TEXT_NODE}
	 * @param xmlnode the XML node that will be checked
	 * @return {@code true} it´has child nodes, {@code false} if not
	 */
	public static boolean hasChildNodes(org.w3c.dom.Node xmlnode){
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
	 * @param xmlfile the file the data will be stored in
	 * @param hash the data source
	 * @param rootElementName the name for the XML root element
	 * @throws TransformerException if somthing went wrong
	 */
	public static void map2FlatXML(File xmlfile, Map<String, String> hash, String rootElementName) throws TransformerException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			Document xmldoc = builder.newDocument();
			xmldoc.appendChild(xmldoc.createElement(rootElementName));

			map2FlatXMLNodes(xmldoc.getDocumentElement(), hash);

			saveXMLDocument(xmlfile, xmldoc);

		} catch (ParserConfigurationException | IOException e) {
			throw new TransformerException(e);
		}
	}

	/**
	 * Converts a Map to a flat XML-Document with only one level and saves it in an specified file.
	 * @param parentXMLNode the parent XML node
	 * @param source the data source
	 */
	public static void map2FlatXMLNodes(org.w3c.dom.Node parentXMLNode, Map<String, String> source){
		for(String key: source.keySet()){
			String val = source.get(key);
			if(!val.equals("")){
				org.w3c.dom.Node xmlNode = parentXMLNode.getOwnerDocument().createElement(key);
				xmlNode.setTextContent(source.get(key));
				parentXMLNode.appendChild(xmlNode);
			}
		}
	}

	/**
	 * Converts a Map to an XML-Document
	 * @param source the data source
	 * @param rootElementName the name of the root element
	 * @return the new XML document
	 */
	public static Document convertMapToXMLDocument(Map<String, String> source, String rootElementName){
		try{
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			Document xmldoc = builder.newDocument();
			xmldoc.appendChild(xmldoc.createElement(rootElementName));

			convertHashToXMLNodes(xmldoc.getDocumentElement(), source);

			return xmldoc;
		} catch (ParserConfigurationException e) {
			return null;
		}
	}

	/**
	 * Converts a Map to an XML-Document and saves it in an specified file.
	 * @param xmlfile the XML file
	 * @param source the data source
	 * @param rootElementName the name of the root element
	 */
	public static void saveMapAsXMLFile(File xmlfile, Map<String, String> source, String rootElementName){
		try {
			saveXMLDocument(xmlfile, convertMapToXMLDocument(source, rootElementName));
		} catch (TransformerException | IOException e) {
			System.out.println("ERROR: Unable to save a Hash as XML. Errormessage: " + e.getMessage());
		}
	}

	/**
	 * Converts a Map to into multiple XML-Nodes which will be added to the 'parentXMLNode'
	 * @param parentXMLNode the parent XML node
	 * @param source the data source
	 */
	public static void convertHashToXMLNodes(org.w3c.dom.Node parentXMLNode, Map<String, String> source){
		Map<String, org.w3c.dom.Node> nodeMap = new HashMap<>();

		for(String key: source.keySet()){
			String[] splitstr = key.split("\\.");
			StringBuilder prefix = new StringBuilder("");
			org.w3c.dom.Node xmlNode = parentXMLNode;
			for(String part: splitstr){
				xmlNode = hash2XML_getHashNode(nodeMap, prefix.toString(), part, xmlNode);
				prefix.append(part + ".");
			}

			xmlNode.setTextContent(source.get(key));
		}
	}

	private static org.w3c.dom.Node hash2XML_getHashNode(Map<String, org.w3c.dom.Node> nodeMap, String prefix, String nodename, org.w3c.dom.Node parentXMLNode)
	{
		if(nodeMap.containsKey(prefix + nodename + ".")){
			return nodeMap.get(prefix + nodename + ".");
		}
		else{
			org.w3c.dom.Node xmlnode;
			if(nodename.contains(":")){

				Pair<String, String> p = Tools.splitKeyAndValue(nodename, ".+", ":", ".+");

				xmlnode = parentXMLNode.getOwnerDocument().createElement(p.getKey());
				org.w3c.dom.Attr attrib = parentXMLNode.getOwnerDocument().createAttribute("name");
				attrib.setNodeValue(p.getValue());
				xmlnode.getAttributes().setNamedItem(attrib);
			}
			else{
				xmlnode = parentXMLNode.getOwnerDocument().createElement(nodename);
			}

			nodeMap.put(prefix + nodename + ".", xmlnode);
			parentXMLNode.appendChild(xmlnode);
			return xmlnode;
		}
	}

	/* Transform an XML-File/XML-Document to a HashMap
	 * ==========================================================================================================================================================================
	 */

	/**
	 * Restores an hash from an XML-File, which has been created with on of the 'map2XML()' functions
	 * @param xmlFile the XML file
	 * @param targetMap the Map the information will be stored in
	 * @param enableOverwrite use {@code true} if you want to enable overwriting of values
	 * @throws XMLParseException if something went wrong
	 */
	public static void xml2Map(File xmlFile, Map<String, String> targetMap, boolean enableOverwrite) throws XMLParseException {
		xml2Map(xmlFile, targetMap, enableOverwrite ? (hash, key, value) -> hash.put(key, value) : (hash, key, value) -> hash.putIfAbsent(key, value));
	}

	private static void xml2Map(File xmlFile, Map<String, String> targetMap, HashStorageFunction<String> storefunction) throws XMLParseException {
		if(xmlFile.exists()){
			try{
				FileInputStream xmlInputStream = new FileInputStream(xmlFile);
				try{
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder;

					builder = factory.newDocumentBuilder();
					Document document = builder.parse(xmlInputStream);

					xml2Map(document.getDocumentElement(), "", targetMap, storefunction);

					} catch (SAXException e) {
						xmlInputStream.close();
						throw new XMLParseException(e.getMessage());
					} catch (ParserConfigurationException e) {
						xmlInputStream.close();
						throw new XMLParseException(e.getMessage());
					}
			} catch (IOException e) {
				throw new XMLParseException(e.getMessage());
			}
		}
	}

	/**
	 * Restores a Map from an XML node
	 * @param parentXMLNode the parent XML node
	 * @param prefix this prefix will be added to all map entries
	 * @param targetHash the Map the information will be stored in
	 */
	public static void xml2Map(org.w3c.dom.Node parentXMLNode, String prefix, Map<String, String> targetHash){
			xml2Map(parentXMLNode, prefix, targetHash, (hash, key, value) -> hash.put(key, value));
	}

	/**
	 * Restores an hash from an XML-Node
	 * @param parentXMLNode the parent XML node
	 * @param prefix this prefix will be added to all map entries
	 * @param targetHash the Map the information will be stored in
	 * @param enableOverwrite use {@code true} if you want to enable overwriting of values
	 */
	public static void xml2Map(org.w3c.dom.Node parentXMLNode, String prefix, Map<String, String> targetHash, boolean enableOverwrite){
			xml2Map(parentXMLNode, prefix , targetHash,  enableOverwrite ? (hash, key, value) -> hash.put(key, value) : (hash, key, value) -> hash.putIfAbsent(key, value));
	}

	private static void xml2Map(org.w3c.dom.Node parentXMLNode, String prefix, Map<String, String> targetHash, HashStorageFunction<String> storefunction){
		for(int i = 0; i < parentXMLNode.getChildNodes().getLength(); i++){
			Node currentNode = parentXMLNode.getChildNodes().item(i);
			if(currentNode.getNodeType() == Node.ELEMENT_NODE){
				if(hasChildNodes(currentNode)){
					org.w3c.dom.Node attrib = currentNode.getAttributes().getNamedItem("name");
					if(attrib != null){
						xml2Map(currentNode, prefix + currentNode.getNodeName() + ":" + attrib.getNodeValue() + ".", targetHash, storefunction);
					}
					else{
						xml2Map(currentNode, prefix + currentNode.getNodeName() + ".", targetHash, storefunction);
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
