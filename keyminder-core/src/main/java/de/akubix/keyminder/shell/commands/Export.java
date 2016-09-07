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
import de.akubix.keyminder.lib.Tools;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Example;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;
import javafx.util.Pair;

@Command("export")
@Operands(cnt = 1)
@Description("Sets a new runtime variable in KeyMinder. A runtime variable only exists until the application is closed.")
@Option(name = Export.OPTION_DELETE, alias={"-d", "-rm", "--rm", "--del", "--unset"}, description = "VAR_NAME  Removes a variable")
@Option(name = Export.OPTION_STDIN, alias={"-i", "--in"},                             description = "VAR_NAME  Uses the input data as value")
@Example({	"# Set a new variable\n  export someVar=Hello world",
			"# Removes the variable 'someVar'\n  export --delete someVar                 ",
			"# Set a new variable using the value from the input data\n  echo \"any value\" | export -i someVar  "})
@PipeInfo(in = "String")
public final class Export extends AbstractShellCommand {

	static final String OPTION_DELETE = "--delete";
	static final String OPTION_STDIN = "--stdin";

	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {

		if(!in.getParameters().containsKey(OPTION_DELETE)){
			// Add a runtime variable

			if(in.getParameters().containsKey(OPTION_STDIN)){
				if(in.getInputData() == null){
					out.setColor(AnsiColor.RED);
					out.println("Error: No input data available.");
					return CommandOutput.error();
				}

				instance.getShell().setRuntimeVariable(in.getParameters().get("$0")[0], in.getInputData().toString());
			}
			else{

				try{
					Pair<String, String> p = Tools.splitKeyAndValue(in.getParameters().get("$0")[0], "[A-Za-z0-9_\\.:-]+", "=", ".+");
					instance.getShell().setRuntimeVariable(p.getKey(), p.getValue().trim());
				}
				catch(IllegalArgumentException e){
					out.setColor(AnsiColor.YELLOW);
					out.printf("Invalid syntax. Usage: '%s=<any value>'\n", in.getParameters().get("$0")[0], in.getParameters().get("$0")[0]);
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
