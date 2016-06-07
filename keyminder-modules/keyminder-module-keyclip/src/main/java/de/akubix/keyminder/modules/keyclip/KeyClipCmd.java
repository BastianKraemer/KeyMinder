package de.akubix.keyminder.modules.keyclip;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.RequireOpenedFile;
import de.akubix.keyminder.shell.annotations.Usage;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@RequireOpenedFile
@Operands(cnt = 1, nodeArgAt = 0, optionalNodeArg = true)
@Option(name = "--user", paramCnt = 1, alias = "-u")
@Option(name = "--password", paramCnt = 1, alias = {"-{pw", "-p"})
@Description("Command line interface for the KeyClip module.")
@Usage(	"${command.name} </path/to/node/>\n" +
		"${command.name} --username [username]\n" +
		"${command.name} --password [password]")
public class KeyClipCmd extends AbstractShellCommand {
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {

		KeyClip keyclip = (KeyClip) instance.getModuleLoader().getModuleInfo("KeyClip").getInstance();

		String username = in.getParameters().containsKey("--user") ?
				in.getParameters().get("--user")[0] :
				in.getTreeNode().getAttribute("username");

		String password = in.getParameters().containsKey("--password") ?
				in.getParameters().get("--password")[0] :
				in.getTreeNode().getAttribute("password");

		keyclip.copyUserAndPassword(username, password);
		return CommandOutput.success();
	}
}
