package de.akubix.keyminder.shell.commands;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.interfaces.CommandOutputProvider;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.annotations.NoArgs;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;

@NoArgs
public class NoArgsCmd extends AbstractShellCommand{

	@Override
	public CommandOutput exec(CommandOutputProvider out, ApplicationInstance instance, CommandInput in) {
		return CommandOutput.success("OK");
	}

}
