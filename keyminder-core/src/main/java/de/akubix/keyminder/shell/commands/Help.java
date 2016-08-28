package de.akubix.keyminder.shell.commands;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.lib.Tools;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.NoArgs;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("help")
@NoArgs
@Description("Displays a list of all possible commands.")
public final class Help extends AbstractShellCommand {
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		out.println("Available commands:\n");
		Tools.asSortedList(instance.getShell().getCommandSet()).forEach((cmd) -> out.printf("    %s\n", cmd));

		out.println("\nActive alias mappings:\n");
		instance.getShell().getAliasSet().forEach((entry) -> out.printf("    %s -> %s\n", entry.getKey(), entry.getValue()));

		return CommandOutput.success();
	}
}
