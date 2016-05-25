/*	KeyMinder
	Copyright (C) 2016 Bastian Kraemer

	CommandBuilder.java

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
package de.akubix.keyminder.shell.parse;

import java.util.ArrayList;
import java.util.List;

public class CommandBuilder {
	private final List<ParsedCommand> finalList;
	private String command;
	private List<String> arguments;
	private StringBuilder originalCommand;
	public CommandBuilder(){
		this.finalList = new ArrayList<>(8);
		command = null;
		arguments = new ArrayList<>(8);
		originalCommand = null;
	}

	public void addCommandPart(String str) throws IllegalArgumentException {
		if(str.equals(";")){
			originalCommand.append(";");
			endOfCommand(ShellExecOption.NONE);
		}
		else if(str.equals("|")){
			originalCommand.append(" |");
			endOfCommand(ShellExecOption.PIPE);
		}
		else if(str.equals("&&")){
			originalCommand.append(" &&");
			endOfCommand(ShellExecOption.REQUIRE_EXIT_0);
		}
		else{
			if(command == null){
				command = str;
				originalCommand = new StringBuilder();
			}
			else{
				arguments.add(str);
				originalCommand.append(" ").append(str);
			}
		}
	}

	private void endOfCommand(ShellExecOption option){
		if(command != null){
			finalList.add(new ParsedCommand(command, arguments, option, originalCommand.toString()));
			command = null;
			arguments = new ArrayList<>(8);
		}
	}

	public List<ParsedCommand> getList(){
		if(command != null){
			addCommandPart(";");
		}
		return finalList;
	}
}
