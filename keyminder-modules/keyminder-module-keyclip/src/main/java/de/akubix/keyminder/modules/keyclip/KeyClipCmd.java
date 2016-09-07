package de.akubix.keyminder.modules.keyclip;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.RequireOpenedFile;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("keyclip")
@RequireOpenedFile
@Description("Command line interface for the KeyClip module.")
@Operands(cnt = 1, nodeArgAt = 0, optionalNodeArg = true, description = "{NODE_PATH}")
@Option(name = KeyClipCmd.OPTION_USER, paramCnt = 1, alias = "-u",               description = "USERNAME  The username")
@Option(name = KeyClipCmd.OPTION_PASSWORD, paramCnt = 1, alias = {"--pw", "-p"}, description = "PASSWORD  The password")
public class KeyClipCmd extends AbstractShellCommand {

	static final String OPTION_USER = "--user";
	static final String OPTION_PASSWORD = "--password";

	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {

		KeyClip keyclip = (KeyClip) instance.getModuleLoader().getModuleInfo("KeyClip").getInstance();

		String username = in.getParameters().containsKey(OPTION_USER) ?
				in.getParameters().get(OPTION_USER)[0] :
				in.getTreeNode().getAttribute("username");

		String password = in.getParameters().containsKey(OPTION_PASSWORD) ?
				in.getParameters().get(OPTION_PASSWORD)[0] :
				in.getTreeNode().getAttribute("password");

		keyclip.copyUserAndPassword(username, password);
		return CommandOutput.success();
	}
}
