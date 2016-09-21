/* KeyMinder
 * Copyright (C) 2015-2016 Bastian Kraemer
 *
 * CommandLineGenerator.java
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
package de.akubix.keyminder.modules.sshtools;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.script.ScriptException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.KeyMinder;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.script.AbstractScriptEnvironment;
import de.akubix.keyminder.script.ScriptFramework;
import de.akubix.keyminder.shell.Shell;

/**
 * The {@link CommandLineGenerator} class is used to generate the command line string for every application you want to start.
 * This is done by parsing an XML file which defines the command line interface of each application.
 * It's also possible to append some data of your tree to the command line. Just add a variable to the XML file and it will be replaced
 * with some information out of your file, for example ${username} will be replaced by the user name which is stored in currently selected tree node.
 */
public class CommandLineGenerator {

	static final Function<String, String> _DEFAULT_RESOURCE_CONTENT_LOADER = (resourcePath) -> {
		try {
			return new String(Files.readAllBytes(Paths.get(CommandLineGenerator.class.getResource(resourcePath).toURI())));
		} catch (IOException | URISyntaxException e) {
			throw new IllegalArgumentException(String.format("Unable to load file '%s': %s", resourcePath, e.getMessage()));
		}
	};

	private final ApplicationInstance app;
	private final Document xmldoc;
	private final Map<String, String> variables;
	private final Function<String, String> resourceContentLoader;

	public CommandLineGenerator(ApplicationInstance app, Document xmldoc, Map<String, String> variables, Function<String, String> resourceContentLoader){
		this.app = app;
		this.xmldoc = xmldoc;
		this.variables = variables;
		this.resourceContentLoader = resourceContentLoader;
	}

	/**
	 * Generates the parameter list for a process defined by its command line descriptor
	 * @param profileIdentifier the identifier to select the configuration that should be used
	 * @param treeNode the currently selected TreeNode (maybe some attributes are referenced by a variable)
	 * @return the list of parameters for your profile (including the application path as first argument)
	 * @throws IllegalArgumentException if the descriptor doesn't contain a profile with the give identifier
	 * @throws ScriptException
	 * @throws DOMException
	 */
	public List<String> generateCommandLineParameters(String profileIdentifier, TreeNode treeNode) throws IllegalArgumentException, DOMException {

		try {
			AbstractScriptEnvironment env = new AbstractScriptEnvironment(app) {
				@Override
				public String lookup(String varName) {
					try{
						return app.lookup(varName, treeNode, variables);
					}
					catch(IllegalArgumentException e){
						return "";
					}
				}

				@Override
				public boolean isDefined(String varName) {
					return app.variableIsDefined(varName, treeNode, variables);
				}
			};

			ScriptFramework scriptFramework = new ScriptFramework(() -> env);

			//Verify that the profile is compatible with this parser
			org.w3c.dom.Node versionAttrib = xmldoc.getDocumentElement().getAttributes().getNamedItem("version");
			if(versionAttrib != null){
				if(!versionAttrib.getNodeValue().matches("^0\\.1(\\.[0-9]+)?$")){
					throw new IllegalArgumentException(String.format("Unsupported version of the command line descriptor (Supported Versions: 0.1.x, Version of document: '%s')", versionAttrib.getNodeValue()));
				}
			}

			org.w3c.dom.Node rootNode = xmldoc.getDocumentElement();
			for(int i = 0; i < rootNode.getChildNodes().getLength(); i++){
				org.w3c.dom.Node childNode = rootNode.getChildNodes().item(i);
				if(childNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE){

					if(childNode.getNodeName().matches("(?i)script")){

						try{
							scriptFramework.eval(getScriptSource(childNode));
						}
						catch(ScriptException e){
							if(childNode.getAttributes().getNamedItem("src") == null){
								throw e;
							}
							else{
								throw new ScriptException(String.format("Error in script '%s':\n%s", childNode.getAttributes().getNamedItem("src").getNodeValue(), e.getMessage(), e));
							}
						}
					}
					else if(childNode.getNodeName().matches("(?i)profile")){
						org.w3c.dom.Node attrib = childNode.getAttributes().getNamedItem("id");
						if(attrib != null){
							if(attrib.getNodeValue().equals(profileIdentifier)){

								org.w3c.dom.Node execAttrib = childNode.getAttributes().getNamedItem("execute");
								if(execAttrib == null || execAttrib.getNodeValue().equals("")){
									throw new IllegalArgumentException("The command line descriptor does not contain the executable that should be started.");
								}

								String exec = replaceVariablesInString(execAttrib.getNodeValue(), treeNode);
								if(exec.equals("")){
									if(execAttrib.getNodeValue().trim().equals("")){
										throw new IllegalArgumentException(String.format("Executable is not defined: Value of '%s' ist empty.", execAttrib.getNodeName()));
									}
									else{
										throw new IllegalArgumentException(String.format("Executable is not defined: '%s' can't be resolved.", execAttrib.getNodeValue()));
									}
								}

								checkRequirements(childNode, treeNode);

								if(KeyMinder.verbose_mode){
									app.println(String.format("Generating command line parameters for application '%s'...", exec));
								}

								CommandLineResult result = scriptFramework.eval(getScriptSource(childNode), "profile", new CommandLineResult(exec));
								if(result.hasFailed()){
									throw new IllegalArgumentException(result.getFailReason());
								}
								return result.getArguments();
							}
						}
					}
					else{
						variables.put(childNode.getNodeName(), childNode.getNodeValue());
					}
				}
			}
		}
		catch(ScriptException | IOException | URISyntaxException e){
			throw new IllegalArgumentException(e.getMessage(), e);
		}

		throw new IllegalArgumentException(String.format("Unable to find profile with identifier '%s'.", profileIdentifier));
	}

	/**
	 * The root node of the XML document may contain an attribute called "require"
	 * The task of this method is to check those requirements. This will fail if one of those variables does not exist or only contain "".
	 * @throws IllegalArgumentException if at least one requirement of this profile are not fulfilled
	 */
	private void checkRequirements(org.w3c.dom.Node profileRootNode, TreeNode treeNode) throws IllegalArgumentException {
		org.w3c.dom.Node attrib = profileRootNode.getAttributes().getNamedItem("require");
		if(attrib != null){
			for(String str: attrib.getNodeValue().split(";\\ |;")){
				if(replaceVariablesInString(str, treeNode).equals("")){
					throw new IllegalArgumentException(String.format("Unable to launch application: Variable %s is empty or does not exist", str));
				}
			}
		}
	}

	private String replaceVariablesInString(String source, TreeNode treeNode){
		return Shell.replaceVariables(source, (var) -> app.lookup(var, treeNode, variables));
	}

	private String getScriptSource(org.w3c.dom.Node xmlNode) throws DOMException, IOException, URISyntaxException{
		if(xmlNode.getAttributes().getNamedItem("src") == null){
			String val = xmlNode.getNodeValue();
			if(val == null && xmlNode.getChildNodes().getLength() == 1){
				return xmlNode.getChildNodes().item(0).getNodeValue();
			}
			return val;
		}
		else{
			return resourceContentLoader.apply(xmlNode.getAttributes().getNamedItem("src").getNodeValue());
		}
	}
}
