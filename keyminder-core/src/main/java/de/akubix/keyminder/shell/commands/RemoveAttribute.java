package de.akubix.keyminder.shell.commands;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.RequireOpenedFile;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("rmattrib")
@RequireOpenedFile
@Description("Removes an attribute from a tree node")
@Operands(cnt = 2, nodeArgAt = 0, optionalNodeArg = true, description = "{NODE_PATH} ATTRIBUTE_NAME")
@PipeInfo(in = "TreeNode", out = "TreeNode")
public final class RemoveAttribute extends AbstractShellCommand {
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

		if(node.hasAttribute(in.getParameters().get("$1")[0])) {
			node.removeAttribute(in.getParameters().get("$1")[0]);
			return CommandOutput.success(node);
		}
		else {
			out.setColor(AnsiColor.YELLOW);
			out.printf("Attribute '%s' does not extist.\n", in.getParameters().get("$1")[0]);
			return CommandOutput.error(node);
		}
	}
}
