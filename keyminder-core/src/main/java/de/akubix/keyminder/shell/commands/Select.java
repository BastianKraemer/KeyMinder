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
@Operands(cnt = 1, nodeArgAt = 0)
@Option(name = "--index", paramCnt = 1, alias={"-i"})
@Option(name = "--get", alias={"-g", "-o", "--out"})
@Description("Selects a tree node by its path or index.")
@Usage("'${command.name} [nodename]' to select a childnode.\n"
	 + "'${command.name} ..' to select the parent node\n"
	 + "'${command.name} /' to select the rootnode.\n"
	 + "'${command.name} --index <num>' to select a child node by its index (you can also use '-i')\n"
	 + "'${command.name} --get' to pipe this not to the output without selecting it\n\n"
	 + "You can also use absolute paths: '${command.name} /path/to/node'")
@PipeInfo(in = "TreeNode", out = "TreeNode")
public final class Select extends AbstractShellCommand {
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		TreeNode selectedNode = in.getTreeNode();
		if(in.getInputData() != null){
			if(in.getInputData() instanceof TreeNode){
				super.checkInputObjectArgumentConflict(out, in, "$0", "--index");
				selectedNode = (TreeNode) in.getInputData();
			}
			else{
				super.printUnusableInputWarning(out);
				return CommandOutput.error();
			}
		}
		else{
			if(in.getParameters().containsKey("--index") && in.getParameters().containsKey("$0")){
				out.setColor(AnsiColor.YELLOW);
				out.println("You cannot use a node path and the '--index' option at the same time.");
				return CommandOutput.error();
			}

			if(in.getParameters().containsKey("--index")){
				try{
					int index = Integer.parseUnsignedInt(in.getParameters().get("--index")[0]);
					selectedNode = selectedNode.getChildNodeByIndex(index);
				}
				catch(NumberFormatException numex){
					out.setColor(AnsiColor.RED);
					out.printf("Cannot parse '%s' as integer.\n", in.getParameters().get("--index")[0]);
					return CommandOutput.error();
				}
				catch(IndexOutOfBoundsException ioobex){
					out.setColor(AnsiColor.RED);
					out.printf("Unable to find child node with index '%s'.\n", in.getParameters().get("--index")[0]);
					return CommandOutput.error();
				}
			}
		}

		if(!in.getParameters().containsKey("--get")){
			instance.getTree().setSelectedNode(selectedNode);
		}

		if(!in.outputIsPiped()){
			out.println(instance.getTree().getNodePath(selectedNode, "/"));
		}

		return CommandOutput.success(selectedNode);
	}
}
