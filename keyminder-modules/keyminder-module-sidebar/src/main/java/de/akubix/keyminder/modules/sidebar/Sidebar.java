/*	KeyMinder
	Copyright (C) 2015-2016 Bastian Kraemer

	Sidebar.java

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
package de.akubix.keyminder.modules.sidebar;

import de.akubix.keyminder.core.exceptions.ModuleStartupException;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.shell.CommandException;
import de.akubix.keyminder.ui.fx.sidebar.FxSidebar;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

@de.akubix.keyminder.core.interfaces.ModuleProperties(
		name="Sidebar",
		description = "This module allows you to store usernames, passwords any many other informations in a single node.",
		version = ".",
		dependencies = "KeyClip",
		author="Bastian Kraemer")
public class Sidebar implements de.akubix.keyminder.core.interfaces.Module {

	private de.akubix.keyminder.core.ApplicationInstance app;
	private de.akubix.keyminder.core.interfaces.FxUserInterface fxUI;

	@Override
	public void onStartup(de.akubix.keyminder.core.ApplicationInstance instance) throws de.akubix.keyminder.core.exceptions.ModuleStartupException{

		if(!instance.isFxUserInterfaceAvailable()){throw new de.akubix.keyminder.core.exceptions.ModuleStartupException("JavaFX Interface not available", ModuleStartupException.ModuleErrorLevel.FxUserInterfaceNotAvailable);}
		this.app = instance;
		this.fxUI = instance.getFxUserInterface();

		FxSidebar sidebar;
		sidebar = new FxSidebar(app, fxUI.getLocaleBundleString("module.sidebar.tabtitle"), true, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					app.getShell().runShellCommand("keyclip");
				} catch (CommandException | UserCanceledOperationException e){
					app.alert(e.getMessage());
				}
		}});

		sidebar.addLabel(fxUI.getLocaleBundleString("module.sidebar.username"));
		sidebar.addElementToSidebar(sidebar.createDefaultSidebarTextbox("username"), "username");

		sidebar.addLabel(fxUI.getLocaleBundleString("module.sidebar.password"));
		sidebar.addElementToSidebar(sidebar.createDefaultSidebarPasswordbox("password"), "password");
		sidebar.addSeperator();

		sidebar.addLabel(fxUI.getLocaleBundleString("module.sidebar.email"));
		sidebar.addElementToSidebar(sidebar.createDefaultSidebarHyperlink("email", true), "email");

		sidebar.addLabel(fxUI.getLocaleBundleString("module.sidebar.website"));
		sidebar.addElementToSidebar(sidebar.createDefaultSidebarHyperlink("url", false), "url");

		sidebar.addLabel(fxUI.getLocaleBundleString("module.sidebar.etc"));
		sidebar.addElementToSidebar(sidebar.createDefaultSidebarTextarea("etc"), "etc");
	}
}
