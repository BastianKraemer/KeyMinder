/*	KeyMinder
	Copyright (C) 2016 Bastian Kraemer

	PluginCmd.java

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
import de.akubix.keyminder.core.plugins.PluginInfo;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.AllowCallWithoutArguments;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Note;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("plugins")
@AllowCallWithoutArguments
@Description("Enables/disables a KeyMinder plugins or shows a list of all plugins")
@Operands(cnt = 1, description = "PLUGIN_NAME")
@Option(name = PluginCmd.OPTION_INFO, alias = {"--about", "-i"}, description = "Prints some plugin information")
@Option(name = PluginCmd.OPTION_ENABLE, alias = "-e",            description = "Enables a plugin")
@Option(name = PluginCmd.OPTION_DISABLE, alias = "-d",           description = "Disables a plugin")
@Note("Running just 'plugins' will display a list of all available plugins.")
public final class PluginCmd extends AbstractShellCommand {

	static final String OPTION_INFO = "--info";
	static final String OPTION_ENABLE = "--enable";
	static final String OPTION_DISABLE = "--disable";

	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in){

		if(in.getParameters().containsKey("$0")){
			String pluginName = in.getParameters().get("$0")[0];

			if(in.getParameters().containsKey(OPTION_ENABLE) && in.getParameters().containsKey(OPTION_DISABLE)){
				out.setColor(AnsiColor.RED);
				out.println("You cannot enable and disable a plugin at the same time.");
				return CommandOutput.error();
			}

			try{
				if(in.getParameters().containsKey(OPTION_ENABLE)){
					instance.getPluginLoader().enablePlugin(pluginName);
				}
				else if(in.getParameters().containsKey(OPTION_DISABLE)){
					instance.getPluginLoader().disablePlugin(pluginName);
				}

				if(in.getParameters().containsKey(OPTION_INFO)){
					PluginInfo m = instance.getPluginLoader().getPluginInfo(pluginName);
					if(m == null){
						throw new IllegalArgumentException("Plugin does not exist.");
					}

					try {
						Properties properties;
						properties = m.getProperties();
						out.println(String.format(
							"Plugin name: \t%s\n" +
							"Version: \t%s\n" +
							"Author: \t%s\n" +
							"Status: \t%s\n\n" +
							"Description:\n%s",
							pluginName,
							properties.getOrDefault("version", "-"),
							properties.getOrDefault("author", "-"),
							(m.isEnabled() ? (m.isStarted() ? "ENABLED" : "ENABLED (Startup error)" ): "DISABLED"),
							properties.getOrDefault("description", "-")));

					} catch (IOException | NullPointerException e) {
						out.setColor(AnsiColor.RED);
						out.println("ERROR: Cannot load plugin property file: " + e.getMessage());
					}
				}
				else{
					out.printf("No information for plugin '%s' available.\n", pluginName);
				}
			}
			catch(IllegalArgumentException IllArgEx){
				out.setColor(AnsiColor.YELLOW);
				out.println(IllArgEx.getMessage());
				return CommandOutput.error();
			}
		}
		else{
			out.println("Status\t\tPlug-in name\n" +
						"------\t\t-----------");

			for(String name: instance.getPluginLoader().getPlugins()){
				PluginInfo m = instance.getPluginLoader().getPluginInfo(name);
				out.println(String.format("%s%s",
						(m.isEnabled() ?
								(m.isStarted() ? "ENABLED\t\t" : "ENABLED (!)\t") :
								(m.isStarted() ? "ENABLED (-)\t" : "DISABLED\t")),
						name));
			}

			out.println("\n(!) This plugin is currently not started, this could happen if the plugin crashed on startup or if it has enabled during this session.\n" +
						"(-) This plugin will be disabled at the next start.");
		}

		return CommandOutput.success();
	}
}
