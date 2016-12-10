package de.akubix.keyminder.core.exceptions;

public class PluginStartupException extends Exception {

	private static final long serialVersionUID = 6373043483054241999L;
	private PluginErrorLevel errorLevel = PluginErrorLevel.Default;
	public PluginStartupException(String informations){
		super(informations);
	}

	public PluginStartupException(String informations, PluginErrorLevel errl){
		super(informations);
	}

	public PluginErrorLevel getErrorLevel(){
		return errorLevel;
	}

	public static enum PluginErrorLevel {
		Critical, Default, OSNotSupported
	}
}
