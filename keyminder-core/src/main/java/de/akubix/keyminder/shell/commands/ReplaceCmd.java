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

import java.util.ArrayList;
import java.util.List;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Example;
import de.akubix.keyminder.shell.annotations.Note;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;
import de.akubix.keyminder.util.search.MatchReplace;
import de.akubix.keyminder.util.search.NodeMatchResult;

@Command("replace")
@Description("Replaces the matching values from a search result with a custom text")
@Operands(cnt = 1, description = "REPLACMENT_TEXT")
@Option(name = ReplaceCmd.OPTION_REGEX, alias = {"-r", "--regex-replace"}, description = "Enable using a regular expression including match groups ('$0', '$1', etc.)")
@Example("find / 'hello(.+)' --regex | replace 'hello, $1' --regex")
@Note("You have to pipe the result of a 'find' command to replace any values.")
@PipeInfo(in = "List of 'NodeMatchResult' or a single 'NodeMatchResult' object")
public final class ReplaceCmd extends AbstractShellCommand {

	static final String OPTION_REGEX = "--regex";

	@SuppressWarnings("unchecked")
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in){
		Object inputData = in.getInputData();

		List<NodeMatchResult> results;

		if(inputData instanceof NodeMatchResult){
			results = new ArrayList<>(1);
			results.add((NodeMatchResult) inputData);
		}
		else{
			try {
				results = (List<NodeMatchResult>) inputData;
			}
			catch(ClassCastException e){
				out.setColor(AnsiColor.RED);
				out.println("Input data is not of type 'NodeMatchResult' nor a list of 'NodeMatchResult'");
				return CommandOutput.error();
			}
		}

		int replaceCount = 0;
		try{
			for(NodeMatchResult match: results){
				if(MatchReplace.replaceContent(match, in.getParameters().get("$0")[0], in.getParameters().containsKey(OPTION_REGEX))){
					replaceCount++;
				}
			}

		}
		catch(IllegalArgumentException e){
			out.printf("ERROR: %s\n", e.getMessage());
			return CommandOutput.error();
		}

		out.println("Replaced values: " + replaceCount);
		return CommandOutput.success();
	}
}
