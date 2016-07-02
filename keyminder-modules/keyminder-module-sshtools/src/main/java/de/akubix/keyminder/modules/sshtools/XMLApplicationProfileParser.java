/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	Breakout.java

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
package de.akubix.keyminder.modules.sshtools;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.KeyMinder;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.lib.XMLCore;
import de.akubix.keyminder.locale.LocaleLoader;
import de.akubix.keyminder.shell.Shell;
import de.akubix.keyminder.ui.console.ConsoleMode;
import de.akubix.keyminder.ui.fx.JavaFxUserInterface;
import de.akubix.keyminder.ui.fx.JavaFxUserInterfaceApi;
import javafx.stage.FileChooser;

/**
 * The {@link XMLApplicationProfileParser} class is used to generate the command line string for every application you want to start.
 * This is done by parsing an XML file which defines the command line interface of each application.
 * It's also possible to append some data of your tree to the command line. Just add a variable to the XML file and it will be replaced
 * with some information out of your file, for example ${username} will be replaced by the user name which is stored in currently selected tree node.
 */
public class XMLApplicationProfileParser
{
	private ApplicationInstance app;
	private Document xmldoc;
	private org.w3c.dom.Node profileRootNode;
	private TreeNode treeNode;
	private Map<String, String> variables;
	private List<String> commandLineArguments = new ArrayList<>();
	private boolean requirementsChecked = false;
	private final ResourceBundle locale;

	public XMLApplicationProfileParser(ApplicationInstance app, Document xmldoc){
		this(app, xmldoc, new HashMap<>());
	}

	public XMLApplicationProfileParser(ApplicationInstance app, Document xmldoc, Map<String, String> variables){
		this.app = app;
		this.xmldoc = xmldoc;
		this.variables = variables;
		this.locale = LocaleLoader.getBundle(JavaFxUserInterface.LANGUAGE_BUNDLE_KEY);
	}

	/**
	 * Create a {@link XMLApplicationProfileParser} object by using an {@link InputStream} which will be parsed as XML {@link Document}
	 * @param app current instance of the application (maybe some settings are referenced by a variable)
	 * @param applicationProfileXMLInputStream the input stream that contains the XML document
	 * @param variables a preset of custom variables
	 * @return An instance of the {@link XMLApplicationProfileParser}
	 * @throws IllegalArgumentException if the XML input stream couldn't be parsed
	 */
	public static XMLApplicationProfileParser createInstance(ApplicationInstance app, InputStream applicationProfileXMLInputStream, Map<String, String> variables) throws IllegalArgumentException {
		if(applicationProfileXMLInputStream == null){throw new IllegalArgumentException("Error: input stream of XML application profile is 'null'.");}

		try {
			return new XMLApplicationProfileParser(app, XMLCore.loadDocumentFromStream(applicationProfileXMLInputStream), variables);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new IllegalArgumentException("Cannot parse XML application profile");
		}
	}

	/**
	 * Create a {@link XMLApplicationProfileParser} object by using a string that will be parsed as XML {@link Document}
	 * @param app current instance of the application (maybe some settings are referenced by a variable)
	 * @param applicationProfileXMLString the string that contains the XML application profile
	 * @param variables a preset of custom variables
	 * @return An instance of the {@link XMLApplicationProfileParser}
	 * @throws IllegalArgumentException if the XML string couldn't be parsed
	 */
	public static XMLApplicationProfileParser createInstance(ApplicationInstance app, String applicationProfileXMLString, Map<String, String> variables) throws IllegalArgumentException {
		try {
			return new XMLApplicationProfileParser(app, XMLCore.loadDocumentFromString(applicationProfileXMLString), variables);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new IllegalArgumentException("Cannot parse XML application profile");
		}
	}

	/**
	 * Generates the parameter list for a process defined by an XML application profile
	 * @param configurationIdentifier the identifier to select the configuration that should be used
	 * @param treeNode the currently selected TreeNode (maybe some attributes are referenced by a variable)
	 * @return the list of parameters for your profile (including the application path as first argument)
	 * @throws IllegalArgumentException if the XML document doesn't contain a application profile with the give identifier
	 */
	public List<String> generateCommandLineParameters(String configurationIdentifier, TreeNode treeNode) throws IllegalArgumentException {
		this.treeNode = treeNode;

		//Verify that the profile is compatible with this parser
		org.w3c.dom.Node versionAttrib = xmldoc.getDocumentElement().getAttributes().getNamedItem("version");
		if(versionAttrib != null){
			if(!versionAttrib.getNodeValue().matches("^(2\\.0|2\\.0\\..*)$")){
				throw new IllegalArgumentException("The application profile has been designed for another profile parser\nSupported Versions: 2.0.x, Version of document: " + versionAttrib.getNodeValue());
			}
		}

		org.w3c.dom.Node rootNode = xmldoc.getDocumentElement();
		for(int i = 0; i < rootNode.getChildNodes().getLength(); i++){
			org.w3c.dom.Node childNode = rootNode.getChildNodes().item(i);
			if(childNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE){
				if(childNode.getNodeName().matches("(?i)configuration")){
					org.w3c.dom.Node attrib = childNode.getAttributes().getNamedItem("id");
					if(attrib != null){
						if(attrib.getNodeValue().equals(configurationIdentifier)){

							// Parse profile
							this.profileRootNode = childNode;
							org.w3c.dom.Node execAttrib = this.profileRootNode.getAttributes().getNamedItem("execute");
							if(execAttrib == null || execAttrib.getNodeValue().equals("")){throw new IllegalArgumentException("The application profile does not contain an executable that should be started.");}
							if(KeyMinder.verbose_mode){app.println(String.format("Generating command line parameters for application '%s'...", execAttrib.getNodeValue()));}

							String exec = replaceVariablesInString(execAttrib.getNodeValue());
							if(exec.equals("")){throw new IllegalArgumentException(String.format("Executable is not defined: '%s' is empty.", execAttrib.getNodeValue()));}
							commandLineArguments.add(replaceVariablesInString(execAttrib.getNodeValue()));

							parseChildNodes(this.profileRootNode);

							return commandLineArguments; //The method above will fill this list
						}
					}
				}
				else{
					variables.put(childNode.getNodeName(), childNode.getNodeValue());
				}
			}
		}

		throw new IllegalArgumentException(String.format("The application profile does not contain a configuration that matches the identifier '%s'.", configurationIdentifier));
	}

	/**
	 * The root node of the XML document may contain an attribute called "require"
	 * The task of this method is to check those requirements. This will fail if one of those variables does not exist or only contain "".
	 * Note: The requirements will be checked when the first "<option>" or "<action>" tag is reached, so you can also require self defined variables
	 * @throws IllegalArgumentException if at least one requirement of this profile are not fulfilled
	 */
	private void checkRequirements() throws IllegalArgumentException {
		requirementsChecked = true;
		org.w3c.dom.Node attrib = profileRootNode.getAttributes().getNamedItem("require");
		if(attrib != null){
			for(String str: attrib.getNodeValue().split(";\\ |;")){
				if(replaceVariablesInString(str).equals("")){
					throw new IllegalArgumentException(String.format("Cannot run application profile: Variable %s is empty or does not exist", str));
				}
			}
		}
	}

	private String parseNextXMLNode(org.w3c.dom.Node xmlNode){
		switch (xmlNode.getNodeName().toLowerCase()) {
			case "var":
				parseVarTag(xmlNode);
				break;

			case "if":
				return parseIfTag(xmlNode);

			case "option":
				parseOptionTag(xmlNode);
				break;

			case "action":
				parseActionTag(xmlNode);
				break;

			case "split":
				return parseSplitTag(xmlNode);

			case "append":
				return parseChildNodes(xmlNode);

			case "exit":
				throw new IllegalArgumentException(String.format("Operation has been canceled: %s", replaceVariablesInString(parseChildNodes(xmlNode))));

			default:
				throw new IllegalArgumentException(String.format("Cannot parse XML application profile: Unknown tag '%s'", xmlNode.getNodeName()));
		}

		return "";
	}

	private String parseChildNodes(org.w3c.dom.Node parentXMLNode){
		if(XMLCore.hasChildNodes(parentXMLNode)){
			StringBuilder returnValue = new StringBuilder();
			for(int i = 0; i < parentXMLNode.getChildNodes().getLength(); i++){
				org.w3c.dom.Node childNode = parentXMLNode.getChildNodes().item(i);
				if(childNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE){
					returnValue.append(parseNextXMLNode(childNode));
				}
			}
			return returnValue.toString();
		}
		else{
			return parentXMLNode.getTextContent();
		}
	}

	private void parseVarTag(org.w3c.dom.Node xmlNode) throws IllegalArgumentException{

		org.w3c.dom.Node nameAttrib = xmlNode.getAttributes().getNamedItem("name");

		if(nameAttrib == null){throw new IllegalArgumentException("Required attribute is not available: Cannot set variable without a name '<var name=???>...</var>'.");}
		if(nameAttrib.getNodeValue().equals("")){throw new IllegalArgumentException("Required attribute is not available: Cannot set variable without a name. '<var name=\"\">...</var>''.");}

		variables.put(nameAttrib.getNodeValue(), replaceVariablesInString(parseChildNodes(xmlNode)));

		if(KeyMinder.verbose_mode){
			app.println(String.format("\tVariable ${%s} = \"%s\"", nameAttrib.getNodeValue(),
						(nameAttrib.getNodeValue().matches(".*(?i)(pw|passwd|password).*")) ?
						 "*****" : variables.get(nameAttrib.getNodeValue())));
		}
	}

	private void parseOptionTag(org.w3c.dom.Node xmlNode) throws IllegalArgumentException{
		if(!requirementsChecked){checkRequirements();}
		org.w3c.dom.Node conditionAttrib = xmlNode.getAttributes().getNamedItem("if");
		if(conditionAttrib != null){
			if(replaceVariablesInString(conditionAttrib.getNodeValue()).equals("")){return;}
		}
		String str = replaceVariablesInString(parseChildNodes(xmlNode));
		if(!str.equals("")){
			commandLineArguments.add(str);
		}
	}

	private String parseIfTag(org.w3c.dom.Node xmlNode) throws IllegalArgumentException{
		if(xmlNode.getAttributes().getNamedItem("exists") != null){return parseIfExists(xmlNode, false);}
		if(xmlNode.getAttributes().getNamedItem("not_exists") != null){return parseIfExists(xmlNode, true);}
		if(xmlNode.getAttributes().getNamedItem("equals") != null){return parseIfEquals(xmlNode, false);}
		if(xmlNode.getAttributes().getNamedItem("not_equals") != null){return parseIfEquals(xmlNode, true);}

		throw new IllegalArgumentException("Unknown if condition. Allowed conditions are 'exists', 'not_exists', 'equals' and 'not_equals'.");
	}

	private String parseIfExists(org.w3c.dom.Node xmlNode, boolean checkNotExists){
		org.w3c.dom.Node existsAttrib = xmlNode.getAttributes().getNamedItem(checkNotExists ? "not_exists" : "exists");

		if(replaceVariablesInString(existsAttrib.getNodeValue()).equals("") ^ !checkNotExists){
			return parseChildNodes(xmlNode);
		}
		else{
			org.w3c.dom.Node elseAttrib = xmlNode.getAttributes().getNamedItem("else");
			return (elseAttrib == null) ? "" : elseAttrib.getNodeValue();
		}
	}

	private String parseIfEquals(org.w3c.dom.Node xmlNode, boolean checkNotEquals) throws IllegalArgumentException{
		org.w3c.dom.Node equalsAttrib = xmlNode.getAttributes().getNamedItem(checkNotEquals ? "not_equals" : "equals");

		if(!equalsAttrib.getNodeValue().contains("==")){throw new IllegalArgumentException("Error: Conditions like 'if (not) equals'  always need an '==' operator. <if equals=\"${var} == 'some text'\">...</if>");}

		String splitStr[] = equalsAttrib.getNodeValue().split("==", 2);

		String left = splitStr[0].trim();
		String right = splitStr[1].trim();
		if(left.matches("^'.*'$")){left = left.substring(1, left.length() - 1);}
		if(right.matches("^'.*'$")){right = right.substring(1, right.length() - 1);}

		if(replaceVariablesInString(left).equals(replaceVariablesInString(right)) ^ checkNotEquals){
			return parseChildNodes(xmlNode);
		}
		else{
			org.w3c.dom.Node elseAttrib = xmlNode.getAttributes().getNamedItem("else");
			return (elseAttrib == null) ? "" : elseAttrib.getNodeValue();
		}
	}

	private void parseActionTag(org.w3c.dom.Node xmlNode) throws IllegalArgumentException{
		if(!requirementsChecked){checkRequirements();}
		final String targetAttributeName = "type";
		org.w3c.dom.Node targetAttribute = xmlNode.getAttributes().getNamedItem(targetAttributeName);
		if(targetAttribute == null){throw new IllegalArgumentException(String.format("Error '%s' attribute is not defined '<action %s=???>...</action>'.", targetAttributeName, targetAttributeName));}

		String value = replaceVariablesInString(parseChildNodes(xmlNode));

		switch(targetAttribute.getNodeValue()){
			case "clipboard":
				if(JavaFxUserInterface.isLoaded(app)){
					JavaFxUserInterface.getInstance(app).setClipboardText(value);
				}
				else{
					ConsoleMode.setClipboardText(value);
				}
				break;

			case "print":
				app.println(value);
				break;

			case "log":
				app.log(value);
				break;

			case "alert":
				app.alert(value);
				break;

			default:
				throw new IllegalArgumentException(String.format("ERROR: Value '%s' of attribute '%s' is not supported.", targetAttribute.getNodeValue(), targetAttributeName));
		}
	}

	/**
	 * Replaces all Variables (${variable}) in a string
	 * @param source The String the variables should be replaced in
	 * @return The String with all replaced variables (if possible)
	 */
	private String replaceVariablesInString(String source){
		return Shell.replaceVariables(source, (var) -> getValueOfVariable(var));
	}

	/**
	 * Returns the value of the variable
	 * @param varName The name of the variable without ${}
	 * @return The value of the variable OR "" if there is no value for this variable
	 */
	private String getValueOfVariable(String varName) throws IllegalArgumentException{
		/* Some "special function variables":
		 * ${_clipboard_} will return the current clip board value
		 * ${_openfiledialog_} will open a file dialog and return the file name
		 * ${_openfiledialog_} will open a file dialog and return the file name
		 */

		if(varName.equals("_clipboard_")){
			if(JavaFxUserInterface.isLoaded(app)){
				JavaFxUserInterface.getInstance(app).getClipboardText();
			}
			else{
				return ConsoleMode.getClipboardText();
			}
		}

		try{
			if(varName.equals("_openfiledialog_")){
				if(JavaFxUserInterface.isLoaded(app)){
					JavaFxUserInterfaceApi fxUI = JavaFxUserInterface.getInstance(app);
					java.io.File f = fxUI.showOpenFileDialog(locale.getString("filebrowser.dialogtitle"), "", null,
															 new FileChooser.ExtensionFilter[]{
																	new FileChooser.ExtensionFilter(locale.getString("filebrowser.allfiles_selector"), "*.*")});
					return (f != null) ? f.getAbsolutePath() : "";
				}
				else{
					return new String(app.requestPasswordInput("Please enter a filename", "Please enter a filename:", ""));
				}
			}

			if(varName.equals("_savefiledialog_")){
				if(JavaFxUserInterface.isLoaded(app)){
					JavaFxUserInterfaceApi fxUI = JavaFxUserInterface.getInstance(app);
					java.io.File f = fxUI.showSaveFileDialog(locale.getString("filebrowser.dialogtitle"), "", null,
															 new FileChooser.ExtensionFilter[]{
																	new FileChooser.ExtensionFilter(locale.getString("filebrowser.allfiles_selector"), "*.*")});
					return (f != null) ? f.getAbsolutePath() : "";
				}
				else{
					return new String(app.requestPasswordInput("Please enter a filename", "Please enter a filename:", ""));
				}
			}
		}
		catch(UserCanceledOperationException e){
			throw new IllegalArgumentException(e.getMessage());
		}

		return app.lookup(varName, treeNode, variables);
	}

	private String parseSplitTag(org.w3c.dom.Node xmlNode) throws IllegalArgumentException {
		org.w3c.dom.Node srcAttrib = xmlNode.getAttributes().getNamedItem("src");
		org.w3c.dom.Node delimiterAttrib = xmlNode.getAttributes().getNamedItem("delimiter");
		org.w3c.dom.Node variableAttrib = xmlNode.getAttributes().getNamedItem("var");
		if(srcAttrib == null || delimiterAttrib == null || variableAttrib == null ||
		   srcAttrib.getNodeValue().equals("") || delimiterAttrib.getNodeValue().equals("") || variableAttrib.getNodeValue().equals("")){
			throw new IllegalArgumentException("Required attribute is not available. Cannot perform split without parameter 'src', 'delimiter' or 'var'. Example '<split src=\"${test}\" delimiter=\"\\n\" var=\"splitstr\">...</split>'.");
		}

		String splitstr[] = replaceVariablesInString(srcAttrib.getNodeValue()).split(replaceVariablesInString(delimiterAttrib.getNodeValue()));
		StringBuilder ret = new StringBuilder();
		for(String str: splitstr){
			variables.put(variableAttrib.getNodeValue(), replaceVariablesInString(str));
			ret.append(parseChildNodes(xmlNode));
		}

		return ret.toString();
	}
}
