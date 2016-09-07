package de.akubix.keyminder.shell.commands;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.exceptions.InvalidValueException;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.AllowCallWithoutArguments;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Example;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.RequireOpenedFile;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("rm")
@RequireOpenedFile
@AllowCallWithoutArguments
@Description("Removes a tree node.")
@Operands(cnt = 1, nodeArgAt = 0, description = "{NODE_PATH}")
@Example({"rm [/path/to/any/node]", "[...] | rm"})
@PipeInfo(in = "TreeNode")
public final class RemoveNode extends AbstractShellCommand {
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		TreeNode node = in.getTreeNode();

		if(in.getParameters().containsKey("$0")){
			if(in.getInputData() != null){
				try {
					node = super.getNodeFromPathOrStdIn(instance, in, in.getParameters().get("$0")[0]);
				} catch (InvalidValueException e) {
					out.setColor(AnsiColor.YELLOW);
					out.println(e.getMessage());
					return CommandOutput.error();
				}
			}
		}
		else if(in.getInputData() != null){
			if(in.getInputData() instanceof TreeNode){
				node = (TreeNode) in.getInputData();
			}
			else{
				super.printUnusableInputWarning(out);
				return CommandOutput.error();
			}
		}
		else {
			out.println("Unkown node path. Usage: rm NODE_PATH");
			return CommandOutput.error();
		}

		instance.getTree().removeNode(node);
		return CommandOutput.success();
	}
}
