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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.Usage;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;
import de.akubix.keyminder.util.search.NodeFinder;
import de.akubix.keyminder.util.search.NodeMatchResult;
import de.akubix.keyminder.util.search.NodeWalker;
import de.akubix.keyminder.util.search.matcher.NodeMatcher;
import de.akubix.keyminder.util.search.matcher.TextMatcher;
import de.akubix.keyminder.util.search.matcher.TextMatcher.NodeMatcherOption;
import de.akubix.keyminder.util.search.matcher.TimeMatcher;

@Command("find")
@Operands(cnt = 2, optionalNodeArg = true, nodeArgAt = 0)
@Option(name = "--modified", paramCnt = 2, alias = "-m")
@Option(name = "--created", paramCnt = 2, alias = "-c")
@Option(name = "--next", paramCnt = 0, alias = "-n")
@Option(name = "--regex", paramCnt = 0, alias = "-r")
@Option(name = "--case-sensitive", paramCnt = 0, alias = "-s")
@Option(name = "--text-only", paramCnt = 0, alias = "-t")
@Option(name = "--attributes-only", paramCnt = 0, alias = "-a")
@Option(name = "--attribute-filter", paramCnt = 1, alias = "-f")
@Description("Finds a selects the next node that matches the search pattern")
@Usage(	"${command.name} [search pattern] [options]\n\n" +
		"Available options:\n" +
		"  --modified, -m         <before|at|after> <date>\n" +
		"  --create, -c           <before|at|after> <date>\n\n" +
		"  --next, -n             Selects the next matching node\n" +
		"  --regex, -r            Allow usage of regular expressions\n" +
		"  --case-sensitive, -s   Make the search case sensitive (this is automatically active when using '--regex')\n" +
		"  --text-only, -t        Ignores the node attributes\n" +
		"  --attributes-only, -a  Ignores the node text" +
		"  --attribute-filter, -f <regular expression> Ignores attribute that does not match the regex pattern")
@PipeInfo(out = "List of 'NodeMatchResult' or a single 'NodeMatchResult' object when using '--next'")
public final class FindCmd extends AbstractShellCommand {

	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in){
		try{
			Map<String, String[]> parameters = in.getParameters();
			ArrayList<NodeMatcher> conditions = new ArrayList<>();

			boolean simpleSearch = !parameters.containsKey("--regex");

			TextMatcher.NodeMatcherOption option = NodeMatcherOption.ALL;

			if(parameters.containsKey("--text-only") && parameters.containsKey("--attributes-only")){
				out.setColor(AnsiColor.RED);
				out.println("You cannot use the parameters '--text-only' and '--attributes-only' at the same time.");
				return CommandOutput.error();
			}
			else{
				if(parameters.containsKey("--text-only")){
					option = NodeMatcherOption.TEXT_ONLY;
				}
				else if(parameters.containsKey("--attributes-only")){
					option = NodeMatcherOption.ATTRIBUTES_ONLY;
				}
			}

			final boolean ignoreCase = simpleSearch && !parameters.containsKey("--case-sensitive");

			if(parameters.containsKey("--attribute-filter")){
				conditions.add(new TextMatcher(parameters.get("$1")[0], simpleSearch, option, Pattern.compile(parameters.get("--attribute-filter")[0]), ignoreCase));
			}
			else{
				conditions.add(new TextMatcher(parameters.get("$1")[0], simpleSearch, option, ignoreCase));
			}

			if(parameters.containsKey("--modified")){
				conditions.add(
					new TimeMatcher(
						"modified",
						TimeMatcher.getCompareTypeFromString(parameters.get("--modified")[0]),
						parameters.get("--modified")[1]));
			}

			if(parameters.containsKey("--created")){
				conditions.add(
					new TimeMatcher(
						"created",
						TimeMatcher.getCompareTypeFromString(parameters.get("--created")[0]),
						parameters.get("--created")[1]));
			}

			if(parameters.containsKey("--next")){

				// Search using the 'NodeWalker'

				NodeWalker.SearchResult result = NodeWalker.find(instance.getTree(), conditions);

				if(result.getState() == NodeWalker.SearchState.FOUND){
					out.setColor(AnsiColor.CYAN);
					out.println(instance.getTree().getNodePath(result.getMatchResult().getNode(), "/"));
					out.setColor(AnsiColor.RESET);
					return CommandOutput.success(result.getMatchResult());
				}
				else{
					out.println("No matching node found.");
					return CommandOutput.success();
				}
			}
			else{

				// Search using the 'NodeFinder'

				List<NodeMatchResult> resultList = NodeFinder.findNodes(in.getTreeNode(), conditions);
				if(resultList.size() == 0){
					out.println("No matching node found.");
				}
				else if(!in.outputIsPiped()){
					out.setColor(AnsiColor.CYAN);
					resultList.forEach((result) -> out.println(instance.getTree().getNodePath(result.getNode(), "/")));
					out.setColor(AnsiColor.RESET);
				}

				return CommandOutput.success(resultList);
			}
		}
		catch(IllegalArgumentException | ParseException  e){
			out.setColor(AnsiColor.RED);
			out.println(e.getMessage());
			return CommandOutput.error();
		}
	}

}
