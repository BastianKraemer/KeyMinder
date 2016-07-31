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
import de.akubix.keyminder.locale.LocaleLoader;
import de.akubix.keyminder.ui.fx.JavaFxUserInterface;
import de.akubix.keyminder.ui.fx.JavaFxUserInterfaceApi;
import de.akubix.keyminder.ui.fx.sidebar.FxSidebar;

public class Sidebar {
	public Sidebar(ApplicationInstance app){

		if(JavaFxUserInterface.isLoaded(app)){
			JavaFxUserInterfaceApi javaFxUserInterfaceApi = JavaFxUserInterface.getInstance(app);

			final ResourceBundle locale = LocaleLoader.loadLanguagePack("modules", "sidebar", app.getLocale());

			FxSidebar sidebar = new FxSidebar(app, javaFxUserInterfaceApi);

			sidebar.addLabel(locale.getString("module.sidebar.username"));
			sidebar.addElementToSidebar(sidebar.createDefaultSidebarTextbox("username"), "username");

			sidebar.addLabel(locale.getString("module.sidebar.password"));
			sidebar.addElementToSidebar(sidebar.createDefaultSidebarPasswordbox("password"), "password");
			sidebar.addSeparator();

			sidebar.addLabel(locale.getString("module.sidebar.email"));
			sidebar.addElementToSidebar(sidebar.createDefaultSidebarHyperlink("email", true), "email");

			sidebar.addLabel(locale.getString("module.sidebar.website"));
			sidebar.addElementToSidebar(sidebar.createDefaultSidebarHyperlink("url", false), "url");

			sidebar.addLabel(locale.getString("module.sidebar.etc"));
			sidebar.addElementToSidebar(sidebar.createDefaultSidebarTextarea("etc"), "etc");

			javaFxUserInterfaceApi.addSidebarPanel(locale.getString("module.sidebar.tabtitle"), sidebar, 0, true);
		}
	}
}
