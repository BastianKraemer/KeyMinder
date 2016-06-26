package de.akubix.keyminder.core.modules;

import java.util.Properties;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.exceptions.ModuleStartupException;

public interface Module {
	public void startupModule(ApplicationInstance instance, Properties moduleProperties) throws ModuleStartupException;
}
