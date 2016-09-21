package de.akubix.keyminder.script;

import de.akubix.keyminder.core.ApplicationInstance;

public abstract class AbstractScriptEnvironment {
	protected final ApplicationInstance app;

	protected AbstractScriptEnvironment(ApplicationInstance app){
		this.app = app;
	}

	public abstract String lookup(String varName);
	public abstract boolean isDefined(String varName);
}
