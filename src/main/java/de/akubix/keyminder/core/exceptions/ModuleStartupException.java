package de.akubix.keyminder.core.exceptions;

public class ModuleStartupException extends Exception {

	private static final long serialVersionUID = 6373043483054241999L;
	private ModuleErrorLevel errorLevel = ModuleErrorLevel.Default;
	public ModuleStartupException(String informations)
	{
		super(informations);
	}

	public ModuleStartupException(String informations, ModuleErrorLevel errl)
	{
		super(informations);
	}

	public ModuleErrorLevel getErrorLevel()
	{
		return errorLevel;
	}

	public static enum ModuleErrorLevel
	{
		Critical, Default, OSNotSupported, FxUserInterfaceNotAvailable
	}
}
