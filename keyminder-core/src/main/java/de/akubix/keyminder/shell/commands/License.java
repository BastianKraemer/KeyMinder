package de.akubix.keyminder.shell.commands;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.Usage;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Option(name="-w", paramCnt=0)
@Description("Displays the KeyMinder license.")
@Usage("${command.name} [-w]")
@PipeInfo(out = "String: The license text")
public final class License extends AbstractShellCommand {
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		String filename = in.getParameters().containsKey("-w") ? "/LICENSE" : "/GPLv3";
		try{
			java.util.Scanner s = new java.util.Scanner(ApplicationInstance.class.getResourceAsStream(filename), "UTF-8");
			String license = s.useDelimiter("\\A").next();
			s.close();
			out.println(license);
			return CommandOutput.success(license);
		}
		catch(Exception ex){
			String errMsg = String.format("ERROR, cannot find resource '%s'", filename);
			out.println(errMsg);
			return CommandOutput.error(errMsg);
		}
	}
}
