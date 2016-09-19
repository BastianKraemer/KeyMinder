package de.akubix.keyminder.modules.sidebar;

import java.util.Properties;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.exceptions.ModuleStartupException;
import de.akubix.keyminder.core.modules.KeyMinderModule;
import de.akubix.keyminder.core.modules.Module;
import de.akubix.keyminder.core.modules.RequireUserInterface;
import de.akubix.keyminder.ui.fx.JavaFxUserInterface;

@KeyMinderModule(name = "Sidebar", properties = "/de/akubix/keyminder/modules/Sidebar.properties")
@RequireUserInterface(JavaFxUserInterface.USER_INTERFACE_ID)
public class SidebarFactory implements Module {
	@Override
	public Object startupModule(ApplicationInstance instance, Properties moduleProperties) throws ModuleStartupException {
		return new Sidebar(instance);
	}
}
