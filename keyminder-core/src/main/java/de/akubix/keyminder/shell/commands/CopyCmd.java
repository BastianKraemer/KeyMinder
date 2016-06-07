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
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.RequireOpenedFile;
import de.akubix.keyminder.shell.annotations.Usage;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@RequireOpenedFile
@Operands(cnt = 2)
@Option(name = "--move", alias = "-m")
@Option(name = "--no-child-nodes", alias = "-n")
@Description("Copies or moves a node to anther location")
@Usage(	"${command.name} [source] [dest] <options>\n\n" +
		"Options:\n" +
		"  --move, -m            Move a node instead of copying it\n" +
		"  --no-child-nodes, -n  Do not include child-nodes\n\n" +
		"You can use '%' reference a piped tree node.\n\n" +
		"Example: ${command.name} % /new/parent/node --move")
@PipeInfo(in = "TreeNode or String", out = "TreeNode")
public final class CopyCmd extends AbstractShellCommand {

	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		String src = in.getParameters().get("$0")[0];
		String dest = in.getParameters().get("$1")[0];

		if(src.equals("%") && dest.equals("%")){
			out.setColor(AnsiColor.RED);
			out.println("You cannot use '%' as source and destination node.");
			return CommandOutput.error();
		}
		try{
			TreeNode source = getNodeFromArg(src, instance, in);
			TreeNode destination = getNodeFromArg(dest, instance, in);
			boolean moveNode = in.getParameters().containsKey("--move");
			nodeCopy(	source,
						destination,
						moveNode,
						!in.getParameters().containsKey("--no-child-nodes"));

			if(!in.outputIsPiped()){
				out.printf("Node successfully %s.\n", moveNode ? "moved" : "copied");
			}
			return CommandOutput.success(destination);
		}
		catch(IllegalArgumentException e){
			out.setColor(AnsiColor.YELLOW);
			out.println(e.getMessage());
			return CommandOutput.error();
		}
	}

	private static TreeNode getNodeFromArg(String path, ApplicationInstance instance, CommandInput in) throws IllegalArgumentException {
		TreeNode node;
		if(path.equals("%")){
			if(in.getInputData() instanceof TreeNode){
				node = (TreeNode) in.getInputData();
			}
			else if(in.getInputData() instanceof String){
				path = (String) in.getInputData();
				node = instance.getTree().getNodeByPath(path);
			}
			else{
				throw new IllegalArgumentException("Input data object is not a 'TreeNode' nor a 'String'");
			}
		}
		else{
			node = instance.getTree().getNodeByPath(path);
		}

		if(node == null){
			throw new IllegalArgumentException(String.format("Cannot find node '%s'.", path));
		}

		return node;
	}

	private static void nodeCopy(TreeNode src, TreeNode dest, boolean move, boolean includeChildNodes)
	{
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
