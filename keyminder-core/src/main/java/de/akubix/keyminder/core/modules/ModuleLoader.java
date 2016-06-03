/*	KeyMinder
	Copyright (C) 2016 Bastian Kraemer

	ModuleLoader.java

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.akubix.keyminder.core.modules;

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
import de.akubix.keyminder.core.interfaces.Module;
import de.akubix.keyminder.core.interfaces.ModuleProperties;

public class ModuleLoader {
	private Map<String, ModuleInfo> allModules = new HashMap<String, ModuleInfo>();
	private ApplicationInstance app;
	public ModuleLoader(ApplicationInstance app){
		this.app = app;
	}

	public void loadModules(){
		List<String> enabledModules = Arrays.asList(app.getSettingsValue(ApplicationInstance.SETTINGS_KEY_ENABLED_MODULES).split(";"));

		Iterator<Module> moduleIterator = ServiceLoader.load(Module.class).iterator();
		while (moduleIterator.hasNext()) {
			Module m = (Module) moduleIterator.next();

			//Get the module description by reading the class annotation
			ModuleProperties moduleDescription = m.getClass().getAnnotation(de.akubix.keyminder.core.interfaces.ModuleProperties.class);

			if(moduleDescription != null){
				if(enabledModules.contains(moduleDescription.name())){
					allModules.put(moduleDescription.name(), new ModuleInfo(m, moduleDescription, true));
				}
				else{
					//Module is not enabled
					allModules.put(moduleDescription.name(), new ModuleInfo(m, moduleDescription, false));
				}
			}
		}

		//Start all modules, observing their dependencies to other modules
		for(String moduleName: enabledModules){
			startModule(moduleName, new ArrayList<String>());
		}
	}

	private void startModule(String moduleName, List<String> initiators){
		if(!allModules.containsKey(moduleName)){return;}

		ModuleInfo m = allModules.get(moduleName);
		if(!m.isStarted() && m.isEnabled()){
			String dependencies = (m.getProperties() == null ? "" : m.getProperties().dependencies().replace(" ", ""));
			if(!dependencies.equals("")){
				initiators.add(moduleName);
				for(String dependent_module: dependencies.split(";")){
					if(!initiators.contains(dependent_module)){
						startModule(dependent_module, initiators);
					}
					else{
						app.println(String.format("Warning: Cannot resolve module dependencies of '%s', because they are cyclic.", initiators.get(0)));
					}
				}
				initiators.remove(moduleName);
			}

			if(startModule(moduleName, m.getInstance())){
				m.setStarted();
			}
			else{
				allModules.put(moduleName, new ModuleInfo(null, m.getProperties(), true)); //Remove the instance from the module list
			}
		}
	}

	/**
	 * Starts a module an handles the errors if the start fails
	 * @param name
	 * @param moduleInstance
	 * @return {@code true} if the module has been successfully started, {@code false} if not
	 */
	private boolean startModule(String name, Module moduleInstance){
		if(moduleInstance == null){return false;}
		try {
			if(KeyMinder.environment.containsKey("verbose_mode")){
				app.println(String.format("Starting module \"%s\"... ", name));
			}

			moduleInstance.onStartup(app);
			return true;

		} catch (de.akubix.keyminder.core.exceptions.ModuleStartupException e) {
			switch(e.getErrorLevel()){
				case Critical:
					app.println("Critical error while loading module \"" + name + "\": " +e.getMessage());
					break;
				case Default:
					app.println("Cannot load module \"" + name + "\": " +e.getMessage());
					break;
				case FxUserInterfaceNotAvailable:
				case OSNotSupported:
					if(!KeyMinder.environment.containsKey("silent_mode")){
						app.println("Cannot load module \"" + name + "\": " +e.getMessage());
					}
					break;
			}
		}
		return false;
	}

	/**
	 * Enable a module (will be loaded on next start)
	 * @param moduleClassName class name of the module
	 * @throws IllegalArgumentException if the module does not exist
	 */
	public void enableModule(String moduleClassName) throws IllegalArgumentException{
		if(allModules.containsKey(moduleClassName)){
			if(app.settingsContainsKey(ApplicationInstance.SETTINGS_KEY_ENABLED_MODULES)){
				String currentlyEnabledModules = app.getSettingsValue(ApplicationInstance.SETTINGS_KEY_ENABLED_MODULES);
				List<String> enabledModules = Arrays.asList(currentlyEnabledModules.split(";"));
				if(!enabledModules.contains(moduleClassName)) {
					if(currentlyEnabledModules.endsWith(";")) {
						app.setSettingsValue(ApplicationInstance.SETTINGS_KEY_ENABLED_MODULES, currentlyEnabledModules + moduleClassName);
					}
					else {
						app.setSettingsValue(ApplicationInstance.SETTINGS_KEY_ENABLED_MODULES, currentlyEnabledModules + ";" + moduleClassName);
					}

					allModules.get(moduleClassName).setEnabled(true);
					app.saveSettings();
				}
			}
			else{
				app.setSettingsValue(ApplicationInstance.SETTINGS_KEY_ENABLED_MODULES, moduleClassName);
			}
		}
		else{
			throw new IllegalArgumentException("Module does not exist.");
		}
	}

	/**
	 * Disable a module (won't be loaded on next start)
	 * @param moduleClassName the class name of the module
	 * @throws IllegalArgumentException if the module does not exist
	 */
	public void disableModule(String moduleClassName) throws IllegalArgumentException {
		if(allModules.containsKey(moduleClassName)){
			if(app.settingsContainsKey(ApplicationInstance.SETTINGS_KEY_ENABLED_MODULES)){
				List<String> enabledModules = new ArrayList<>();
				for(String s: app.getSettingsValue(ApplicationInstance.SETTINGS_KEY_ENABLED_MODULES).split(";")){
					enabledModules.add(s);
				}

				if(enabledModules.contains(moduleClassName)){
					enabledModules.remove(moduleClassName);

					if(enabledModules.size() > 0){
						StringBuilder sb = new StringBuilder("");
						for(String moduleName: enabledModules){
							sb.append(moduleName + ";");
						}

						app.setSettingsValue(ApplicationInstance.SETTINGS_KEY_ENABLED_MODULES, sb.toString());
					}

					allModules.get(moduleClassName).setEnabled(false);
					app.saveSettings();
				}
			}
		}
		else{
			throw new IllegalArgumentException("Module does not exist.");
		}
	}

	/**
	 * Returns a list, respectively a Set of all modules
	 * @return a list (as Set) of all modules
	 */
	public Set<String> getModules(){
		return allModules.keySet();
	}

	/**
	 * Returns the properties of a specific module
	 * @param moduleName the name of the module
	 * @return The ModuleInfo OR null if the module does not exist respectively the module does not have any properties
	 */
	public ModuleInfo getModuleInfo(String moduleName){
		return allModules.get(moduleName);
	}
}
