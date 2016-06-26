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

import java.util.ResourceBundle;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.locale.LocaleLoader;
import de.akubix.keyminder.shell.CommandException;
import de.akubix.keyminder.ui.fx.sidebar.FxSidebar;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class Sidebar {
	public Sidebar(ApplicationInstance app){
		final ResourceBundle locale = LocaleLoader.loadLanguagePack("modules", "sidebar", app.getLocale());

		FxSidebar sidebar;
		sidebar = new FxSidebar(app, locale.getString("module.sidebar.tabtitle"), true, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				try {
					app.getShell().runShellCommand("keyclip");
				} catch (CommandException | UserCanceledOperationException e){
					app.alert(e.getMessage());
				}
		}});

		sidebar.addLabel(locale.getString("module.sidebar.username"));
		sidebar.addElementToSidebar(sidebar.createDefaultSidebarTextbox("username"), "username");

		sidebar.addLabel(locale.getString("module.sidebar.password"));
		sidebar.addElementToSidebar(sidebar.createDefaultSidebarPasswordbox("password"), "password");
		sidebar.addSeperator();

		sidebar.addLabel(locale.getString("module.sidebar.email"));
		sidebar.addElementToSidebar(sidebar.createDefaultSidebarHyperlink("email", true), "email");

		sidebar.addLabel(locale.getString("module.sidebar.website"));
		sidebar.addElementToSidebar(sidebar.createDefaultSidebarHyperlink("url", false), "url");

		sidebar.addLabel(locale.getString("module.sidebar.etc"));
		sidebar.addElementToSidebar(sidebar.createDefaultSidebarTextarea("etc"), "etc");
	}
}
