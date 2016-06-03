package de.akubix.keyminder.core.modules;

import de.akubix.keyminder.core.interfaces.Module;
import de.akubix.keyminder.core.interfaces.ModuleProperties;

public class ModuleInfo {
	private final Module moduleInstance;
	private boolean isEnabled;
	private final ModuleProperties properties;
	private boolean moduleIsStarted;

	protected ModuleInfo(Module instance, ModuleProperties moduleProperties, boolean isEnabled){
		this.moduleInstance = instance;
		this.isEnabled = isEnabled;
		this.properties = moduleProperties;
		this.moduleIsStarted = false;
	}

	protected Module getInstance(){
		return this.moduleInstance;
	}

	public ModuleProperties getProperties(){
		return this.properties;
	}

	public boolean isStarted(){
		return moduleIsStarted;
	}

	public void setStarted(){
		this.moduleIsStarted = true;
	}

	public boolean isEnabled(){
		return isEnabled;
	}

	public void setEnabled(boolean value){
		this.isEnabled = value;
	}
}
