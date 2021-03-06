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

@Command("set")
@RequireOpenedFile
@Description("Sets an attribute of a tree node")
@Operands(cnt = 3, nodeArgAt = 0, optionalNodeArg = true, description = "{NODE_PATH} ATTRIBUTE_NAME ATTRIBUTE_VALUE")
@PipeInfo(in = "TreeNode", out = "TreeNode")
public final class SetAttribute extends AbstractShellCommand {

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

		if(in.getParameters().get("$1")[0].toLowerCase().equals("text")){
			node.setText(in.getParameters().get("$2")[0]);
		}
		else {
			node.setAttribute(in.getParameters().get("$1")[0], in.getParameters().get("$2")[0]);
		}

		return CommandOutput.success(node);
	}
}
