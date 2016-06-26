package de.akubix.keyminder.core.modules;

import java.util.Properties;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.exceptions.ModuleStartupException;

/**
 * Every KeyMinder module (or the factory for the module) has to implement this interface.
 * The {@link Module#startupModule(ApplicationInstance, Properties)} method is called when your module is enabled an should startup now.
 */
public interface Module {
	/**
	 * This method is called when your module should startup
	 * @param instance The application instance
	 * @param moduleProperties The properties file of your module (see {@link KeyMinderModule})
	 * @throws ModuleStartupException Can be thrown if there is any error during the startup
	 */
	public void startupModule(ApplicationInstance instance, Properties moduleProperties) throws ModuleStartupException;
}
