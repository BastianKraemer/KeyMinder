package de.akubix.keyminder.modules.sshtools;

import java.util.Properties;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.exceptions.ModuleStartupException;
import de.akubix.keyminder.core.modules.KeyMinderModule;
import de.akubix.keyminder.core.modules.Module;

@KeyMinderModule(name = "SSHTools", properties = "/de/akubix/keyminder/modules/SSHTools.properties")
public class SSHToolsFactory implements Module {
	@Override
	public Object startupModule(ApplicationInstance instance, Properties moduleProperties) throws ModuleStartupException {
		return new SSHTools(instance);
	}
}
