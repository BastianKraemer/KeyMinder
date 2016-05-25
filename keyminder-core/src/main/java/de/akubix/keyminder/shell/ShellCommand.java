package de.akubix.keyminder.shell;

import java.util.List;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.interfaces.CommandOutputProvider;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;

/**
 * This interface has to be implemented by every KeyMinder Shell Command.
 * For own command implementations it is recommended to extend the {@link AbstractShellCommand} class.
 */
public interface ShellCommand {
	public abstract CommandInput parseArguments(ApplicationInstance instance, List<String> args) throws CommandException;
	public abstract CommandOutput exec(CommandOutputProvider out, ApplicationInstance instance, CommandInput in);
}
