/*	KeyMinder
	Copyright (C) 2016 Bastian Kraemer

	ModuleCmd.java

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.akubix.keyminder.shell.commands;

import java.io.IOException;
import java.util.Properties;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.modules.ModuleInfo;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.AllowCallWithoutArguments;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.Usage;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@AllowCallWithoutArguments
@Operands(cnt = 1)
@Option(name = "--info", alias = {"--about", "-i"})
@Option(name = "--enable", alias = "-e")
@Option(name = "--disable", alias = "-d")
@Description("Enables and disables a KeyMinder module or view a list of all modules.")
@Usage(	"${command.name} [module name] [options]\n\n" +
		"--info, -i    Prints some module information\n" +
		"--enable, -e  Enables a module\n" +
		"--disable, -d Disables a module\n\n" +
		"Running just '${command.name}' will display a list of all available modules.")
public final class ModuleCmd extends AbstractShellCommand {
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in){

		if(in.getParameters().containsKey("$0")){
			String moduleName = in.getParameters().get("$0")[0];

			if(in.getParameters().containsKey("--enable") && in.getParameters().containsKey("--disable")){
				out.setColor(AnsiColor.RED);
				out.println("You cannot enable and disable a module at the same time.");
				return CommandOutput.error();
			}

			try{
				if(in.getParameters().containsKey("--enable")){
					instance.getModuleLoader().enableModule(moduleName);
				}
				else if(in.getParameters().containsKey("--disable")){
					instance.getModuleLoader().disableModule(moduleName);
				}

				if(in.getParameters().containsKey("--info")){
					ModuleInfo m = instance.getModuleLoader().getModuleInfo(moduleName);
					if(m == null){
						throw new IllegalArgumentException("Module does not exist.");
					}

					try {
						Properties properties;
						properties = m.getProperties();
						out.println(String.format(
							"Module name: \t%s\n" +
							"Version: \t%s\n" +
							"Author: \t%s\n" +
							"Status: \t%s\n\n" +
							"Description:\n%s",
							moduleName,
							properties.getOrDefault("version", "-"),
							properties.getOrDefault("author", "-"),
							(m.isEnabled() ? (m.isStarted() ? "ENABLED" : "ENABLED (Startup error)" ): "DISABLED"),
							properties.getOrDefault("description", "-")));

					} catch (IOException | NullPointerException e) {
						out.setColor(AnsiColor.RED);
						out.println("ERROR: Cannot load module property file: " + e.getMessage());
					}
				}
				else{
					out.printf("No information for module '%s' available.\n", moduleName);
				}
			}
			catch(IllegalArgumentException IllArgEx){
				out.setColor(AnsiColor.YELLOW);
				out.println(IllArgEx.getMessage());
				return CommandOutput.error();
			}
		}
		else{
			out.println("Status\t\tModule name\n" +
						"------\t\t-----------");

			for(String name: instance.getModuleLoader().getModules()){
				ModuleInfo m = instance.getModuleLoader().getModuleInfo(name);
				out.println(String.format("%s%s",
						(m.isEnabled() ?
								(m.isStarted() ? "ENABLED\t\t" : "ENABLED (!)\t") :
								(m.isStarted() ? "ENABLED (-)\t" : "DISABLED\t")),
						name));
			}

			out.println("\n(!) This module is currently not started, this could happen if the module crashed on startup or if it has enabled during this session.\n" +
						"(-) This module will be disabled at the next start.");
		}

		return CommandOutput.success();
	}
}
