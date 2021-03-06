/*	KeyMinder
	Copyright (C) 2016 Bastian Kraemer

	ListNodes.java

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
import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.RequireOpenedFile;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;
import de.akubix.keyminder.util.Utilities;

@Command("ls")
@RequireOpenedFile
@Description("Lists the child nodes of a tree node.")
@Operands(cnt = 1, nodeArgAt = 0, description = "{NODE_PATH}")
@PipeInfo(in = "TreeNode")
public final class ListNodes extends AbstractShellCommand {
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		TreeNode node = null;
		if(in.getInputData() != null){
			if(in.getInputData() instanceof TreeNode){
				super.checkInputObjectArgumentConflict(out, in, "$0");
				node = (TreeNode) in.getInputData();
			}
			else{
				super.printUnusableInputWarning(out);
				return CommandOutput.error();
			}
		}

		if(node == null){
			node = in.getTreeNode();
		}

		out.println("Childnodes of \"" + node.getText() + "\":\n\n" +
					"Index\tCreation date\tModification date\tNode name\n" +
					"-----\t-------------\t-----------------\t---------");

		int i = 0;
		for(TreeNode n: node.getChildNodes()){
			String creationDate = Utilities.getTimeFromEpochMilli(n.getAttribute(de.akubix.keyminder.core.ApplicationInstance.NODE_ATTRIBUTE_CREATION_DATE), true, "-\t");
			String modificationDate = Utilities.getTimeFromEpochMilli(n.getAttribute(de.akubix.keyminder.core.ApplicationInstance.NODE_ATTRIBUTE_MODIFICATION_DATE), true, "-\t");

			out.println(i++ + "\t" + creationDate + "\t" + modificationDate + "\t\t" + n.getText());
		}
		return CommandOutput.success();
	}
}
