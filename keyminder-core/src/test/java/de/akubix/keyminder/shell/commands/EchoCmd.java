package de.akubix.keyminder.shell.commands;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.interfaces.CommandOutputProvider;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;

@Operands(cnt=1)
@Option(name="--newline")
public class EchoCmd extends AbstractShellCommand{
	@Override
	public CommandOutput exec(CommandOutputProvider out, ApplicationInstance instance, CommandInput in) {
		String val = in.getParameters().get("$0")[0];
		if(in.getParameters().containsKey("--newline")){
			return CommandOutput.success(val + "\n");
		}
		else{
			return CommandOutput.success(val);
		}
	}
}
