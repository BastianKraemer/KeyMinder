/* KeyMinder
 * Copyright (C) 2015-2016 Bastian Kraemer
 *
 * Sidebar.java
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
package de.akubix.keyminder.plugins.sidebar;

import java.util.ResourceBundle;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.locale.LocaleLoader;
import de.akubix.keyminder.ui.fx.JavaFxUserInterface;
import de.akubix.keyminder.ui.fx.JavaFxUserInterfaceApi;
import de.akubix.keyminder.ui.fx.sidebar.FxSidebar;
import de.akubix.keyminder.util.KeyValuePair;

public class Sidebar {
	public Sidebar(ApplicationInstance app){

		if(JavaFxUserInterface.isLoaded(app)){
			JavaFxUserInterfaceApi javaFxUserInterfaceApi = JavaFxUserInterface.getInstance(app);

			final ResourceBundle locale = LocaleLoader.loadLanguagePack("plugins", "sidebar", app.getLocale());

			FxSidebar sidebar = new FxSidebar(app, javaFxUserInterfaceApi);

			sidebar.addLabel(locale.getString("sidebar.username"));
			sidebar.addElementToSidebar(sidebar.createDefaultSidebarTextbox("username"), "username");

			sidebar.addLabel(locale.getString("sidebar.password"));
			sidebar.addElementToSidebar(sidebar.createDefaultSidebarPasswordbox("password"), "password");
			sidebar.addSeparator();

			sidebar.addLabel(locale.getString("sidebar.email"));
			sidebar.addElementToSidebar(sidebar.createDefaultSidebarHyperlink("email", true), "email");

			sidebar.addLabel(locale.getString("sidebar.website"));
			sidebar.addElementToSidebar(sidebar.createDefaultSidebarHyperlink("url", false), "url");

			sidebar.addLabel(locale.getString("sidebar.etc"));
			sidebar.addElementToSidebar(sidebar.createDefaultSidebarTextarea("etc"), "etc");

			sidebar.setUsernameAndPasswordSupplier(() -> new KeyValuePair<>(sidebar.getValueOf("username"), sidebar.getValueOf("password")));

			javaFxUserInterfaceApi.addSidebarPanel(locale.getString("sidebar.tabtitle"), sidebar, 0, true);
		}
	}
}
