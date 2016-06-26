package de.akubix.keyminder.core.modules;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class ModuleInfo {
	private Module moduleInstace;
	private final String propertiesFile;
	private boolean isEnabled;
	private final boolean requiredUIisAvailable;
	private final String requiredUIName;
	private boolean moduleIsStarted;

	protected ModuleInfo(Module moduleInstace, String propertiesFile, boolean requiredUIisAvailable, String requiredUIName, boolean isEnabled){
		this.moduleInstace = moduleInstace;
		this.propertiesFile = propertiesFile;
		this.requiredUIisAvailable = requiredUIisAvailable;
		this.requiredUIName = requiredUIName;
		this.isEnabled = isEnabled;
		this.moduleIsStarted = false;
	}

	public Module getInstance(){
		return this.moduleInstace;
	}

	public Properties getProperties() throws NullPointerException, IOException {
		Properties properties = new Properties();
		properties.load(new InputStreamReader(getClass().getResourceAsStream(this.propertiesFile), "UTF-8"));
		return properties;
	}

	public boolean isStarted(){
		return moduleIsStarted;
	}

	public void setStarted(){
		this.moduleIsStarted = true;
	}

	public void startFailed(){
		this.moduleIsStarted = false;
		this.moduleInstace = null;
	}

	public boolean isEnabled(){
		return isEnabled;
	}

	public void setEnabled(boolean value){
		this.isEnabled = value;
	}

	public boolean requiredUIisAvailable(){
		return this.requiredUIisAvailable;
	}

	public String getRequiredUIName(){
		return this.requiredUIName;
	}
}
