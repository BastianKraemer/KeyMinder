package de.akubix.keyminder.modules.sshtools;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.Usage;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("run")
@Operands(cnt = 2, nodeArgAt = 1, optionalNodeArg = true)
@Option(name = "--noforward")
@Option(name = "--socks", paramCnt = 1, alias = "-s")
@Description("(Module SSH-Tools) Starts a application by using the values stored in a tree node.\n" +
			 "The command line mapping needs to be defined in an 'XML Application Profile' (Take a look.")
@Usage(	"${command.name} [profile name] <nodename> [--noforward] [--socks socksprofile*]\n\n" +
		"*Maybe the '--socks' options is not supported in all application profiles.")
public class AppStartCmd extends AbstractShellCommand {
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		SSHTools sshtools = (SSHTools) instance.getModuleLoader().getModuleInfo("SSH-Tools").getInstance();

		String starterName = in.getParameters().get("$0")[0];
		AppStarter as = sshtools.getAppStarterByName(starterName);

		boolean ignoreForward = in.getParameters().containsKey("--noforward");
		if(in.getParameters().containsKey("--socks")){
			out.println(sshtools.startApplication(as, in.getTreeNode(), ignoreForward, in.getParameters().get("--socks")[0]));
		}
		else{
			out.println(sshtools.startApplication(as, in.getTreeNode(), ignoreForward, null));
		}
		return null;
	}
}
