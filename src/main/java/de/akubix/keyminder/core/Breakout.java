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
package de.akubix.keyminder.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.interfaces.FxUserInterface;
import javafx.stage.FileChooser;

/**
 * This class is able to generate a command line string for any application using the data and information which are stored in your tree
 * All members of this class are static, use the function "generateParameterListForProfile()" for all interactions.
 */
public class Breakout
{
	private Breakout(){}
		
	/**
	 * Generates the parameter list for a process defined by an XML application profile
	 * @param app current instance of the application (maybe some settings are referenced by a variable)
	 * @param applicationProfileXMLInputStream the input stream that contains the XML document
	 * @param identifier the identifier to select the configuration that should be used
	 * @param node The currently selected TreeNode (maybe some attributes are referenced by a variable
	 * @param variables Allows you to preset custom variables which can be used in the application profile. If you don't want to use this, you can simply submit {@code null}
	 * @return the list of parameters for your profile (including the application path as first argument)
	 * @throws IllegalArgumentException if the XML input stream couldn't be parsed
	 */
	public static List<String> generateParameterListForProfile(ApplicationInstance app, InputStream applicationProfileXMLInputStream, String identifier, TreeNode node, Map<String, String> variables) throws IllegalArgumentException
	{
		try {
			if(applicationProfileXMLInputStream == null){throw new IllegalArgumentException("Error: input stream of XML application profile is 'null'.");}

			return generateParameterListForProfile(app, de.akubix.keyminder.lib.XMLCore.loadDocumentFromStream(applicationProfileXMLInputStream), identifier, node, variables);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Cannot parse XML");
		}
	}
	
	/**
	 * Generates the parameter list for a process defined by an XML application profile
	 * @param app current instance of the application (maybe some settings are referenced by a variable)
	 * @param applicationProfileXMLString the XML-Document that contains the profile as string
	 * @param identifier the identifier to select the configuration that should be used
	 * @param node The currently selected TreeNode (maybe some attributes are referenced by a variable)
	 * @param variables Allows you to preset custom variables which can be used in the application profile. If you don't want to use this, you can simply use 'null'
	 * @return the list of parameters for your profile (including the application path as first argument)
	 * @throws IllegalArgumentException if the XML String couldn't be parsed
	 */
	public static List<String> generateParameterListForProfile(ApplicationInstance app, String applicationProfileXMLString, String identifier, TreeNode node, Map<String, String> variables) throws IllegalArgumentException
	{
		try {
			return generateParameterListForProfile(app, de.akubix.keyminder.lib.XMLCore.loadXMLFromString(applicationProfileXMLString), identifier, node, variables);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Cannot parse XML-String.");
		}
	}
	
	/**
	 * Generates the parameter list for a process defined by an XML application profile
	 * @param app current instance of the application (maybe some settings are referenced by a variable)
	 * @param xmldoc the XML-Document that contains the profile
	 * @param identifier the identifier to select the configuration that should be used
	 * @param treeNode the currently selected TreeNode (maybe some attributes are referenced by a variable)
	 * @param variables allows you to preset custom variables which can be used in the application profile. If you don't want to use this, you can simply use 'null'
	 * @return the list of parameters for your profile (including the application path as first argument)
	 * @throws IllegalArgumentException if the XML document doesn't contain a application profile with the give identifier
	 */
	public static List<String> generateParameterListForProfile(ApplicationInstance app, Document xmldoc, String identifier, TreeNode treeNode, Map<String, String> variables) throws IllegalArgumentException
	{	
		if(variables == null){variables = new HashMap<>();}
		
		//Verify that the profile is compatible with this parser
		org.w3c.dom.Node versionAttrib = xmldoc.getDocumentElement().getAttributes().getNamedItem("version");
		if(versionAttrib != null){if(!versionAttrib.getNodeValue().startsWith("1.")){
			throw new IllegalArgumentException("The application profile has been designed for another profile parser\nSupported Versions: 1.x, Version of document: " + versionAttrib.getNodeValue());
		}}
		
		Node rootNode = xmldoc.getDocumentElement();
		for(int i = 0; i < rootNode.getChildNodes().getLength(); i++){
			Node childNode = rootNode.getChildNodes().item(i);
			if(childNode.getNodeType() == Node.ELEMENT_NODE){
				if(childNode.getNodeName().matches("(?i)configuration")){
					Node attrib = childNode.getAttributes().getNamedItem("id");
					if(attrib != null){
						if(attrib.getNodeValue().equals(identifier)){
							return parseApplicationProfile(app, childNode, treeNode, variables);
						}
					}		
				}
				else
				{
					variables.put(childNode.getNodeName(), childNode.getNodeValue());
				}
			}
		}
		
		throw new IllegalArgumentException(String.format("The application profile does not contain a configuration that matches the identifier '%s'.", identifier));
	}
	
	/**
	 * Generates the parameter list for a process defined by a chosen configuration of an XML application profile
	 * @param app current instance of the application (maybe some settings are referenced by a variable)
	 * @param rootNode the XML root node of the chosen configuration
	 * @param node the currently selected TreeNode (maybe some attributes are referenced by a variable)
	 * @param variables Allows you to preset custom variables which can be used in the application profile. If you don't want to use this, you can simply use 'null'
	 * @return the list of parameters for your profile (including the application path as first argument)
	 * @throws IllegalArgumentException if at least one requirement of this profile are not fulfilled
	 */
	private static List<String> parseApplicationProfile(ApplicationInstance app, Node rootNode, TreeNode node, Map<String, String> variables) throws IllegalArgumentException
	{
		List<String> cmdline = new ArrayList<>();
		
		if(variables == null){variables = new HashMap<>();}
		
		org.w3c.dom.Node execAttrib = rootNode.getAttributes().getNamedItem("execute");
		if(execAttrib == null || execAttrib.getNodeValue().equals("")){throw new IllegalArgumentException("The application profile does not contain an executable that should be started.");}
		
		if(Launcher.verbose_mode){app.println(String.format("Generating command line parameters for application '%s'...", execAttrib.getNodeValue()));}
		
		boolean requirementsAlreadyChecked = false;
		final String optionTag = "option";
		final String actionTag = "action";
		
		for(int i = 0; i < rootNode.getChildNodes().getLength(); i++)
		{
			org.w3c.dom.Node xmlnode = rootNode.getChildNodes().item(i);
			
			if(xmlnode.getNodeType() == Node.ELEMENT_NODE)
			{
				if(xmlnode.getNodeName().equals("var"))
				{
					createVariable(variables, xmlnode, node, app);
				}
				else if(xmlnode.getNodeName().equals(optionTag) || xmlnode.getNodeName().equals(actionTag))
				{
					if(!requirementsAlreadyChecked){
						// This code will be executed when the first <option> or <command> tag is reached
						checkRequirements(variables, rootNode, node, app); // This will throw a "IllegalArgumentException" if some requirements are not fulfilled.
						cmdline.add(replaceVariablesInString(variables, node, app, execAttrib.getNodeValue()));
						requirementsAlreadyChecked = true;
					}

					org.w3c.dom.Node ifCondition = xmlnode.getAttributes().getNamedItem("if");
					if(ifCondition == null || ifTerm(replaceVariablesInString(variables, node, app, ifCondition.getNodeValue())))
					{
						if(xmlnode.getNodeName().equals(optionTag)){
							org.w3c.dom.Node attrib = xmlnode.getAttributes().getNamedItem("parameterSeperator");
							if(attrib == null)
							{
								cmdline.add(replaceVariablesInString(variables, node, app, xmlnode.getTextContent()));
							}
							else
							{
								splitMultipleParameters(cmdline, variables, attrib.getNodeValue(), node, app, xmlnode.getTextContent());
							}
						}
						else
						{
							final String targetAttributeName = "type";
							org.w3c.dom.Node targetAttribute = xmlnode.getAttributes().getNamedItem(targetAttributeName);
							if(targetAttribute == null){throw new IllegalArgumentException(String.format("ERROR: Attribute '%s' is not defined on '%s' tag", targetAttributeName, xmlnode.getNodeName()));}
							String value = replaceVariablesInString(variables, node, app, xmlnode.getTextContent());
							switch(targetAttribute.getNodeValue()){
								case "clipboard":
									if(app.isFxUserInterfaceAvailable()){
										app.getFxUserInterface().setClipboardText(value);
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
									
								case "exit":
									throw new IllegalArgumentException(String.format("Cannot run appliication profile%s%s", (value.equals("") ? "." : ": "), value));
		
								default:
									throw new IllegalArgumentException(String.format("ERROR: Value '%s' of attribute '%s' is not supported.", targetAttribute.getNodeValue(), targetAttributeName));
							}
						}
					}
				}
				else{
					throw new IllegalArgumentException(String.format("ERROR: Unknown tag '%s'", xmlnode.getNodeName()));
				}
			}
		}
		
		return cmdline;
	}
	
	/**
	 * The root node of the XML Document maybe contains an attribute "require"
	 * The task of this method is to check those requirements. This will fail if one of those is null or "".
	 * Note: The requirements will be checked when the first "<option>" or "<command>" tag is reached, so you can also require self defined variables
	 * @param var the Map which contains the list of all self defined variables
	 * @param xmlRootNode the root node of the XML Document
	 * @param node the currently selected TreeNode (maybe some attributes are referenced by a variable)
	 * @param app current instance of the application (maybe some settings are referenced by a variable)
	 * @throws IllegalArgumentException if at least one requirement of this profile are not fulfilled
	 */
	private static void checkRequirements(Map<String, String> var, org.w3c.dom.Node xmlRootNode, TreeNode node, ApplicationInstance app) throws IllegalArgumentException
	{
		org.w3c.dom.Node attrib = xmlRootNode.getAttributes().getNamedItem("require");
		if(attrib != null)
		{
			for(String y: attrib.getNodeValue().split(";\\ |;"))
			{
				if(!ifTerm(replaceVariablesInString(var, node, app, y), true))
				{
					throw new IllegalArgumentException(String.format("Requiremets not fulfilled: Variable %s is empty or does not exist", y));
				}
			}
		}
	}
	
	/**
	 * Create a new variable
	 * @param var The map that will contain the new variable
	 * @param xmlNode The XML-Node which contains all informations about the variable
	 * @param node The currently selected TreeNode (maybe some attributes are referenced by this variable)
	 * @param instance current instance of the application (maybe some settings are referenced by a variable)
	 * @throws IllegalArgumentException Occurs if required XML-Attributes does not exist or for example the name is empty
	 */
	private static void createVariable(Map<String, String> var, org.w3c.dom.Node xmlNode, TreeNode node, ApplicationInstance instance) throws IllegalArgumentException
	{
		// Load all supported XML attributes 
		org.w3c.dom.Node ifCondition = xmlNode.getAttributes().getNamedItem("if");
		org.w3c.dom.Node nameAttrib = xmlNode.getAttributes().getNamedItem("name");
		org.w3c.dom.Node valueAttrib = xmlNode.getAttributes().getNamedItem("defaultValue");
		org.w3c.dom.Node alternativeValueAttrib = xmlNode.getAttributes().getNamedItem("alternative");
		org.w3c.dom.Node ignoreAttrib = xmlNode.getAttributes().getNamedItem("ignoreOn");
		
		// Check If-Condition (if available) and cancel if the function 'ifTerm' returns false
		if(ifCondition != null){
			if(!ifTerm(replaceVariablesInString(var, node, instance, ifCondition.getNodeValue()))){	return;}
		}
		
		// Verify that all these attributes exists
		if(nameAttrib == null || valueAttrib == null){throw new IllegalArgumentException("Required attributes are not available. Attributes 'name' and 'defaultValue' must be defined.");}
		if(nameAttrib.getNodeValue().equals("")){throw new IllegalArgumentException("Name of variable cannot be \"\".");}
		
		String value = replaceVariablesInString(var, node, instance, valueAttrib.getNodeValue());
		
		if(ignoreAttrib != null){
			// Check if the value attribute equals the 'ignoreOn' attribute, if so the variable wont be created. -> return
			if(value.equals(replaceVariablesInString(var, node, instance, ignoreAttrib.getNodeValue()))){
				if(Launcher.verbose_mode){instance.println(String.format("\tVariable \"${s}\" will be ignored. Attribute 'ignoreOn' matched", nameAttrib.getNodeValue()));}
				return;
			}
		}
		
		// Store variable an replace all attribute references to tree nodes by their values (e.g ${ssh_host} -> "https://example.com")
		if(alternativeValueAttrib != null){
			var.put(nameAttrib.getNodeValue(), ifTerm(value) ? value : replaceVariablesInString(var, node, instance, alternativeValueAttrib.getNodeValue()));
		}
		else
		{
			var.put(nameAttrib.getNodeValue(), value);
		}

		if(Launcher.verbose_mode){
			instance.println(String.format("\tVariable ${%s} = \"%s\"", nameAttrib.getNodeValue(),
							(nameAttrib.getNodeValue().contains("pw") || nameAttrib.getNodeValue().contains("passw") ?
							 "*****" : var.get(nameAttrib.getNodeValue()))));
		}
	}
	
	/**
	 * Evaluate an "if" term
	 * @param val the value that will be checked
	 * @param onlyCheckIfEmpty if this parameter is {@code false} this function will return {@code false} if the value is '0', 'false', "off' or 'no'
	 * @return {@code true} if the String is NOT {@code null} and NOT "", {@code false} otherwise
	 */
	private static boolean ifTerm(String val, boolean onlyCheckIfEmpty)
	{		
		if(val == null){return false;}
		if(val.equals("")){return false;}
		if(!onlyCheckIfEmpty){
			val = val.toLowerCase();

			if(val.equals("0") || val.equals("false") || val.equals("off") || val.equals("no")){return false;}
		}
		return true;
	}

	private static boolean ifTerm(String val)
	{		
		return ifTerm(val, false);
	}

	/**
	 * Replaces all Variables (${variable}) in a String
	 * @param var The Map which contains all self defined variables
	 * @param node The currently selected TreeNode (maybe some attributes are referenced by a variable)
	 * @param instance current instance of the application (maybe some settings are referenced by a variable)
	 * @param source The String the variables should be replaced in
	 * @return The String with all replaced variables (if possible)
	 */
	private static String replaceVariablesInString(Map<String, String> var, TreeNode node, ApplicationInstance instance, String source)
	{
		// Regular expression to allow all characters in variables except "$"
		//	-> \\$\\{[^\\$]*\\}
		// Regular expression to allow only A-Z, a-z and 0-9 for variable names
		// 	-> \\$\\{[a-zA-Z0-9]*\\}
		
		for(MatchResult match : allMatches(Pattern.compile("\\$\\{[^\\$]*\\}"), source))
		{
			source = source.replace(match.group(), getValueOfVariable(var, node, instance, match.group().substring(2, match.group().length() - 1)));
		}
		return source;
	}
	
	/**
	 * Returns the value of the variable
	 * @param var The Map which contains all self defined variables
	 * @param node The currently selected TreeNode (maybe some attributes are referenced by a variable)
	 * @param app current instance of the application (maybe some settings are referenced by a variable)
	 * @param varName The name of the variable without ${}
	 * @return The value of the variable OR "" if there is no value for this variable
	 */
	private static String getValueOfVariable(Map<String, String> var, TreeNode node, ApplicationInstance app, String varName)
	{
		// There are multiple sources for the values of the variables: The variables of this document stored in 'var' or as part of the node attributes or ...
		// Hint: Take a look at the order - you can "overwrite" node attributes because they will be first looked up in 'var'

		if(var.containsKey(varName)){return var.get(varName);}
		if(node != null){if(node.hasAttribute(varName)){return node.getAttribute(varName);}}
		if(app.fileSettingsContainsKey(varName)){return app.getFileSettingsValue(varName);}
		if(app.settingsContainsKey(varName)){return app.getSettingsValue(varName);}
		if(node != null){if(varName.toLowerCase().equals("text")){return node.getText();}}
		
		/* Some "special function variables":
		 * ${_clipboard_} will return the current clip board value
		 * ${_openfiledialog_} will open a file dialog and return the file name
		 * ${_openfiledialog_} will open a file dialog and return the file name
		 */
		
		if(varName.equals("_clipboard_")){
			if(app.isFxUserInterfaceAvailable()){
				return app.getFxUserInterface().getClipboardText();
			}
			else{
				return ConsoleMode.getClipboardText();
			}
		}

		if(varName.equals("_openfiledialog_")){
			if(app.isFxUserInterfaceAvailable()){
				FxUserInterface fxUI = app.getFxUserInterface();
				java.io.File f = fxUI.showOpenFileDialog(fxUI.getLocaleBundleString("filebrowser.dialogtitle"), "", null,
														 new FileChooser.ExtensionFilter[]{
																new FileChooser.ExtensionFilter(fxUI.getLocaleBundleString("filebrowser.allfiles_selector"), "*.*")});
				return (f != null) ? f.getAbsolutePath() : "";
			}
			else{
				return app.requestStringInput("Please enter a filename", "Please enter a filename:", "", false);
			}
		}

		if(varName.equals("_savefiledialog_")){
			if(app.isFxUserInterfaceAvailable()){
				FxUserInterface fxUI = app.getFxUserInterface();
				java.io.File f = fxUI.showSaveFileDialog(fxUI.getLocaleBundleString("filebrowser.dialogtitle"), "", null,
														 new FileChooser.ExtensionFilter[]{
																new FileChooser.ExtensionFilter(fxUI.getLocaleBundleString("filebrowser.allfiles_selector"), "*.*")});
				return (f != null) ? f.getAbsolutePath() : "";
			}
			else{
				return app.requestStringInput("Please enter a filename", "Please enter a filename:", "", false);
			}
		}

		return "";
	}
	
	private static void splitMultipleParameters(List<String> commands, Map<String, String> var, String delimeter, TreeNode node, ApplicationInstance app, String source)
	{
		source = replaceVariablesInString(var, node, app, source);
		for(String s: source.split(delimeter))
		{
			commands.add(s);
		}
	}

	/* The following code was written by StackOverflow (stackoverflow.com) user Mike Samuel and is licensed under CC BY-SA 3.0 
	 * "Creative Commons Attribution-ShareAlike 3.0 Unported", http://creativecommons.org/licenses/by-sa/3.0/)
	 *
	 * Source: http://stackoverflow.com/questions/6020384/create-array-of-regex-matches
	 * The code has not been modified.
	 */
	
	private static Iterable<MatchResult> allMatches(final Pattern p, final CharSequence input)
	{
		return new Iterable<MatchResult>()
		{
			public Iterator<MatchResult> iterator() {
				return new Iterator<MatchResult>() 
				{
					// Use a matcher internally.
					final Matcher matcher = p.matcher(input);
					// Keep a match around that supports any interleaving of hasNext/next calls.
					MatchResult pending;
	
					public boolean hasNext() {
					  // Lazily fill pending, and avoid calling find() multiple times if the
					  // clients call hasNext() repeatedly before sampling via next().
					  if (pending == null && matcher.find()) {
					    pending = matcher.toMatchResult();
					  }
					  return pending != null;
					}
	
					public MatchResult next() {
					  // Fill pending if necessary (as when clients call next() without
					  // checking hasNext()), throw if not possible.
						if (!hasNext()) { throw new NoSuchElementException(); }
						// Consume pending so next call to hasNext() does a find().
						MatchResult next = pending;
						pending = null;
						return next;
					}
	
					/** Required to satisfy the interface, but unsupported. */
					public void remove() { throw new UnsupportedOperationException(); }
				};
			}
		};
	}
}
