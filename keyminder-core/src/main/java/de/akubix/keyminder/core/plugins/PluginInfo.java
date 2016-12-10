package de.akubix.keyminder.core.plugins;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class PluginInfo {
	private Plugin pluginFactory;
	private Object pluginInstance;
	private final String propertiesFile;
	private boolean isEnabled;
	private final boolean requiredUIisAvailable;
	private final String requiredUIName;
	private boolean pluginIsStarted;

	protected PluginInfo(Plugin pluginFactory, String propertiesFile, boolean requiredUIisAvailable, String requiredUIName, boolean isEnabled){
		this.pluginFactory = pluginFactory;
		this.propertiesFile = propertiesFile;
		this.requiredUIisAvailable = requiredUIisAvailable;
		this.requiredUIName = requiredUIName;
		this.isEnabled = isEnabled;
		this.pluginIsStarted = false;
	}

	public Plugin getFactory(){
		return this.pluginFactory;
	}

	public Object getInstance(){
		return this.pluginInstance;
	}

	public Properties getProperties() throws NullPointerException, IOException {
		Properties properties = new Properties();
		properties.load(new InputStreamReader(getClass().getResourceAsStream(this.propertiesFile), "UTF-8"));
		return properties;
	}

	public boolean isStarted(){
		return pluginIsStarted;
	}

	public void setStarted(Object pluginInstance){
		this.pluginIsStarted = true;
		this.pluginInstance = pluginInstance;
		this.pluginFactory = null;
	}

	public void startFailed(){
		this.pluginIsStarted = false;
		this.pluginFactory = null;
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
