package de.akubix.keyminder.shell.commands;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.RequireOpenedFile;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Operands(cnt=3, nodeArgAt=1)
@RequireOpenedFile
public class TestCmd extends AbstractShellCommand{

	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		String ret = in.getParameters().get("$0")[0] + "," +
					 (in.getTreeNode() != null ? (instance.getTree().getNodePath(in.getTreeNode(), "/") + ",") : "") +
					 in.getParameters().get("$2")[0];
		return CommandOutput.success(ret);
	}

}
