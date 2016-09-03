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
import de.akubix.keyminder.core.exceptions.ModuleStartupException;

/**
 * This class is used manage all modules.
 * It loads all available modules by using the Java {@link ServiceLoader} and handles the module configuration (enable/disable modules)
 */
public class ModuleLoader {
	private Map<String, ModuleInfo> allModules = new HashMap<>();
	private ApplicationInstance app;
	public ModuleLoader(ApplicationInstance app){
		this.app = app;
	}

	/**
	 * Loads all modules in the class path using the Java {@link ServiceLoader}
	 */
	public void loadModules(){
		List<String> enabledModules = Arrays.asList(app.getSettingsValue(ApplicationInstance.SETTINGS_KEY_ENABLED_MODULES).split(";"));

		Iterator<Module> moduleIterator = ServiceLoader.load(Module.class).iterator();
		while (moduleIterator.hasNext()) {
			Module m = (Module) moduleIterator.next();

			//Get the module description by reading the class annotation
			KeyMinderModule moduleAnnotation = m.getClass().getAnnotation(KeyMinderModule.class);

			if(moduleAnnotation != null){

				RequireUserInterface uiRequirement = m.getClass().getAnnotation(RequireUserInterface.class);

				boolean uiCheck = true;
				String requiredUserInterfaceName = null;

				if(uiRequirement != null){
					requiredUserInterfaceName = uiRequirement.value();
					uiCheck = app.getUserInterfaceInformation().id().equals(requiredUserInterfaceName);
				}

				allModules.put(
					moduleAnnotation.name(),
					new ModuleInfo(m, moduleAnnotation.properties(), uiCheck, requiredUserInterfaceName, enabledModules.contains(moduleAnnotation.name()))
				);
			}
			else if(KeyMinder.verbose_mode){
				app.log(String.format("Cannot load Module '%s'. Missing annotation '@%s'.", m.getClass().getName(), KeyMinderModule.class.getName()));
			}
		}

		//Start all modules, observing their dependencies to other modules
		for(String moduleName: enabledModules){
			startModule(moduleName);
		}
	}

	/**
	 * Starts a module
	 * @param moduleName The name of the module
	 */
	private void startModule(String moduleName){
		if(!allModules.containsKey(moduleName)){return;}

		ModuleInfo m = allModules.get(moduleName);
		if(!m.isStarted() && m.isEnabled()){
			if(!m.requiredUIisAvailable()){
				app.log(String.format("Cannot start module '%s': Required user interface '%s' is not available.", moduleName, m.getRequiredUIName()));
				m.startFailed();
				return;
			}

			if(callModuleStartupMethod(moduleName, m)){
				m.setStarted();
			}
			else{
				m.startFailed();
			}
		}
	}

	/**
	 * Starts a module an handles the errors if the start fails
	 * @param name
	 * @param moduleInfo
	 * @return {@code true} if the module has been successfully started, {@code false} if not
	 */
	private boolean callModuleStartupMethod(String name, ModuleInfo moduleInfo){

		Module moduleInstance = moduleInfo.getInstance();
		if(moduleInstance == null){return false;}

		try {
			if(KeyMinder.environment.containsKey("verbose_mode")){
				app.println(String.format("Starting module \"%s\"... ", name));
			}

			moduleInstance.startupModule(app, moduleInfo.getProperties());
			return true;
		}
		catch (IOException | NullPointerException e){
			app.log(String.format("Start of module '%s' failed: Unable to load module property file.", name));
		}
		catch (ModuleStartupException e) {
			switch(e.getErrorLevel()){
				case Critical:
					app.log(String.format("Critical error while loading module '%s': %s", name, e.getMessage()));
					break;
				case Default:
					app.log(String.format("Cannot load module '%s': %s", name, e.getMessage()));
					break;
				case OSNotSupported:
					if(!KeyMinder.environment.containsKey("silent_mode")){
						app.log(String.format("Cannot load module '%s': %s", name, e.getMessage()));
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
