/*	KeyMinder
	Copyright (C) 2016 Bastian Kraemer

	ConfigCmd.java

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

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.events.EventTypes.DefaultEvent;
import de.akubix.keyminder.lib.Tools;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Usage;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

@Command("config")
@Option(name = "--fileconfig", alias={"-f", "-fc", "-file", "--file"})
@Option(name = "--reload", alias={"-r", "-R"})
@Option(name = "--delete", paramCnt = 1, alias = {"-d", "-D"})
@Option(name = "--set", paramCnt = 2, alias = "-s")
@Option(name = "--save", alias={"-S"})
@Option(name = "--print", alias={"-p"})
@Description("View or mdoify the KeyMinder configuration")
@Usage( "${command.name} [options]\n\n" +
		"Available options:\n" +
		"  --fileconfig, -f: Use the file configuration instead of the global settings.\n" +
		"  --reload, -r: Reloads the global settings\n" +
		"  --delete, -d [key]: Delete a settings key\n" +
		"  --set, -s [key] [value]: Add custom settings" +
		"  --save, -S: Saves the global settings file\n" +
		"  --print, -p Prints out the current settings")
public final class ConfigCmd extends AbstractShellCommand {
	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		boolean updateFileSettings;
		boolean noneOfTheAbove = true;

		if(in.getParameters().containsKey("--fileconfig")){
			if(!instance.isAnyFileOpened()){
				out.setColor(AnsiColor.RED);
				out.println("No file opened.");
				return CommandOutput.error();
			}
			updateFileSettings = true;
		}
		else{
			updateFileSettings = false;
		}

		if(in.getParameters().containsKey("--reload")){
			instance.reloadSettings();
			noneOfTheAbove = false;
		}

		if(in.getParameters().containsKey("--delete")){
			noneOfTheAbove = false;
			String keyName = in.getParameters().get("--delete")[0];
			boolean result = updateFileSettings ? instance.removeFileSettingsValue(keyName) : instance.removeSettingsValue(keyName);
			if(result){
				instance.fireEvent(updateFileSettings ? DefaultEvent.OnFileSettingsChanged : DefaultEvent.OnSettingsChanged);
			}
			else{
				out.setColor(AnsiColor.YELLOW);
				out.println(String.format("Settings key '%s' does not exist.", keyName));
				return CommandOutput.error();
			}
		}

		if(in.getParameters().containsKey("--set")){
			noneOfTheAbove = false;
			String name = in.getParameters().get("--set")[0];
			String value = in.getParameters().get("--set")[1];
			try{

				if(updateFileSettings){
					instance.setFileSettingsValue(name, value);
					instance.fireEvent(DefaultEvent.OnFileSettingsChanged);
				}
				else{
					instance.setSettingsValue(name, value);
					instance.fireEvent(DefaultEvent.OnSettingsChanged);
				}
			}
			catch(IllegalArgumentException argEx){
				out.setColor(AnsiColor.YELLOW);
				out.println(argEx.getMessage());
			}

		}

		if(in.getParameters().containsKey("--save")){
			noneOfTheAbove = false;
			instance.saveSettings();
		}

		if(in.getParameters().containsKey("--print") || noneOfTheAbove){
			printSettingsMap(out, instance, updateFileSettings);
		}

		return CommandOutput.success();
	}

	private static void printSettingsMap(ShellOutputWriter out, ApplicationInstance instance, boolean useFileSettings){
		Tools.asSortedList(useFileSettings ? instance.getFileSettingsKeySet(): instance.getSettingsKeySet()).forEach((String key) -> {
			String value = useFileSettings ? instance.getFileSettingsValue(key) : instance.getSettingsValue(key);
			if(value.contains("\n")){
				out.print(String.format("%s = ", key));
				for(String str: value.split("\n")){
					out.print(String.format("    %s", str));
				}
				out.print("\n");
			}
			else{
				String lCaseKey = key.toString().toLowerCase();
				if(lCaseKey.contains("password") || lCaseKey.contains("pw") || lCaseKey.contains("paswd")){
					out.println(String.format("%s = *****", key));
				}
				else{
					out.println(String.format("%s = %s", key, value));
				}
			}
		});
	}
}
