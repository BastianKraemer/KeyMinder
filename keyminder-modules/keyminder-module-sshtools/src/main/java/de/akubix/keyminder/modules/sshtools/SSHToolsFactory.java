package de.akubix.keyminder.modules.sshtools;

import java.util.Properties;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.exceptions.ModuleStartupException;
import de.akubix.keyminder.core.modules.KeyMinderModule;
import de.akubix.keyminder.core.modules.Module;
import de.akubix.keyminder.core.modules.Preload;

@KeyMinderModule(name = "SSHTools", properties = "/de/akubix/keyminder/modules/SSHTools.properties")
@Preload({"Sidebar", "KeyClip"})
public class SSHToolsFactory implements Module {
	@Override
	public void startupModule(ApplicationInstance instance, Properties moduleProperties) throws ModuleStartupException {
		new SSHTools(instance);
	}
}
