package de.akubix.keyminder.plugins.sshtools;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.AllowCallWithoutArguments;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("socks")
@AllowCallWithoutArguments
@Description("Starts or stops a socks profile")
@Operands(cnt = 2, description = "[ 'start' | 'stop' ] SOCKS_PROFILE_NAME")
public class SocksCmd extends AbstractShellCommand {
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		SSHTools sshtools = (SSHTools) instance.getPluginLoader().getPluginInfo("SSH-Tools").getInstance();

		if(in.getParameters().containsKey("$0")){
			String command = in.getParameters().get("$0")[0];
			String profileName = in.getParameters().get("$1")[0];

			if(!sshtools.socksProfileExists(profileName)){
				out.setColor(AnsiColor.RED);
				out.printf("Socks profile '%s' does not exist.", profileName);
				return CommandOutput.error();
			}

			switch(command.toLowerCase()) {
				case "start":
					if(sshtools.startSocksProfile(profileName)){
						out.setColor(AnsiColor.GREEN);
						out.println("Socks-Profile started.");
						break;
					} else {
						out.setColor(AnsiColor.RED);
						out.println("Start of Socks-Profile failed!");
						return CommandOutput.error();
					}

				case "stop":
					sshtools.stopSocksProfile(profileName);
					break;

				default:
					out.setColor(AnsiColor.YELLOW);
					out.printf("Unknown option '%s'. Use 'man socks' for more information.", command);
					return CommandOutput.error();
			}
		}
		else{
			out.println("All available Socks-Profiles:");
			sshtools.forEachSocksProfile((name) -> out.printf("	- %s\n", name));

			out.println("\n\nCurrently running Socks-Profiles:");
			sshtools.forEachActiveSocksProfile((name, process) -> out.printf("	- %s\n", name));
		}

		return CommandOutput.success();
	}
}
