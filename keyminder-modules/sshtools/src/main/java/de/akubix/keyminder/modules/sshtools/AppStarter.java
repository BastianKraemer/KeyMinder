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
import de.akubix.keyminder.ui.fx.JavaFxUserInterface;
import de.akubix.keyminder.ui.fx.JavaFxUserInterfaceApi;
import de.akubix.keyminder.ui.fx.MenuEntryPosition;
import de.akubix.keyminder.ui.fx.utils.FxCommons;
import de.akubix.keyminder.ui.fx.utils.ImageMap;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public class AppStarter {
	private ApplicationInstance app;
	private Supplier<org.w3c.dom.Document> xmldocument;
	private Menu contextMenuItemUsingSocks;
	private final boolean socksSupport;
	private final String name;
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
			name = attrib.getNodeValue().toLowerCase();
			String icon = "";
			Node iconAttrib = xmldocument.get().getDocumentElement().getAttributes().getNamedItem("icon");
			if(iconAttrib != null){icon = iconAttrib.getNodeValue();}

			Node canBeUsedWithSocks = xmldocument.get().getDocumentElement().getAttributes().getNamedItem("socks");
			if(canBeUsedWithSocks != null){
				socksSupport = true;
				socksItems = new HashMap<>();
			}
			else{
				contextMenuItemUsingSocks = null;
				socksSupport = false;
			}
			if(!noItemsAndCommands){
				if(JavaFxUserInterface.isLoaded(app)){
					final JavaFxUserInterfaceApi fxUI = JavaFxUserInterface.getInstance(this.app);

					fxUI.addMenuEntry(
						FxCommons.createFxMenuItem(
							attrib.getNodeValue(),
							ImageMap.getIcon(icon),
							(event) -> fxUI.updateStatus(sshtools.startApplication(this, false, null))
						),
						MenuEntryPosition.CONTEXTMENU, true);

					if(socksSupport){
						if(icon.equals("")){
							contextMenuItemUsingSocks = new Menu(attrib.getNodeValue() + " using socks");
						}
						else{
							contextMenuItemUsingSocks = new Menu(attrib.getNodeValue() + " using socks", ImageMap.getFxImageView(icon));
						}

						fxUI.addMenuEntry(contextMenuItemUsingSocks, MenuEntryPosition.CONTEXTMENU, true);
					}
				}
			}
		}
		else{
			throw new IllegalArgumentException("Invalid XML-Document, the execution profile must have a \"name\" attribute.");
		}
	}

	public String getName(){
		return this.name;
	}

	public List<String> getCommandLineArgs(Map<String, String> predefinedVariables, String id, TreeNode treeNode) throws IllegalArgumentException{
		if(id == null){id = "default";}
		CommandLineGenerator cmdGen = new CommandLineGenerator(app, xmldocument.get(), predefinedVariables);
		return cmdGen.generateCommandLineParameters(id, treeNode);
	}

	public void createUsingSocksItem(String socksProfileId, String socksProfileName, JavaFxUserInterfaceApi fxUI){
		if(socksSupport && enableMenuItems){
			MenuItem m = FxCommons.createFxMenuItem(socksProfileName, "", (event) -> fxUI.updateStatus(sshtools.startApplication(this, false, socksProfileId)));
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
