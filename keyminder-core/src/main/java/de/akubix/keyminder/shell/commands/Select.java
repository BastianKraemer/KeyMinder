package de.akubix.keyminder.shell.commands;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.Alias;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Example;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.RequireOpenedFile;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("select")
@RequireOpenedFile
@Description("Selects a tree node by its path or index")
@Operands(cnt = 1, nodeArgAt = 0, description = "NODE_PATH")
@Option(name = Select.OPTION_INDEX, paramCnt = 1, alias={"-i"}, description = "INDEX  Selects a child node by its index")
@Option(name = Select.OPTION_GET, alias={"-g", "-o", "--out"},  description = "Pipe the node to the next command without selecting it")
@Example({	"# Select any node:\n  select /path/to/any/node  OR  select any/child/node",
			"# Select the parent node:\n  select ..",
			"# Select the rootnode:\n  select /",
			"# Select the first child node by its index:\n  select -i 0"})
@PipeInfo(in = "TreeNode", out = "TreeNode")
@Alias({"cd = select", "getnode = select --get"})
public final class Select extends AbstractShellCommand {

	static final String OPTION_INDEX = "--index";
	static final String OPTION_GET = "--get";

	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		TreeNode selectedNode = in.getTreeNode();
		if(in.getInputData() != null){
			if(in.getInputData() instanceof TreeNode){
				super.checkInputObjectArgumentConflict(out, in, "$0", OPTION_INDEX);
				selectedNode = (TreeNode) in.getInputData();
			}
			else{
				super.printUnusableInputWarning(out);
				return CommandOutput.error();
			}
		}
		else{
			if(in.getParameters().containsKey(OPTION_INDEX) && in.getParameters().containsKey("$0")){
				out.setColor(AnsiColor.YELLOW);
				out.println("You cannot use a node path and the '--index' option at the same time.");
				return CommandOutput.error();
			}

			if(in.getParameters().containsKey(OPTION_INDEX)){
				try{
					int index = Integer.parseUnsignedInt(in.getParameters().get(OPTION_INDEX)[0]);
					selectedNode = selectedNode.getChildNodeByIndex(index);
				}
				catch(NumberFormatException numex){
					out.setColor(AnsiColor.RED);
					out.printf("Cannot parse '%s' as integer.\n", in.getParameters().get(OPTION_INDEX)[0]);
					return CommandOutput.error();
				}
				catch(IndexOutOfBoundsException ioobex){
					out.setColor(AnsiColor.RED);
					out.printf("Unable to find child node with index '%s'.\n", in.getParameters().get(OPTION_INDEX)[0]);
					return CommandOutput.error();
				}
			}
		}

		if(!in.getParameters().containsKey(OPTION_GET)){
			instance.getTree().setSelectedNode(selectedNode);
		}

		if(!in.outputIsPiped()){
			out.println(selectedNode.getNodePath());
		}

		return CommandOutput.success(selectedNode);
	}
}
