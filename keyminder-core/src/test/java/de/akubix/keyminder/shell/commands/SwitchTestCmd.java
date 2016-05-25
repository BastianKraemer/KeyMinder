package de.akubix.keyminder.shell.commands;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.interfaces.CommandOutputProvider;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.RequiredOptions;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;

@Option(name="-required", paramCnt=1)
@Option(name="-optional1", paramCnt=0)
@Option(name="-optional2", paramCnt=1)
@Option(name="-optional3", paramCnt=1, defaultValue={"opt"})
@Option(name="-twoparams", paramCnt=2, defaultValue={"1", "2"})
@RequiredOptions(names={"-required"})
public class SwitchTestCmd extends AbstractShellCommand{

	@Override
	public CommandOutput exec(CommandOutputProvider out, ApplicationInstance instance, CommandInput in){
		StringBuilder sb = new StringBuilder();

		sb.append(in.getParameters().get("-required")[0]).append(";");
		sb.append(in.getParameters().containsKey("-optional1") ? "Y" : "N").append(";");

		if(in.getParameters().containsKey("-optional2")){
			sb.append(in.getParameters().get("-optional2")[0]);
		}
		sb.append(";");

		sb.append(in.getParameters().get("-optional3")[0]).append(";");
		sb.append(in.getParameters().get("-twoparams")[0]).append(",")
		  .append(in.getParameters().get("-twoparams")[1]);

		return CommandOutput.success(sb.toString());
	}
}
