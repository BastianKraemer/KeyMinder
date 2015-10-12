package de.akubix.keyminder.core.interfaces;

public interface Command {
	public String runCommand(CommandOutputProvider out, de.akubix.keyminder.core.ApplicationInstance instance, String[] args);
}
