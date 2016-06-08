package de.akubix.keyminder.shell.commands;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.CommandException;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.Usage;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Operands(cnt = 1)
@Option(name = "-a")
@Description("Displays a short description of a command.")
@Usage( "${command.name <command> [-a]\n\n" +
		"The '-a' option displays the supported input- and output object types (if any).")
public final class Manual extends AbstractShellCommand {
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		try {
			String cmdName = in.getParameters().get("$0")[0];
			String[] data = instance.getShell().getManual(cmdName);
			for(int i = 0; i < 2; i++){
				if(data[i] != null){
					printHeading(out, (i == 0 ? "Description:" : "\nUsage:"));
					printContent(out, data[i], cmdName);
				}
			}

			if(in.getParameters().containsKey("-a")){
				try{
					PipeInfo pi = instance.getShell().getPipeInformation(data[0]);
					if(pi != null){
						printHeading(out, "\nSupported input types:");
						printContent(out, pi.in(), cmdName);
						printHeading(out, "Output type:");
						printContent(out, pi.out(), cmdName);
					}
				} catch (CommandException e) {
					out.setColor(AnsiColor.BLUE);
					out.println("\n" + e.getMessage());
				}
			}

			return CommandOutput.success();
		} catch (CommandException e) {
			out.setColor(AnsiColor.RED);
			out.println(e.getMessage());
			return CommandOutput.error();
		}
	}

	private void printHeading(ShellOutputWriter out, String txt){
		out.setColor(AnsiColor.CYAN);
		out.print(txt);
		out.setColor(AnsiColor.RESET);
		out.print("\n    ");
	}

	private void printContent(ShellOutputWriter out, String txt, String cmdName){
		if(txt == null || txt.equals("")){
			out.println("-");
		}
		else{
			out.println(txt.replace("${command.name}", cmdName).replace("\n", "\n    "));
		}
	}
}
