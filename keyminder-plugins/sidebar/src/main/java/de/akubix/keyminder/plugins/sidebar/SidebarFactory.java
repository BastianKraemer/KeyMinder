package de.akubix.keyminder.plugins.sidebar;

import java.util.Properties;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.exceptions.PluginStartupException;
import de.akubix.keyminder.core.plugins.KeyMinderPlugin;
import de.akubix.keyminder.core.plugins.Plugin;
import de.akubix.keyminder.core.plugins.RequireUserInterface;
import de.akubix.keyminder.ui.fx.JavaFxUserInterface;

@KeyMinderPlugin(name = "Sidebar", properties = "/de/akubix/keyminder/plugins/Sidebar.properties")
@RequireUserInterface(JavaFxUserInterface.USER_INTERFACE_ID)
public class SidebarFactory implements Plugin {
	@Override
	public Object startup(ApplicationInstance instance, Properties pluginProperties) throws PluginStartupException {
		return new Sidebar(instance);
	}
}
