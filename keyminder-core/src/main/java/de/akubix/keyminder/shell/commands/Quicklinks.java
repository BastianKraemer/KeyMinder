/*	KeyMinder
	Copyright (C) 2016 Bastian Kraemer

	Quicklinks.java

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
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.AllowCallWithoutArguments;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.Usage;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Operands(cnt = 1)
@Option(name = "--get", alias = "-g")
@Option(name = "--add", alias = "-a", paramCnt = 1)
@Option(name = "--remove", alias = {"-r", "--rm"})
@AllowCallWithoutArguments
@PipeInfo(in = "TreeNode", out = "TreeNode")
@Description("A Quicklink is a reference to a any tree node, so you can jump to frequently used nodes with just one command.")
@Usage( "Jump to a Quicklink:\n" +
		"  ${command.name} [Quicklink name]\n" +
		"Define a new Quicklink:\n" +
		"  ${command.name} [Quicklink name] --add [/path/to/node *]\n" +
		"Remove a Quicklink:\n" +
		"  ${command.name} [Quicklink name] --remove\n\n" +
		"*You can use '%' to reference a tree node in the input data.")
public class Quicklinks extends AbstractShellCommand {

	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in){
		if(in.getParameters().containsKey("--add") && in.getParameters().containsKey("--remove")){
			out.setColor(AnsiColor.RED);
			out.println("You cannot use '--add' and '--remove' at the same time.");
			return CommandOutput.error();
		}

		if(in.getParameters().containsKey("$0")){
			if(in.getParameters().containsKey("--add")){

				String param = in.getParameters().get("--add")[0];

				TreeNode node;
				if(param.equals("%")){
					if(in.getInputData() instanceof TreeNode){
						node = (TreeNode) in.getInputData();
					}
					else{
						out.setColor(AnsiColor.RED);
						out.println("Input object is not a 'TreeNode'.");
						return CommandOutput.error();
					}
				}
				else{
					node = instance.getTree().getNodeByPath(param);
				}

				if(node != null){
					instance.addQuicklink(in.getParameters().get("$0")[0], node);
					return CommandOutput.success(node);
				}
				else{
					out.setColor(AnsiColor.RED);
					out.printf("Node '%s' does not exist.\n", in.getParameters().get("--add")[0]);
					return CommandOutput.error();
				}
			}

			if(in.getParameters().containsKey("--remove")){
				instance.removeQuicklink(in.getParameters().get("$0")[0]);
				return CommandOutput.success();
			}

			TreeNode quicklinkNode = instance.getQuicklinkNode(in.getParameters().get("$0")[0]);

			if(quicklinkNode != null){
				if(!in.getParameters().containsKey("--get")){
					instance.getTree().setSelectedNode(quicklinkNode);
				}

				return CommandOutput.success(quicklinkNode);
			}
			else{
				out.setColor(AnsiColor.YELLOW);
				out.printf("Quicklink '%s' is not defined.\n", in.getParameters().get("$0")[0]);
				return CommandOutput.error();
			}
		}
		else{
			instance.getQuicklinks().stream().sorted().forEach((str) -> {
				TreeNode node = instance.getQuicklinkNode(str);
				if(node != null){
					out.printf("%16s -> %s\n", str, instance.getTree().getNodePath(node, "/"));
				}
			});
			return CommandOutput.success();
		}
	}
}
