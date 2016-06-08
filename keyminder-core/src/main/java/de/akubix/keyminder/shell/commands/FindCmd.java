/*	KeyMinder
	Copyright (C) 2016 Bastian Kraemer

	FindCmd.java

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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Map;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.lib.NodeTimeCondition;
import de.akubix.keyminder.lib.TreeSearch;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.Usage;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Operands(cnt = 1)
@Option(name = "--modified", paramCnt = 2, alias = "-m")
@Option(name = "--created", paramCnt = 2, alias = "-c")
@Description("Finds a selects the next node that matches the search pattern")
@Usage(	"${command.name} [search pattern] [options]\n\n" +
		"Available options:\n" +
		"  --modified, -m <before|at|after> <date>\n" +
		"  --create, -c <before|at|after> <date>")
public final class FindCmd extends AbstractShellCommand {
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in){
		try{
			Map<String, String[]> parameters = in.getParameters();
			ArrayList<NodeTimeCondition> conditions = new ArrayList<>(2);

			if(parameters.containsKey("--modified")){
				conditions.add(
					new NodeTimeCondition(
						"modified",
						NodeTimeCondition.getCompareTypeFromString(parameters.get("--modified")[0]),
						parameters.get("--modified")[1]));
			}

			if(parameters.containsKey("--created")){
				conditions.add(
					new NodeTimeCondition(
						"created",
						NodeTimeCondition.getCompareTypeFromString(parameters.get("--created")[0]),
						parameters.get("--created")[1]));
			}

			if(TreeSearch.find(parameters.get("$0")[0], instance.getTree(), true, conditions.toArray(new NodeTimeCondition[conditions.size()])) == TreeSearch.SearchResult.FOUND){
				out.setColor(AnsiColor.CYAN);
				out.print("Found matching node: ");
				out.setColor(AnsiColor.RESET);
				out.println(instance.getTree().getSelectedNode().getText());
				return CommandOutput.success();
			}
			else{
				out.println("No matching node found.");
				return CommandOutput.error();
			}
		}
		catch(IllegalArgumentException | ParseException e){
			out.setColor(AnsiColor.RED);
			out.println(e.getMessage());
			return CommandOutput.error();
		}
	}

}
