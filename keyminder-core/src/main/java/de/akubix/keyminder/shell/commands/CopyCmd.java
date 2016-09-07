/*	KeyMinder
	Copyright (C) 2016 Bastian Kraemer

	CopyCmd.java

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
import de.akubix.keyminder.core.exceptions.InvalidValueException;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.Alias;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Example;
import de.akubix.keyminder.shell.annotations.Note;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.RequireOpenedFile;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("cp")
@RequireOpenedFile
@Description("Copies or moves a node to anther location")
@Operands(cnt = 2,                               description = "SOURCE DESTINATION")
@Option(name = CopyCmd.OPTION_MOVE,           alias = "-m", description = "Perform a move action instead of copy")
@Option(name = CopyCmd.OPTION_NO_CHILD_NODES, alias = "-n", description = "Do not include child-nodes")
@Example({"cp /path/to/any/node /path/to/any/other/node", "[...] | cp " + AbstractShellCommand.REFERENCE_TO_STDIN_KEYWORD + " /        # Copies the node from the input data to '/'"})
@Note("You can use '" + AbstractShellCommand.REFERENCE_TO_STDIN_KEYWORD + "' to reference a piped tree node.")
@PipeInfo(in = "TreeNode or String", out = "TreeNode")
@Alias("mv = cp --move")
public final class CopyCmd extends AbstractShellCommand {

	static final String OPTION_MOVE = "--move";
	static final String OPTION_NO_CHILD_NODES = "--no-child-node";

	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		String src = in.getParameters().get("$0")[0];
		String dest = in.getParameters().get("$1")[0];

		if(src.equals(AbstractShellCommand.REFERENCE_TO_STDIN_KEYWORD) && dest.equals(AbstractShellCommand.REFERENCE_TO_STDIN_KEYWORD)){
			out.setColor(AnsiColor.RED);
			out.printf("You cannot use '%s' as source and destination node.\n", AbstractShellCommand.REFERENCE_TO_STDIN_KEYWORD);
			return CommandOutput.error();
		}

		try {
			TreeNode source = super.getNodeFromPathOrStdIn(instance, in, src);
			TreeNode destination = super.getNodeFromPathOrStdIn(instance, in, dest);

			boolean moveNode = in.getParameters().containsKey(OPTION_MOVE);
			nodeCopy(source, destination, moveNode,	!in.getParameters().containsKey(OPTION_NO_CHILD_NODES));

			if(!in.outputIsPiped()){
				out.printf("Node(s) successfully %s.\n", moveNode ? "moved" : "copied");
			}
			return CommandOutput.success(destination);

		} catch (InvalidValueException e) {
			out.setColor(AnsiColor.YELLOW);
			out.println(e.getMessage());
			return CommandOutput.error();
		}
	}

	private static void nodeCopy(TreeNode src, TreeNode dest, boolean move, boolean includeChildNodes) {
		//src.getTree() and dest.getTree() will point to the same reference
		if(move){src.getTree().beginUpdate();}
		TreeNode clone = src.getTree().cloneTreeNode(src, includeChildNodes);
		dest.getTree().addNode(clone, dest);
		if(move){
			src.getTree().removeNode(src);
			dest.getTree().endUpdate();
		}
	}
}
