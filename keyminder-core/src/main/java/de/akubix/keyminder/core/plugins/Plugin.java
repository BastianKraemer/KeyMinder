package de.akubix.keyminder.core.plugins;

import java.util.Properties;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.exceptions.PluginStartupException;

/**
 * Every KeyMinder plugin (or the factory for the plugin) has to implement this interface.
 * The {@link Plugin#startup(ApplicationInstance, Properties)} method is called when your plugin is enabled an should startup now.
 */
public interface Plugin {
	/**
	 * This method is called when your plugin should startup
	 * @param instance The application instance
	 * @param pluginProperties The properties file of your plugin (see {@link KeyMinderPlugin})
	 * @throws PluginStartupException Can be thrown if there is any error during the startup
	 */
	public Object startup(ApplicationInstance instance, Properties pluginProperties) throws PluginStartupException;
}
