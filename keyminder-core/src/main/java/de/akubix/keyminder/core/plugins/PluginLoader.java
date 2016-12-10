/* KeyMinder
 * Copyright (C) 2016 Bastian Kraemer
 *
 * PluginLoader.java
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
package de.akubix.keyminder.core.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.KeyMinder;
import de.akubix.keyminder.core.exceptions.PluginStartupException;

/**
 * This class is used manage all plugins.
 * It loads all available plugins by using the Java {@link ServiceLoader} and handles the plugin configuration (enable/disable plugins)
 */
public class PluginLoader {
	private Map<String, PluginInfo> allPlugins = new HashMap<>();
	private ApplicationInstance app;
	public PluginLoader(ApplicationInstance app){
		this.app = app;
	}

	/**
	 * Loads all plugins in the class path using the Java {@link ServiceLoader}
	 */
	public void loadPlugins(){
		List<String> enabledPlugins = Arrays.asList(app.getSettingsValue(ApplicationInstance.SETTINGS_KEY_ENABLED_PLUGINS).split(";"));

		Iterator<Plugin> pluginIterator = ServiceLoader.load(Plugin.class).iterator();
		while (pluginIterator.hasNext()) {
			Plugin m = (Plugin) pluginIterator.next();

			//Get the plugin description by reading the class annotation
			KeyMinderPlugin pluginAnnotation = m.getClass().getAnnotation(KeyMinderPlugin.class);

			if(pluginAnnotation != null){

				RequireUserInterface uiRequirement = m.getClass().getAnnotation(RequireUserInterface.class);

				boolean uiCheck = true;
				String requiredUserInterfaceName = null;

				if(uiRequirement != null){
					requiredUserInterfaceName = uiRequirement.value();
					uiCheck = app.getUserInterfaceInformation().id().equals(requiredUserInterfaceName);
				}

				allPlugins.put(
					pluginAnnotation.name(),
					new PluginInfo(m, pluginAnnotation.properties(), uiCheck, requiredUserInterfaceName, enabledPlugins.contains(pluginAnnotation.name()))
				);
			}
			else if(KeyMinder.verbose_mode){
				app.log(String.format("Cannot load plugin '%s'. Missing annotation '@%s'.", m.getClass().getName(), KeyMinderPlugin.class.getName()));
			}
		}

		// Start all plugins
		for(String pluginName: enabledPlugins){
			startPlugin(pluginName);
		}
	}

	/**
	 * Starts a plugin
	 * @param pluginName The name of the plugin
	 */
	private void startPlugin(String pluginName){
		if(!allPlugins.containsKey(pluginName)){return;}

		PluginInfo m = allPlugins.get(pluginName);
		if(!m.isStarted() && m.isEnabled()){
			if(!m.requiredUIisAvailable()){
				app.log(String.format("Cannot start plugin '%s': Required user interface '%s' is not available.", pluginName, m.getRequiredUIName()));
				m.startFailed();
				return;
			}

			Object pluginInstance = callPluginStartupMethod(pluginName, m);
			if(pluginInstance != null){
				m.setStarted(pluginInstance);
			}
			else{
				m.startFailed();
			}
		}
	}

	/**
	 * Starts a plugin an handles the errors if the start fails
	 * @param name
	 * @param pluginInfo
	 * @return {@code true} if the plugin has been successfully started, {@code false} if not
	 */
	private Object callPluginStartupMethod(String name, PluginInfo pluginInfo) {

		Plugin pluginFactory = pluginInfo.getFactory();
		if(pluginFactory == null){return false;}

		try {
			if(KeyMinder.environment.containsKey("verbose_mode")){
				app.println(String.format("Starting plugin \"%s\"... ", name));
			}

			return pluginFactory.startup(app, pluginInfo.getProperties());
		}
		catch (IOException | NullPointerException e){
			app.log(String.format("Start of plugin '%s' failed: Unable to load plugin property file.", name));
		}
		catch (PluginStartupException e) {
			switch(e.getErrorLevel()){
				case Critical:
					app.log(String.format("Critical error while loading plugin '%s': %s", name, e.getMessage()));
					break;
				case Default:
					app.log(String.format("Cannot load plugin '%s': %s", name, e.getMessage()));
					break;
				case OSNotSupported:
					if(!KeyMinder.environment.containsKey("silent_mode")){
						app.log(String.format("Cannot load plugin '%s': %s", name, e.getMessage()));
					}
					break;
			}
		}
		return null;
	}

	/**
	 * Enable a plugin (will be loaded on next start)
	 * @param pluginClassName class name of the plugin
	 * @throws IllegalArgumentException if the plugin does not exist
	 */
	public void enablePlugin(String pluginClassName) throws IllegalArgumentException{
		if(allPlugins.containsKey(pluginClassName)){
			if(app.settingsContainsKey(ApplicationInstance.SETTINGS_KEY_ENABLED_PLUGINS)){
				String currentlyEnabledPlugins = app.getSettingsValue(ApplicationInstance.SETTINGS_KEY_ENABLED_PLUGINS);
				List<String> enabledPlugins = Arrays.asList(currentlyEnabledPlugins.split(";"));
				if(!enabledPlugins.contains(pluginClassName)) {
					if(currentlyEnabledPlugins.endsWith(";")) {
						app.setSettingsValue(ApplicationInstance.SETTINGS_KEY_ENABLED_PLUGINS, currentlyEnabledPlugins + pluginClassName);
					}
					else {
						app.setSettingsValue(ApplicationInstance.SETTINGS_KEY_ENABLED_PLUGINS, currentlyEnabledPlugins + ";" + pluginClassName);
					}

					allPlugins.get(pluginClassName).setEnabled(true);
					app.saveSettings();
				}
			}
			else{
				app.setSettingsValue(ApplicationInstance.SETTINGS_KEY_ENABLED_PLUGINS, pluginClassName);
			}
		}
		else{
			throw new IllegalArgumentException("plugin does not exist.");
		}
	}

	/**
	 * Disable a plugin (won't be loaded on next start)
	 * @param pluginClassName the class name of the plugin
	 * @throws IllegalArgumentException if the plugin does not exist
	 */
	public void disablePlugin(String pluginClassName) throws IllegalArgumentException {
		if(allPlugins.containsKey(pluginClassName)){
			if(app.settingsContainsKey(ApplicationInstance.SETTINGS_KEY_ENABLED_PLUGINS)){
				List<String> enabledPlugins = new ArrayList<>();
				for(String s: app.getSettingsValue(ApplicationInstance.SETTINGS_KEY_ENABLED_PLUGINS).split(";")){
					enabledPlugins.add(s);
				}

				if(enabledPlugins.contains(pluginClassName)){
					enabledPlugins.remove(pluginClassName);

					if(enabledPlugins.size() > 0){
						StringBuilder sb = new StringBuilder("");
						for(String pluginName: enabledPlugins){
							sb.append(pluginName + ";");
						}

						app.setSettingsValue(ApplicationInstance.SETTINGS_KEY_ENABLED_PLUGINS, sb.toString());
					}

					allPlugins.get(pluginClassName).setEnabled(false);
					app.saveSettings();
				}
			}
		}
		else{
			throw new IllegalArgumentException("plugin does not exist.");
		}
	}

	/**
	 * Returns a list, respectively a Set of all plugins
	 * @return a list (as Set) of all plugins
	 */
	public Set<String> getPlugins(){
		return allPlugins.keySet();
	}

	/**
	 * Returns the properties of a specific plugin
	 * @param pluginName the name of the plugin
	 * @return The {@link PluginInfo} OR null if the plugin does not exist respectively the plugin does not have any properties
	 */
	public PluginInfo getPluginInfo(String pluginName){
		return allPlugins.get(pluginName);
	}
}
