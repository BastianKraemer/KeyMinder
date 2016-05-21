package de.akubix.keyminder.core.interfaces;

import de.akubix.keyminder.core.exceptions.ModuleStartupException;

public interface Module {

	public void onStartup(de.akubix.keyminder.core.ApplicationInstance instance) throws ModuleStartupException;
}
