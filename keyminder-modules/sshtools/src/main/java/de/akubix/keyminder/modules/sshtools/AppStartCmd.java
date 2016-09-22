package de.akubix.keyminder.modules.sshtools;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Note;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("run")
@Description(	"(Module SSH-Tools) Starts an application by using the values stored in a tree node.\n" +
				"The command line mapping needs to be defined in a 'command line descriptor' file (Take a look at the KeyMinder manual).")
@Operands(cnt = 2, nodeArgAt = 1, optionalNodeArg = true, description = "PROFILE_NAME {NODE_PATH}")
@Option(name = AppStartCmd.OPTION_NO_PPRT_FORWARDING, alias = {"--no-pf", "--no-forward"}, description = "Disable port forwarding")
@Option(name = AppStartCmd.OPTION_USE_SOCKS, paramCnt = 1, alias = "-s",                   description = "SOCKS_PROFILE_NAME  Start the applikation using a socks profile*")
@Note("* Maybe the '--socks' options is not supported in all application profiles.")
public class AppStartCmd extends AbstractShellCommand {

	static final String OPTION_NO_PPRT_FORWARDING = "--no-port-forwarding";
	static final String OPTION_USE_SOCKS = "--socks";

	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		SSHTools sshtools = (SSHTools) instance.getModuleLoader().getModuleInfo("SSH-Tools").getInstance();

		String starterName = in.getParameters().get("$0")[0];
		AppStarter as = sshtools.getAppStarterByName(starterName);

		boolean ignoreForward = in.getParameters().containsKey(OPTION_NO_PPRT_FORWARDING);
		if(in.getParameters().containsKey(OPTION_USE_SOCKS)){
			out.println(sshtools.startApplication(as, in.getTreeNode(), ignoreForward, in.getParameters().get("--socks")[0]));
		}
		else{
			out.println(sshtools.startApplication(as, in.getTreeNode(), ignoreForward, null));
		}
		return null;
	}
}