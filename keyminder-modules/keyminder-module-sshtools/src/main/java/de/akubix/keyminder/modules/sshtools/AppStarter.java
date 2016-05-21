/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	AppStarter.java

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.w3c.dom.Node;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.etc.MenuEntryPosition;
import de.akubix.keyminder.lib.Tools;
import de.akubix.keyminder.lib.gui.ImageSelector;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public class AppStarter {
	private ApplicationInstance app;
	private Supplier<org.w3c.dom.Document> xmldocument;
	private Menu contextMenuItemUsingSocks;
	public final boolean socksSupport;
	public final String socksProfileId;
	private final SSHTools sshtools;
	private final boolean enableMenuItems;

	private Map<String, MenuItem> socksItems = null;

	public AppStarter(ApplicationInstance app, SSHTools sshtools, Supplier<org.w3c.dom.Document> xmlDocumentSupplier) throws IllegalArgumentException{
		this(app, sshtools, false, xmlDocumentSupplier);
	}

	public AppStarter(ApplicationInstance app, SSHTools sshtools, boolean noItemsAndCommands, Supplier<org.w3c.dom.Document> xmlDocumentSupplier) throws IllegalArgumentException{
		this.app = app;
		this.xmldocument = xmlDocumentSupplier;
		this.sshtools = sshtools;
		this.enableMenuItems = !noItemsAndCommands;

		Node attrib = xmldocument.get().getDocumentElement().getAttributes().getNamedItem("name");
		if(attrib != null){

			String icon = "";
			Node iconAttrib = xmldocument.get().getDocumentElement().getAttributes().getNamedItem("icon");
			if(iconAttrib != null){icon = iconAttrib.getNodeValue();}

			Node canBeUsedWithSocks = xmldocument.get().getDocumentElement().getAttributes().getNamedItem("socks");
			if(canBeUsedWithSocks != null){
				socksProfileId = canBeUsedWithSocks.getNodeValue();
				socksSupport = true;
				socksItems = new HashMap<>();
			}
			else{
				contextMenuItemUsingSocks = null;
				socksProfileId = null;
				socksSupport = false;
			}
			if(!noItemsAndCommands){
				if(app.isFxUserInterfaceAvailable()){
					MenuItem contextMenuItem = Tools.createFxMenuItem(attrib.getNodeValue(), ImageSelector.getIcon(icon), (event) -> app.getFxUserInterface().updateStatus(sshtools.startApplication(this, false, null)));
					app.getFxUserInterface().addMenuEntry(contextMenuItem, MenuEntryPosition.CONTEXTMENU, true);

					if(socksSupport){
						if(icon.equals("")){
							contextMenuItemUsingSocks = new Menu(attrib.getNodeValue() + " using socks");
						}
						else{
							contextMenuItemUsingSocks = new Menu(attrib.getNodeValue() + " using socks", ImageSelector.getFxImageView(icon));
						}

						app.getFxUserInterface().addMenuEntry(contextMenuItemUsingSocks, MenuEntryPosition.CONTEXTMENU, true);
					}
				}

				Node commandNameAttribute = xmldocument.get().getDocumentElement().getAttributes().getNamedItem("command");
				String cmdName = commandNameAttribute == null ? attrib.getNodeValue().toLowerCase() : commandNameAttribute.getNodeValue();

				app.provideNewCommand(cmdName, (out, appInstance, args) -> {
					TreeNode node = null;
					if(args.length > 0){
						if(!args[0].startsWith("--")){
							node = appInstance.getTree().getNodeByPath(args[0]);
						}
					}
					else if(node == null){
						node = appInstance.getTree().getSelectedNode();
					}

					boolean ignoreForward = (de.akubix.keyminder.lib.Tools.arrayIndexOf(args, "--noforward", true) != -1);
					int socksprofileArgIndex = socksSupport ? de.akubix.keyminder.lib.Tools.arrayIndexOf(args, "--socks", true) : -1;
					if(socksprofileArgIndex != -1 && args.length > ++socksprofileArgIndex){
						out.println(sshtools.startApplication(this, node, ignoreForward, args[socksprofileArgIndex]));
					}
					else
					{
						out.println(sshtools.startApplication(this, node, ignoreForward, null));
					}
					return null;
				}, "(Module SSH-Tools) Starts the application " + cmdName + " using parameters stored in a treenode.\n" +
				   "Usage: " + cmdName + " [nodename] [--noforward] " + (socksSupport ? "[--socks socksprofile]" : ""));
			}
		}
		else{
			throw new IllegalArgumentException("Invalid XML-Document, the execution profile must have a \"name\" attribute.");
		}
	}

	public List<String> getCommandLineArgs(Map<String, String> predefinedVariables, String id, TreeNode treeNode) throws IllegalArgumentException{
		if(id == null){id = "default";}
		XMLApplicationProfileParser xapp = new XMLApplicationProfileParser(app, xmldocument.get(), predefinedVariables);
		return xapp.generateCommandLineParameters(id, treeNode);
	}

	public void createUsingSocksItem(String socksProfileId, String socksProfileName){
		if(socksSupport && enableMenuItems){
			MenuItem m = Tools.createFxMenuItem(socksProfileName, "", (event) -> app.getFxUserInterface().updateStatus(sshtools.startApplication(this, false, socksProfileId)));
			contextMenuItemUsingSocks.getItems().add(m);
			m.setDisable(true);
			socksItems.put(socksProfileId, m);
			contextMenuItemUsingSocks.setVisible(true);
		}
	}
	public void enableSocksItem(String socksProfileId, boolean value){
		if(socksSupport && enableMenuItems){
			if(socksItems.containsKey(socksProfileId)){
				socksItems.get(socksProfileId).setDisable(!value);
			}
		}
	}

	public void clearSocksItems(){
		if(socksSupport && enableMenuItems){
			socksItems.clear();
			contextMenuItemUsingSocks.getItems().clear();
			contextMenuItemUsingSocks.setVisible(false);
		}
	}
}
