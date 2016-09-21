package de.akubix.keyminder.script;

import de.akubix.keyminder.core.ApplicationInstance;

class SimpleScriptEnvironment extends AbstractScriptEnvironment {

	SimpleScriptEnvironment(ApplicationInstance app){
		super(app);
	}

	@Override
	public String lookup(String varName){
		return app.lookup(varName);
	}

	@Override
	public boolean isDefined(String varName){
		return app.variableIsDefined(varName);
	}
}
