package de.akubix.keyminder.shell.commands;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.CommandException;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("man")
@Operands(cnt = 1, description = "COMMAND_NAME")
@Description("Displays a short description of a shell command.")
public final class Manual extends AbstractShellCommand {
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		try {
			out.println(instance.getShell().getManual(in.getParameters().get("$0")[0]));
			return CommandOutput.success();

		} catch (CommandException e) {
			out.setColor(AnsiColor.RED);
			out.println(e.getMessage());
			return CommandOutput.error();
		}
	}
}
