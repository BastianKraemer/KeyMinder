package de.akubix.keyminder.shell.commands;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.RequireOpenedFile;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("sort")
@RequireOpenedFile
@Description("Sorts the child nodes of any tree node alphabetically")
@Operands(cnt = 1, nodeArgAt = 0, optionalNodeArg = true, description = "{NODE_PATH}")
@Option(name = SortCmd.OPTION_RECURSIVE, alias = "-r", 	  description = "Sorts the child nodes as well")
@PipeInfo(in = "TreeNode", out = "TreeNode")
public final class SortCmd extends AbstractShellCommand {

	static final String OPTION_RECURSIVE = "--recursive";

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

		boolean recursive = in.getParameters().containsKey(OPTION_RECURSIVE);

		parentNode.sortChildNodes(recursive);
		return CommandOutput.success(parentNode);
	}
}
