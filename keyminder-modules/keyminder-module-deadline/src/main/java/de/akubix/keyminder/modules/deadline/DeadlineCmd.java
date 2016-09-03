package de.akubix.keyminder.modules.deadline;

import java.text.SimpleDateFormat;
import java.time.ZoneId;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.RequireOpenedFile;
import de.akubix.keyminder.shell.annotations.Usage;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("deadline")
@RequireOpenedFile
@Operands(cnt = 2, nodeArgAt = 1, optionalNodeArg = true)
@Description("Adds or removes a deadline (e.g an expiration date) to a tree node.")
@Usage(	"To set a deadline:\n" +
		"    ${command.name} <date> [/path/to/node] <date>\n" +
		"    ${command.name} reset [/path/to/node]\n" +
		"To run a check for expired nodes:" +
		"    ${command.name} check\n\n" +
		"You can use '" + AbstractShellCommand.REFERENCE_TO_STDIN_KEYWORD + "' to take this value from the piped input data.")
@PipeInfo(in = "TreeNode, String, Long", out = "TreeNode")
public class DeadlineCmd extends AbstractShellCommand {
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		Deadline deadline = (Deadline) instance.getModuleLoader().getModuleInfo("Deadline").getInstance();

		TreeNode node;
		if(in.getParameters().containsKey("$1")){
			node = in.getTreeNode();
		}
		else{
			if(in.getInputData() != null){
				if(in.getInputData() instanceof TreeNode){
					node = (TreeNode) in.getInputData();
				}
				else{
					super.printUnusableInputWarning(out);
					return CommandOutput.error();
				}
			}
			else{
				node = in.getTreeNode();
			}
		}

		String value = in.getParameters().get("$0")[0];
		switch(value){
			case "check":
				deadline.checkForExpiredNodes(true);
				break;

			case "reset":
			case "none":
			case "-":
				node.removeAttribute(Deadline.NODE_EXPIRATION_ATTRIBUTE);
				out.println(String.format("Deadline of '%s' removed.", node.getText()));
				break;

			default:
				try{
					long epochMilli = -1;
					if(value.equals(AbstractShellCommand.REFERENCE_TO_STDIN_KEYWORD)){
						if(in.getInputData() != null){
							if(in.getInputData() instanceof String){
								value = (String) in.getInputData();
							}
							else if(in.getInputData() instanceof Long){
								epochMilli = (Long) in.getInputData();
							}
							else{
								super.printUnusableInputWarning(out);
							}
						}
					}

					if(epochMilli == -1){
						SimpleDateFormat sda = new SimpleDateFormat("dd.MM.yyyy");
						epochMilli = sda.parse(value).toInstant().atZone(ZoneId.systemDefault()).toEpochSecond() * 1000;
					}

					node.setAttribute(Deadline.NODE_EXPIRATION_ATTRIBUTE, Long.toString(epochMilli));
					if(!in.outputIsPiped()){
						out.setColor(AnsiColor.GREEN);
						out.println("Deadline successfully added.");
					}

					return CommandOutput.success(node);
				}
				catch (Exception e){
					out.setColor(AnsiColor.YELLOW);
					out.printf("Unable to parse date '%s'. Required format: 'dd.MM.yyyy'.\n", value);
					return CommandOutput.error();
				}
		}

		return CommandOutput.success(node);
	}
}
