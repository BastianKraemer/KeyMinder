/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	KeyMindFileHandler.java

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
import de.akubix.keyminder.core.db.StandardNode;
import de.akubix.keyminder.core.db.Tree;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.encryption.EncryptionManager;
import de.akubix.keyminder.core.exceptions.StorageException;
import de.akubix.keyminder.core.exceptions.StorageExceptionType;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.lib.AESCore;
import de.akubix.keyminder.lib.XMLCore;

public class KeyMindFileHandler implements StorageHandler {

	private static String latestFileFormatVersion = "1.1.0";
	private String fileType;
	public KeyMindFileHandler(String forFileType){
		this.fileType = forFileType;
	}

	@Override
	public FileConfiguration open(File xmlFile, String filePassword, Tree tree, ApplicationInstance instance) throws StorageException {
		if(!xmlFile.exists()){throw new StorageException(StorageExceptionType.FileNotFound, "File not Found.");}

		try {
			return readXMLFrame(xmlFile, filePassword, instance.getTree(), instance);
		} catch (UserCanceledOperationException e) {
			throw new StorageException(StorageExceptionType.UserCancelation, e.getMessage());
		} catch (XMLParseException e) {
			throw new StorageException(StorageExceptionType.ParserException, e.getMessage());
		}
	}

	@Override
	public void save(FileConfiguration file, Tree tree, ApplicationInstance instance) throws StorageException {
		try {
			XMLCore.saveXMLDocument(file.getFilepath(), createXMLFrame(tree.getRootNode(), file));
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

	private FileConfiguration readXMLFrame(File xmlFile, String filepassword, Tree tree, ApplicationInstance app) throws XMLParseException, UserCanceledOperationException, StorageException {
		String fileVersion = latestFileFormatVersion; // Will be overwritten by the value in the file (if available)
		boolean fileIsEncrypted;
		Map<String, String> fileAttributes = new HashMap<>();
		Map<String, String> fileSettings = new HashMap<>();

		try {
			Document document = XMLCore.loadDocumentFromFile(xmlFile);

			// Read the version from the XML root node
			Map<String, String> rootAttribs = XMLCore.xmlAttributes2Hash(document.getDocumentElement());
			if(rootAttribs.containsKey("version")){
				if(!rootAttribs.get("version").matches("1.[0-1]($|.(.*))")){throw new StorageException(StorageExceptionType.UnsupportedVersion, "Document Version not supported.");}
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
					 XMLCore.xml2Map(headNode, "", fileAttributes);

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
										XMLCore.xml2Map(dataNode.getChildNodes().item(i), "", fileSettings);
										break;

									case "tree":
										addChildNodes(dataNode.getChildNodes().item(i), tree.getRootNode());
										break;
								}
							}

							// unencrypted file has been successfully opened
							return new FileConfiguration(xmlFile, fileVersion, fileIsEncrypted, this.fileType, null, fileAttributes, fileSettings);
						}
						else{
							// File is encrypted
							if(dataNode.getTextContent().trim().equals("")){throw new XMLParseException("XML-File is empty");}

							Node ivAttribute = dataNode.getAttributes().getNamedItem("iv");
							if(ivAttribute == null){throw new XMLParseException("IV of encrypted file is not available.");}
							byte[] aesIV = AESCore.bytesFromBase64String(ivAttribute.getNodeValue());

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

									String pw;
									if(filepassword.equals("")){
										String txt = app.isFxUserInterfaceAvailable() ? app.getFxUserInterface().getLocaleBundleString("decryption.input_password_label") : "Please enter your password:";
										pw = app.requestStringInput(ApplicationInstance.APP_NAME, txt,
																	fileAttributes.containsKey("PasswordHint") ? fileAttributes.get("PasswordHint") : "", true);

										if(pw.equals("")){throw new UserCanceledOperationException("The user canceled the operation.");}
									}
									else{
										pw = filepassword;
										filepassword = "";
									}

									byte[] salt = new byte[0];
									Node saltAttribute = dataNode.getAttributes().getNamedItem("salt");
									if(saltAttribute != null){salt = AESCore.bytesFromBase64String(saltAttribute.getNodeValue());}

									EncryptionManager em = new EncryptionManager(cipherName, pw.toCharArray(), aesIV, salt);

									Document xmldoc = XMLCore.loadDocumentFromString(em.decrypt(dataNode.getTextContent()));

									for(int i = 0; i < xmldoc.getDocumentElement().getChildNodes().getLength(); i++){
										switch(xmldoc.getDocumentElement().getChildNodes().item(i).getNodeName()){
											case "settings":
												XMLCore.xml2Map(xmldoc.getDocumentElement().getChildNodes().item(i), "", fileSettings);
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
									app.alert(app.isFxUserInterfaceAvailable() ? app.getFxUserInterface().getLocaleBundleString("decryption.wrong_password") : "Wrong password.");

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

		catch (ParserConfigurationException e) {
			throw new XMLParseException("ParserConfigurationException, unable to parse XMLDoc");
		}
	}

	private static void addChildNodes(org.w3c.dom.Node parentXMLNode, TreeNode parentTreeNode){
		addChildNodes(parentXMLNode, parentTreeNode, 0);
	}

	private static void addChildNodes(org.w3c.dom.Node parentXMLNode, TreeNode parentTreeNode, int startindex){
		for(int i = startindex; i < parentXMLNode.getChildNodes().getLength(); i++){
			org.w3c.dom.Node childXMLNode = parentXMLNode.getChildNodes().item(i);

			if(childXMLNode.getNodeType() == Node.ELEMENT_NODE){
				StandardNode newTreenode = createTreeNode(childXMLNode, parentTreeNode.getTree());
				parentTreeNode.getTree().addNode(newTreenode, parentTreeNode);

				if(childXMLNode.getChildNodes().getLength() > 0){
					addChildNodes(childXMLNode, newTreenode);
				}
			}
		}
	}

	private static StandardNode createTreeNode(org.w3c.dom.Node xmlNode, Tree tree){
		Map<String, String> attribs = new HashMap<String, String>();
		String text = "";
		String color = "";
		int id = 0;
		for(int i = 0; i < xmlNode.getAttributes().getLength(); i++){
			String name = xmlNode.getAttributes().item(i).getNodeName();
			String value = xmlNode.getAttributes().item(i).getNodeValue();

			switch(name){
				case "id":
					try{
						id = Integer.parseInt(value);
					}
					catch(NumberFormatException numEx){
						System.out.println(String.format("Warning: Illegal node id value (id=\"%s\"). Using temporary id and fix this later.", value));
					}
					break;

				case "text":
					text = value;
					break;

				case "color":
					color = value;
					break;

				default:
					attribs.put(name, value);
			}
		}
		StandardNode newNode = tree.loadNode(text, id, color, attribs).getUnrestrictedAccess();
		return newNode;
	}

	/*
	 * ==============================================================================================================================================
	 * Save file
	 * ==============================================================================================================================================
	 */

	private static Document createXMLFrame(TreeNode rootNode, FileConfiguration fileConfig) throws XMLParseException, InvalidKeyException{
		try {
			Document xmldoc = XMLCore.createEmptyXMLDocument("KeyMind");

			org.w3c.dom.Attr versionAttrib = xmldoc.createAttribute("version");
			versionAttrib.setNodeValue(latestFileFormatVersion);
			xmldoc.getDocumentElement().getAttributes().setNamedItem(versionAttrib);

			org.w3c.dom.Node configNode = xmldoc.createElement("configuration");
			org.w3c.dom.Node dataNode = xmldoc.createElement("data");

			XMLCore.map2FlatXMLNodes(configNode, fileConfig.fileAttributes);

			xmldoc.getDocumentElement().appendChild(configNode);
			xmldoc.getDocumentElement().appendChild(dataNode);

			if(fileConfig.isEncrypted()){
				Document subxmldoc = XMLCore.createEmptyXMLDocument("root");

				org.w3c.dom.Node settingsNode = subxmldoc.createElement("settings");
				org.w3c.dom.Node treeNode = subxmldoc.createElement("tree");

				XMLCore.convertHashToXMLNodes(settingsNode, fileConfig.fileSettings);
				appendChildNodesToXMLFile(subxmldoc, treeNode, rootNode);

				subxmldoc.getDocumentElement().appendChild(settingsNode);
				subxmldoc.getDocumentElement().appendChild(treeNode);

				try{
					dataNode.setTextContent(fileConfig.getEncryptionManager().encrypt(XMLCore.transfromXMLDocumentToString(subxmldoc)));

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

				XMLCore.convertHashToXMLNodes(settingsNode, fileConfig.fileSettings);
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
			attrib.setNodeValue(Integer.toString(childNode.getId()));
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
