package de.akubix.keyminder.shell.commands;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Example;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.RequireOpenedFile;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("get")
@RequireOpenedFile
@Description("Reads the value of a tree node attribute")
@Operands(cnt = 2, nodeArgAt = 0, optionalNodeArg = true, description = "{NODE_PATH} ATTRIBUTE_NAME")
@Example("get /path/to/any/node username  # Returns the 'username' that is stored in '/path/to/any/node'")
@PipeInfo(in = "TreeNode", out = "String")
public final class GetAttribute extends AbstractShellCommand {
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

		String attribName = in.getParameters().get("$1")[0];

		if(node.hasAttribute(attribName)) {
			String value = node.getAttribute(attribName);
			if(!in.outputIsPiped()){
				out.println(value);
			}
			return CommandOutput.success(value);
		}
		else {
			if(!in.outputIsPiped()){
				out.printf("Attribute '%s' does not extist.\n", in.getParameters().get("$1")[0]);
			}
			return CommandOutput.error();
		}
	}
}
