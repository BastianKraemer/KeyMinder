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
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;
import de.akubix.keyminder.util.Utilities;

@Command("config")
@Description("View or modify the configuration")
@Option(name = ConfigCmd.OPTION_FILE_CONFIG,          alias={"-f", "-fc", "-file", "--file"}, description = "Use the file configuration instead of the global settings")
@Option(name = ConfigCmd.OPTION_RELOAD,               alias={"-r", "-R"},                     description = "Reloads the global settings")
@Option(name = ConfigCmd.OPTION_DELETE, paramCnt = 1, alias = {"-d", "-D"},                   description = "KEY  Delete a settings key")
@Option(name = ConfigCmd.OPTION_SET,    paramCnt = 2, alias = "-s",                           description = "KEY VALUE   Add custom settings")
@Option(name = ConfigCmd.OPTION_SAVE,                 alias={"-S"},                           description = "Saves the global settings file")
@Option(name = ConfigCmd.OPTION_PRINT,                alias={"-p"},                           description = "Prints out the current settings")
public final class ConfigCmd extends AbstractShellCommand {

	static final String OPTION_FILE_CONFIG = "--fileconfig";
	static final String OPTION_RELOAD = "--reload";
	static final String OPTION_DELETE = "--delete";
	static final String OPTION_SET = "--set";
	static final String OPTION_SAVE = "--save";
	static final String OPTION_PRINT = "--print";

	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		boolean updateFileSettings;
		boolean noneOfTheAbove = true;

		if(in.getParameters().containsKey(OPTION_FILE_CONFIG)){
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

		if(in.getParameters().containsKey(OPTION_RELOAD)){
			instance.reloadSettings();
			noneOfTheAbove = false;
		}

		if(in.getParameters().containsKey(OPTION_DELETE)){
			noneOfTheAbove = false;
			String keyName = in.getParameters().get(OPTION_DELETE)[0];
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

		if(in.getParameters().containsKey(OPTION_SET)){
			noneOfTheAbove = false;
			String name = in.getParameters().get(OPTION_SET)[0];
			String value = in.getParameters().get(OPTION_SET)[1];
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

		if(in.getParameters().containsKey(OPTION_SAVE)){
			noneOfTheAbove = false;
			instance.saveSettings();
		}

		if(in.getParameters().containsKey(OPTION_PRINT) || noneOfTheAbove){
			printSettingsMap(out, instance, updateFileSettings);
		}

		return CommandOutput.success();
	}

	private static void printSettingsMap(ShellOutputWriter out, ApplicationInstance instance, boolean useFileSettings){
		Utilities.asSortedList(useFileSettings ? instance.getFileSettingsKeySet(): instance.getSettingsKeySet()).forEach((String key) -> {
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
