package de.akubix.keyminder.shell.commands;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.shell.AbstractShellCommand;
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
@Operands(cnt = 1, nodeArgAt = 0, optionalNodeArg = true)
@Option(name = "--recursive", alias = "-r")
@Description("Sorts the child nodes of any tree node alphabetically")
@Usage("${command.name} </path/to/tree/node> <--recursive, -r>")
@PipeInfo(in = "TreeNode", out = "TreeNode")
public final class SortCmd extends AbstractShellCommand {
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		TreeNode parentNode;
		if(in.getParameters().containsKey("$0") || in.getInputData() == null){
			parentNode = in.getTreeNode();
		}
		else{
			if(in.getInputData() instanceof TreeNode){
				parentNode = (TreeNode) in.getInputData();
			}
			else{
				super.printUnusableInputWarning(out);
				return CommandOutput.error();
			}
		}

		boolean recursive = in.getParameters().containsKey("--recursive");

		instance.getTree().sortChildNodes(parentNode, recursive);
		return CommandOutput.success(parentNode);
	}
}
