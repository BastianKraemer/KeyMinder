/*	KeyMinder
 * Copyright (C) 2015-2016 Bastian Kraemer
 *
 * KeyMindFileHandler.java
 *
 * KeyMinder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * KeyMinder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with KeyMinder.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.akubix.keyminder.core.io;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;

import javax.management.modelmbean.XMLParseException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.FileConfiguration;
import de.akubix.keyminder.core.encryption.AES;
import de.akubix.keyminder.core.encryption.EncryptionManager;
import de.akubix.keyminder.core.exceptions.StorageException;
import de.akubix.keyminder.core.exceptions.StorageExceptionType;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.core.tree.DefaultTreeNode;
import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.core.tree.TreeStore;
import de.akubix.keyminder.locale.LocaleLoader;

public class KeyMindFileHandler implements StorageHandler {

	private static final String latestFileFormatVersion = "1.2.0";
	private String fileType;

	// This flag is set to true if a a file with version 1.0 or 1.1 is opened
	// Prior to KeyMinder 0.3 a node identifier was an integer values
	private boolean compatibilityRegenerateNodeIds = false;

	public KeyMindFileHandler(String forFileType){
		this.fileType = forFileType;
	}

	@Override
	public FileConfiguration open(File xmlFile, String filePassword, Object tree, ApplicationInstance instance) throws StorageException {
		if(!xmlFile.exists()){throw new StorageException(StorageExceptionType.FileNotFound, "File not found.");}

		try {
			return readXMLFrame(xmlFile, filePassword, instance.getTree(), instance);
		} catch (UserCanceledOperationException e) {
			throw new StorageException(StorageExceptionType.UserCancelation, e.getMessage());
		} catch (XMLParseException e) {
			throw new StorageException(StorageExceptionType.ParserException, e.getMessage());
		}
	}

	@Override
	public void save(FileConfiguration file, TreeStore tree, ApplicationInstance instance) throws StorageException {
		try {
			XML.writeXmlDocumentToFile(file.getFilepath(), createXMLFrame(tree.getRootNode(), file));
			file.setFileFormatVersion(latestFileFormatVersion);
		} catch (InvalidKeyException e) {
			throw new StorageException(StorageExceptionType.UnknownException, e.getMessage());
		} catch (IOException e) {
			throw new StorageException(StorageExceptionType.IOException, e.getMessage());
		} catch (TransformerException e) {
			throw new StorageException(StorageExceptionType.ParserException, e.getMessage());
		} catch (XMLParseException e) {
			throw new StorageException(StorageExceptionType.ParserException, e.getMessage());
		}
	}

	/*
	 * ==============================================================================================================================================
	 * Load file
	 * ==============================================================================================================================================
	 */

	private FileConfiguration readXMLFrame(File xmlFile, String filepassword, TreeStore tree, ApplicationInstance app) throws XMLParseException, UserCanceledOperationException, StorageException {
		String fileVersion = latestFileFormatVersion; // Will be overwritten by the value in the file (if available)
		boolean fileIsEncrypted;
		Map<String, String> fileAttributes = new HashMap<>();
		Map<String, String> fileSettings = new HashMap<>();

		try {
			Document document = XML.loadXmlDocument(xmlFile);

			// Read the version from the XML root node
			Map<String, String> rootAttribs = XML.getXmlAttributesAsMap(document.getDocumentElement());
			if(rootAttribs.containsKey("version")){
				String version = rootAttribs.get("version");
				if(!version.matches("1.[0-2]($|.(.*))")){throw new StorageException(StorageExceptionType.UnsupportedVersion, "Document version not supported.");}

				if(version.matches("1.[0-1]($|.(.*))")){
					this.compatibilityRegenerateNodeIds = true;
				}
			}

			if(document.getDocumentElement().getChildNodes().getLength() >= 2){
				int nodeCount = 0;
				org.w3c.dom.Node headNode = null;
				org.w3c.dom.Node dataNode = null;

				for(int i = 0; i < document.getDocumentElement().getChildNodes().getLength(); i++)
				{
					org.w3c.dom.Node currentNode = document.getDocumentElement().getChildNodes().item(i);
					if(currentNode.getNodeType() == Node.ELEMENT_NODE){
						if(nodeCount == 0){
							// This is the first node, which is NOT a TextNode
							headNode = currentNode;
							nodeCount++;
						}
						else{
							// This is the second node, which is NOT a TextNode
							// A correct KeyMind-XML File does not have more nodes (this case is not handled, all other nodes will be ignored)
							dataNode = currentNode;
							break;
						}
					}
				}

				//if((headNode.getNodeName().toLowerCase().equals("head") || headNode.getNodeName().toLowerCase().equals("configuration")) && (dataNodeName.equals("keystore") || dataNodeName.equals("data")))
				if(headNode.getNodeName().toLowerCase().equals("configuration") && dataNode.getNodeName().toLowerCase().equals("data")){
					// Read file attributes
					 XML.convertXmlToMap(headNode, fileAttributes, true);

					// Load the node data from the XML file
					int dateNodeChildCount = dataNode.getChildNodes().getLength();
					if(dateNodeChildCount > 0){
						int textNodeSkipper = 0;
						if(dataNode.getChildNodes().getLength() > 1){
							for(textNodeSkipper = 0; textNodeSkipper < dateNodeChildCount; textNodeSkipper++){
								if(dataNode.getChildNodes().item(textNodeSkipper).getNodeType() == Node.ELEMENT_NODE){
									break;
								}
							}
						}

						if(dataNode.getChildNodes().item(textNodeSkipper).getNodeType() == Node.ELEMENT_NODE){
							// File is not encrypted
							fileIsEncrypted = false;

							for(int i = 0; i < dataNode.getChildNodes().getLength(); i++){
								switch(dataNode.getChildNodes().item(i).getNodeName()){
									case "settings":
										XML.convertXmlToMap(dataNode.getChildNodes().item(i), fileSettings, true);
										break;

									case "tree":
										addChildNodes(dataNode.getChildNodes().item(i), tree.getRootNode());
										break;
								}
							}

							// file has been successfully opened
							return new FileConfiguration(xmlFile, fileVersion, fileIsEncrypted, this.fileType, null, fileAttributes, fileSettings);
						}
						else{
							// File is encrypted
							if(dataNode.getTextContent().trim().equals("")){throw new XMLParseException("XML-File is empty");}

							Node ivAttribute = dataNode.getAttributes().getNamedItem("iv");
							if(ivAttribute == null){throw new XMLParseException("IV of encrypted file is not available.");}
							byte[] aesIV = AES.bytesFromBase64String(ivAttribute.getNodeValue());

							String cipherName = "";

							Node encMethodAttribute = dataNode.getAttributes().getNamedItem("encryption");
							if(encMethodAttribute != null){
								cipherName = encMethodAttribute.getNodeValue();}
							else{
								throw new StorageException(StorageExceptionType.UnknownEncryptionCipher, "Cannot open file, the used encryption cipher is not mentioned.");
							}

							if(!EncryptionManager.getCipherAlgorithms().contains(cipherName)){
								throw new StorageException(StorageExceptionType.UnknownEncryptionCipher, "Encryption with '" + cipherName + "' is not supported on this system.");
							}

							int attempts = 0;
							while(attempts < 3){
								attempts++;
								try {
									// Decrypt data...
									fileIsEncrypted = true;

									char[] pw;
									if(filepassword.equals("")){
										String txt = LocaleLoader.getBundle(ApplicationInstance.CORE_LANGUAGE_BUNDLE).getString("encryption.input_password_label");
										pw = app.requestPasswordInput(ApplicationInstance.APP_NAME, txt, fileAttributes.getOrDefault(FileConfiguration.PASSWORD_HINT_ATTRIBUTE_NAME, ""));

										if(pw.length == 0){throw new UserCanceledOperationException("The user canceled the operation.");}
									}
									else{
										pw = filepassword.toCharArray();
										filepassword = "";
									}

									byte[] salt = new byte[0];
									Node saltAttribute = dataNode.getAttributes().getNamedItem("salt");
									if(saltAttribute != null){salt = AES.bytesFromBase64String(saltAttribute.getNodeValue());}

									EncryptionManager em = new EncryptionManager(cipherName, pw, aesIV, salt);

									Document xmldoc = XML.loadXmlDocument(em.decrypt(dataNode.getTextContent()));

									for(int i = 0; i < xmldoc.getDocumentElement().getChildNodes().getLength(); i++){
										switch(xmldoc.getDocumentElement().getChildNodes().item(i).getNodeName()){
											case "settings":
												XML.convertXmlToMap(xmldoc.getDocumentElement().getChildNodes().item(i), fileSettings, true);
												break;

											case "tree":
												addChildNodes(xmldoc.getDocumentElement().getChildNodes().item(i), tree.getRootNode());
												break;
										}
									}

									// Encrypted file has been successfully opened
									return new FileConfiguration(xmlFile, fileVersion, fileIsEncrypted, this.fileType, em, fileAttributes, fileSettings);

								} catch (NoSuchAlgorithmException e) {
									throw new StorageException(StorageExceptionType.UnknownEncryptionCipher, "Encryption with '" + cipherName + "' is not supported on this system.");
								} catch (InvalidKeyException e) {
									app.alert(LocaleLoader.getBundle(ApplicationInstance.CORE_LANGUAGE_BUNDLE).getString("encryption.incorrect_password"));
								} catch (DOMException e) {
									throw new XMLParseException(e.getMessage());
								}
							}

							// The user entered a wrong password three times...
							app.println("You entered a wrong password three times, canceling...");
							throw new UserCanceledOperationException("The user entered a wrong password three times.");
						}
					}
					else{
						throw new XMLParseException("Completly empty file.");
					}
				}
				else{
					throw new XMLParseException("Unsupported XML-File format.");
				}
			}
			else{
				throw new XMLParseException("Unsupported XML-File format.");
			}

		} catch (SAXException e) {
			throw new XMLParseException("SAXException, unable to parse XMLDoc");

		} catch (IOException e) {
			e.printStackTrace();
			throw new XMLParseException("IOException, unable to parse XMLDoc");
		}
	}

	private void addChildNodes(org.w3c.dom.Node parentXMLNode, TreeNode parentTreeNode){
		addChildNodes(parentXMLNode, parentTreeNode, 0);
	}

	private void addChildNodes(org.w3c.dom.Node parentXMLNode, TreeNode parentTreeNode, int startindex){
		for(int i = startindex; i < parentXMLNode.getChildNodes().getLength(); i++){
			org.w3c.dom.Node childXMLNode = parentXMLNode.getChildNodes().item(i);

			if(childXMLNode.getNodeType() == Node.ELEMENT_NODE){
				TreeNode newTreenode = createTreeNode(childXMLNode);
				parentTreeNode.addChildNode(newTreenode);

				if(childXMLNode.getChildNodes().getLength() > 0){
					addChildNodes(childXMLNode, newTreenode);
				}
			}
		}
	}

	private TreeNode createTreeNode(org.w3c.dom.Node xmlNode){
		String id = null;

		TreeNode newNode = new DefaultTreeNode();

		for(int i = 0; i < xmlNode.getAttributes().getLength(); i++){
			String name = xmlNode.getAttributes().item(i).getNodeName();
			String value = xmlNode.getAttributes().item(i).getNodeValue();

			switch(name){
				case "id":
					if (!this.compatibilityRegenerateNodeIds) {
						id = value;
						break;
					}

				case "text":
					newNode.setText(value);
					break;

				case "color":
					newNode.setColor(value);
					break;

				default:
					newNode.setAttribute(name, value);
			}
		}

		return id == null ? newNode : TreeStore.restoreNode(newNode, id);
	}

	/*
	 * ==============================================================================================================================================
	 * Save file
	 * ==============================================================================================================================================
	 */

	private static Document createXMLFrame(TreeNode rootNode, FileConfiguration fileConfig) throws XMLParseException, InvalidKeyException{
		try {
			Document xmldoc = XML.createXmlDocument("KeyMind");

			org.w3c.dom.Attr versionAttrib = xmldoc.createAttribute("version");
			versionAttrib.setNodeValue(latestFileFormatVersion);
			xmldoc.getDocumentElement().getAttributes().setNamedItem(versionAttrib);

			org.w3c.dom.Node configNode = xmldoc.createElement("configuration");
			org.w3c.dom.Node dataNode = xmldoc.createElement("data");

			XML.convertMapToFlatXml(fileConfig.getFileAttributes(), configNode);

			xmldoc.getDocumentElement().appendChild(configNode);
			xmldoc.getDocumentElement().appendChild(dataNode);

			if(fileConfig.isEncrypted()){
				Document subxmldoc = XML.createXmlDocument("root");

				org.w3c.dom.Node settingsNode = subxmldoc.createElement("settings");
				org.w3c.dom.Node treeNode = subxmldoc.createElement("tree");

				XML.convertMapToXmlNodes(fileConfig.getFileSettings(), settingsNode);
				appendChildNodesToXMLFile(subxmldoc, treeNode, rootNode);

				subxmldoc.getDocumentElement().appendChild(settingsNode);
				subxmldoc.getDocumentElement().appendChild(treeNode);

				try{
					dataNode.setTextContent(fileConfig.getEncryptionManager().encrypt(XML.writeXmlDocumentToString(subxmldoc)));

					org.w3c.dom.Attr ivAttrib = xmldoc.createAttribute("iv");
					org.w3c.dom.Attr cipherName = xmldoc.createAttribute("encryption");

					if(fileConfig.getEncryptionManager().getCipher().areSaltedHashesSupported()){
						org.w3c.dom.Attr saltAttribute = xmldoc.createAttribute("salt");
						saltAttribute.setNodeValue(fileConfig.getEncryptionManager().getPasswordSaltAsBase64());
						dataNode.getAttributes().setNamedItem(saltAttribute);
					}

					ivAttrib.setNodeValue(fileConfig.getEncryptionManager().getIVasBase64());
					cipherName.setNodeValue(fileConfig.getEncryptionManager().getCipher().getCipherName());

					dataNode.getAttributes().setNamedItem(ivAttrib);
					dataNode.getAttributes().setNamedItem(cipherName);

				} catch (InvalidKeySpecException e) {
					e.printStackTrace();
					throw new InvalidKeyException(e.getMessage());
				}
			}
			else{
				org.w3c.dom.Node settingsNode = xmldoc.createElement("settings");
				org.w3c.dom.Node treeNode = xmldoc.createElement("tree");

				XML.convertMapToXmlNodes(fileConfig.getFileSettings(), settingsNode);
				appendChildNodesToXMLFile(xmldoc, treeNode, rootNode);

				dataNode.appendChild(settingsNode);
				dataNode.appendChild(treeNode);
			}

			return xmldoc;

		} catch (ParserConfigurationException | DOMException | TransformerException e) {
			throw new XMLParseException(e.getMessage());
		}
	}

	private static void appendChildNodesToXMLFile(Document xmldoc, org.w3c.dom.Node parentXMLNode, TreeNode parentTreeNode){
		for(int i = 0; i < parentTreeNode.countChildNodes(); i++){
			TreeNode childNode = parentTreeNode.getChildNodeByIndex(i);

			org.w3c.dom.Node xmlNode = xmldoc.createElement("Node");

			org.w3c.dom.Attr attrib = xmldoc.createAttribute("text");
			attrib.setNodeValue(childNode.getText());
			xmlNode.getAttributes().setNamedItem(attrib);

			attrib = xmldoc.createAttribute("id");
			attrib.setNodeValue(childNode.getId());
			xmlNode.getAttributes().setNamedItem(attrib);

			if(childNode.getColor() != null){
				attrib = xmldoc.createAttribute("color");
				attrib.setNodeValue(childNode.getColor());
				xmlNode.getAttributes().setNamedItem(attrib);
			}

			for(String key: childNode.listAttributes()){
				String value = childNode.getAttribute(key);
				if(value != null){
					if(!value.equals("")){
						org.w3c.dom.Attr attribute = xmldoc.createAttribute(key);
						attribute.setNodeValue(value);
						xmlNode.getAttributes().setNamedItem(attribute);
					}
				}
			}

			if(childNode.countChildNodes() > 0){
				appendChildNodesToXMLFile(xmldoc, xmlNode, childNode);
			}

			parentXMLNode.appendChild(xmlNode);
		}
	}
}
