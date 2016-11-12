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
import de.akubix.keyminder.core.exceptions.InvalidValueException;
import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.AllowCallWithoutArguments;
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

@Command("qlnk")
@AllowCallWithoutArguments
@Description("A Quicklink is a reference to a any tree node, so you can jump to frequently used nodes with just one command.")
@Operands(cnt = 1, description = "QUICKLINK_NAME")
@Option(name = Quicklinks.OPTION_ADD, alias = "-a", paramCnt = 1, description = "NODE_PATH  Adds a new Quicklink")
@Option(name = Quicklinks.OPTION_REMOVE, alias = {"-r", "--rm"},  description = "Removes a Quicklink")
@PipeInfo(in = "TreeNode", out = "TreeNode")
@Example({	"# Jump to a Quicklink:\n  qlnk QUICKLINK_NAME",
			"# Define a new Quicklink:\n  qlnk QUICKLINK_NAME --add /path/to/node *",
			"# Remove a Quicklink:\n  qlnk QUICKLINK_NAME --remove"})
@Note("* You can use '@-' to reference a tree node in the input data.")
public class Quicklinks extends AbstractShellCommand {

	static final String OPTION_ADD = "--add";
	static final String OPTION_REMOVE = "--remove";

	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in){
		if(in.getParameters().containsKey(OPTION_ADD) && in.getParameters().containsKey(OPTION_REMOVE)){
			out.setColor(AnsiColor.RED);
			out.printf("You cannot use '%s' and '%s' at the same time.\n", OPTION_ADD, OPTION_REMOVE);
			return CommandOutput.error();
		}

		if(in.getParameters().containsKey("$0")){
			if(in.getParameters().containsKey(OPTION_ADD)){

				try {
					TreeNode node = super.getNodeFromPathOrStdIn(instance, in, in.getParameters().get(OPTION_ADD)[0]);
					instance.addQuicklink(in.getParameters().get("$0")[0], node);
					return CommandOutput.success(node);

				} catch (InvalidValueException e) {
					out.setColor(AnsiColor.RED);
					out.println(e.getMessage());
					return CommandOutput.error();
				}
			}

			if(in.getParameters().containsKey(OPTION_REMOVE)){
				instance.removeQuicklink(in.getParameters().get("$0")[0]);
				return CommandOutput.success();
			}

			TreeNode quicklinkNode = instance.getQuicklinkNode(in.getParameters().get("$0")[0]);

			if(quicklinkNode != null){
				if(in.outputIsPiped()){
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
					out.printf("%16s -> %s\n", str, node.getNodePath());
				}
			});
			return CommandOutput.success();
		}
	}
}
