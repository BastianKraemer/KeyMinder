package de.akubix.keyminder.plugins.sshtools;

import java.util.Properties;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.exceptions.PluginStartupException;
import de.akubix.keyminder.core.plugins.KeyMinderPlugin;
import de.akubix.keyminder.core.plugins.Plugin;

@KeyMinderPlugin(name = SSHTools.PLUGIN_NAME, properties = "/de/akubix/keyminder/plugins/SSHTools.properties")
public class SSHToolsFactory implements Plugin {
	@Override
	public Object startup(ApplicationInstance instance, Properties pluginProperties) throws PluginStartupException {
		return new SSHTools(instance);
	}
}
