/* KeyMinder
 * Copyright (C) 2015-2016 Bastian Kraemer
 *
 * DeadlineCmd.java
 *
 * KeyMinder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * KeyMinder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with KeyMinder.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.akubix.keyminder.plugins.deadline;

import java.text.SimpleDateFormat;
import java.time.ZoneId;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Example;
import de.akubix.keyminder.shell.annotations.Note;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.RequireOpenedFile;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("deadline")
@RequireOpenedFile
@Description("Adds or removes a deadline (e.g an expiration date) to a tree node.")
@Operands(cnt = 2, nodeArgAt = 1, optionalNodeArg = true, description = "[DATE | 'rest' | 'check'] {NODE_PATH}")
@Example({	"# Set a 'deadline':\n  deadline 31.12.2016 /path/to/any/node",
			"# Remove a 'deadline':\n  deadline reset /path/to/any/node",
			"# Check for expired nodes:\n  deadline reset /path/to/any/node"})
@Note("You can use '" + AbstractShellCommand.REFERENCE_TO_STDIN_KEYWORD + "' to take this value from the piped input data.")
@PipeInfo(in = "TreeNode, String, Long", out = "TreeNode")
public class DeadlineCmd extends AbstractShellCommand {
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		Deadline deadline = (Deadline) instance.getPluginLoader().getPluginInfo("Deadline").getInstance();

		TreeNode node;
		if(in.getParameters().containsKey("$1")){
			node = in.getTreeNode();
		}
		else{
			if(in.getInputData() != null){
				if(in.getInputData() instanceof TreeNode){
					node = (TreeNode) in.getInputData();
				}
				else{
					super.printUnusableInputWarning(out);
					return CommandOutput.error();
				}
			}
			else{
				node = in.getTreeNode();
			}
		}

		String value = in.getParameters().get("$0")[0];
		switch(value){
			case "check":
				deadline.checkForExpiredNodes(true);
				break;

			case "reset":
			case "none":
			case "-":
				node.removeAttribute(Deadline.NODE_EXPIRATION_ATTRIBUTE);
				out.println(String.format("Deadline of '%s' removed.", node.getText()));
				break;

			default:
				try{
					long epochMilli = -1;
					if(value.equals(AbstractShellCommand.REFERENCE_TO_STDIN_KEYWORD)){
						if(in.getInputData() != null){
							if(in.getInputData() instanceof String){
								value = (String) in.getInputData();
							}
							else if(in.getInputData() instanceof Long){
								epochMilli = (Long) in.getInputData();
							}
							else{
								super.printUnusableInputWarning(out);
							}
						}
					}

					if(epochMilli == -1){
						SimpleDateFormat sda = new SimpleDateFormat("dd.MM.yyyy");
						epochMilli = sda.parse(value).toInstant().atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
					}

					node.setAttribute(Deadline.NODE_EXPIRATION_ATTRIBUTE, Long.toString(epochMilli));
					if(!in.outputIsPiped()){
						out.setColor(AnsiColor.GREEN);
						out.println("Deadline successfully added.");
					}

					return CommandOutput.success(node);
				}
				catch (Exception e){
					out.setColor(AnsiColor.YELLOW);
					out.printf("Unable to parse date '%s'. Required format: 'dd.MM.yyyy'.\n", value);
					return CommandOutput.error();
				}
		}

		return CommandOutput.success(node);
	}
}
