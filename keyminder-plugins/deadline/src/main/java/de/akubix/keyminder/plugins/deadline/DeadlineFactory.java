package de.akubix.keyminder.plugins.deadline;

import java.util.Properties;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.exceptions.PluginStartupException;
import de.akubix.keyminder.core.plugins.KeyMinderPlugin;
import de.akubix.keyminder.core.plugins.Plugin;

@KeyMinderPlugin(name = "Deadline", properties = "/de/akubix/keyminder/plugins/Deadline.properties")
public class DeadlineFactory implements Plugin {
	@Override
	public Object startup(ApplicationInstance instance, Properties pluginProperties) throws PluginStartupException {
		return new Deadline(instance);
	}
}
