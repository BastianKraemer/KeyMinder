package de.akubix.keyminder.shell.commands;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.annotations.Alias;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.RequireOpenedFile;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;
import de.akubix.keyminder.util.Utilities;

@Command("vi")
@RequireOpenedFile
@Description("Displays all attributes of a tree node.")
@Operands(cnt = 1, nodeArgAt = 0, optionalNodeArg = true, description = "{NODE_PATH}")
@PipeInfo(in = "TreeNode", out = "TreeNode")
@Alias("view = vi")
public final class ViewNode extends AbstractShellCommand {
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
			}
		}

		if(node == null){
			node = in.getTreeNode();
		}

		out.println("Attributes of '" + node.getText() + "'");

		int i = 0;
		out.println("#" + i++ +"\tName:\tid\n\tValue:\t" + node.getId() + "\n");

		for(String key: Utilities.asSortedList(node.listAttributes())){
			out.println("#" + i++ +"\tName:\t" + key + "\n\tValue:\t" + node.getAttribute(key) + "\n");
		}

		if(!node.getColor().equals("")){
			out.println("#" + i++ +"\tName:\tcolor\n\tValue:\t" + node.getColor() + "\n");
		}

		return CommandOutput.success(node);
	}
}
