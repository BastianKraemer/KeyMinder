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
import de.akubix.keyminder.shell.annotations.Example;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.PipeInfo;
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
@Description("Finds all nodes that matches the search pattern")
@Operands(cnt = 2, optionalNodeArg = true, nodeArgAt = 0, description = "{tree node} [search pattern]")
@Option(name = FindCmd.OPTION_MODIFIED,       paramCnt = 2, alias = "-m", description = "[before|at|after] [date]")
@Option(name = FindCmd.OPTION_CREATED,        paramCnt = 2, alias = "-c", description = "[before|at|after] [date]")
@Option(name = FindCmd.OPTION_NEXT,           paramCnt = 0, alias = "-n", description = "Selects the next matching node")
@Option(name = FindCmd.OPTION_REGEX,          paramCnt = 0, alias = "-r", description = "Allow usage of regular expressions")
@Option(name = FindCmd.OPTION_CASE_SENSITIVE, paramCnt = 0, alias = "-s", description = "Make the search case sensitive (this is automatically active when using '--regex')")
@Option(name = FindCmd.OPTION_TEXT_ONLY,      paramCnt = 0, alias = "-t", description = "Ignores the node attributes")
@Option(name = FindCmd.OPTION_ATTRIBS_ONLY,   paramCnt = 0, alias = "-a", description = "Ignores the node text")
@Option(name = FindCmd.OPTION_FILTER_ATTRIBS, paramCnt = 1, alias = "-f", description = "[regular expression] Ignores attribute that does not match the regex pattern")
@PipeInfo(out = "List of 'NodeMatchResult' or a single 'NodeMatchResult' object when using '--next'")
@Example({"find /some/node \"Hello world\" --next", "find / Hello(.*) --case-sensitive --regex | replace -r \"Hello, $1\""})
public final class FindCmd extends AbstractShellCommand {

	static final String OPTION_MODIFIED = "--modified";
	static final String OPTION_CREATED = "--created";
	static final String OPTION_NEXT = "--next";
	static final String OPTION_REGEX = "--regex";
	static final String OPTION_CASE_SENSITIVE= "--case-sensitive";
	static final String OPTION_TEXT_ONLY= "--text-only";
	static final String OPTION_ATTRIBS_ONLY = "--attributes-only";
	static final String OPTION_FILTER_ATTRIBS = "--attribute-filter";

	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in){
		try{
			Map<String, String[]> parameters = in.getParameters();
			ArrayList<NodeMatcher> conditions = new ArrayList<>();

			boolean simpleSearch = !parameters.containsKey(OPTION_REGEX);

			TextMatcher.NodeMatcherOption option = NodeMatcherOption.ALL;

			if(parameters.containsKey(OPTION_TEXT_ONLY) && parameters.containsKey(OPTION_ATTRIBS_ONLY)){
				out.setColor(AnsiColor.RED);
				out.printf("You cannot use the parameters '%s' and '%s' at the same time.\n", OPTION_TEXT_ONLY, OPTION_ATTRIBS_ONLY);
				return CommandOutput.error();
			}
			else{
				if(parameters.containsKey(OPTION_TEXT_ONLY)){
					option = NodeMatcherOption.TEXT_ONLY;
				}
				else if(parameters.containsKey(OPTION_ATTRIBS_ONLY)){
					option = NodeMatcherOption.ATTRIBUTES_ONLY;
				}
			}

			final boolean ignoreCase = simpleSearch && !parameters.containsKey(OPTION_CASE_SENSITIVE);

			if(parameters.containsKey(OPTION_FILTER_ATTRIBS)){
				conditions.add(new TextMatcher(parameters.get("$1")[0], simpleSearch, option, Pattern.compile(parameters.get(OPTION_FILTER_ATTRIBS)[0]), ignoreCase));
			}
			else{
				conditions.add(new TextMatcher(parameters.get("$1")[0], simpleSearch, option, ignoreCase));
			}

			if(parameters.containsKey(OPTION_MODIFIED)){
				conditions.add(
					new TimeMatcher(
						"modified",
						TimeMatcher.getCompareTypeFromString(parameters.get(OPTION_MODIFIED)[0]),
						parameters.get(OPTION_MODIFIED)[1]));
			}

			if(parameters.containsKey(OPTION_CREATED)){
				conditions.add(
					new TimeMatcher(
						"created",
						TimeMatcher.getCompareTypeFromString(parameters.get(OPTION_CREATED)[0]),
						parameters.get(OPTION_CREATED)[1]));
			}

			if(parameters.containsKey("--next")){

				// Search using the 'NodeWalker'

				NodeWalker.SearchResult result = NodeWalker.find(instance.getTree(), conditions);

				if(result.getState() == NodeWalker.SearchState.FOUND){
					out.setColor(AnsiColor.CYAN);
					out.println(instance.getTree().getNodePath(result.getMatchResult().getNode(), "/"));
					out.setColor(AnsiColor.RESET);

					instance.getTree().setSelectedNode(result.getMatchResult().getNode());

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
