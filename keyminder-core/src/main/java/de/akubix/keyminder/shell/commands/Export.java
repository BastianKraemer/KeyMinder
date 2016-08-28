/*	KeyMinder
	Copyright (C) 2016 Bastian Kraemer

	AddNode.java

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.akubix.keyminder.shell.commands;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Usage;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("export")
@Operands(cnt = 1)
@Option(name ="-d", alias={"--delete", "-rm", "--rm", "--del", "--unset"})
@Option(name ="-i", alias={"--stdin", "--in"})
@Description("Sets a new runtime variable in KeyMinder. A runtime variable only exists until KeyMinder is closed. ")
@Usage( "${command.name} [-d | -i] <value>\n\n" +
		"Examples:\n"+
		"  Set a variable:    export someVar=Hello world\n" +
		"                     echo \"any value\" | export -i someVar\n" +
		"  Remove a variable: export -d someVar")
@PipeInfo(in = "String")
public final class Export extends AbstractShellCommand {
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {

		if(!in.getParameters().containsKey("-d")){
			// Add a runtime variable

			if(in.getParameters().containsKey("-i")){
				if(in.getInputData() == null){
					out.setColor(AnsiColor.RED);
					out.println("Error: No input data available.");
					return CommandOutput.error();
				}

				instance.getShell().setRuntimeVariable(in.getParameters().get("$0")[0], in.getInputData().toString());
			}
			else{
				String[] splitStr = in.getParameters().get("$0")[0].split("=", 2);
				if(splitStr.length == 2){
					instance.getShell().setRuntimeVariable(splitStr[0], splitStr[1]);
				}
				else{
					out.setColor(AnsiColor.YELLOW);
					out.printf("No value for runtime variable '%s'. Usage: '%s=<any value>'\n", in.getParameters().get("$0")[0], in.getParameters().get("$0")[0]);
					return CommandOutput.error();
				}
			}
		}
		else{
			// Remove a runtime variable
			instance.getShell().removeRuntimeVariable(in.getParameters().get("$0")[0]);
		}

		return CommandOutput.success();
	}
}
