package de.akubix.keyminder.shell.commands;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.shell.CommandException;
import de.akubix.keyminder.shell.ShellCommand;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("echo")
@Description("Prints out any data.")
@Operands(cnt = 1, description = "OUTPUT_TEXT")
@PipeInfo(in = "* (Any object)", out = "String")
public class Echo implements ShellCommand {
	@Override
	public CommandInput parseArguments(ApplicationInstance instance, List<String> args) throws CommandException {
		Map<String, String[]> map = new HashMap<>();
		if(args.size() > 0){
			map.put("$0", args.toArray(new String[args.size()]));
		}

		return new CommandInput(map, instance.getTree().getSelectedNode());
	}

	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {

		final StringBuilder sb = new StringBuilder();
		boolean prependSpace = false;

		if(in.getParameters().containsKey("$0")){
			for(String s: in.getParameters().get("$0")){
				if(prependSpace){sb.append(" ");}else{prependSpace = true;}
				sb.append(s);
			}
		}

		if(in.getInputData() != null){
			if(prependSpace){sb.append(" ");}else{prependSpace = true;}
			sb.append(in.getInputData().toString());
		}

		if(!in.outputIsPiped() && prependSpace){
			out.println(sb.toString());
		}

		return CommandOutput.success(sb.toString());
	}
}
