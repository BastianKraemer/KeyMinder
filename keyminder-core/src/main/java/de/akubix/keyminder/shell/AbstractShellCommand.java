/*	KeyMinder
	Copyright (C) 2016 Bastian Kraemer

	AbstractShellCommand.java

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
package de.akubix.keyminder.shell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.shell.annotations.NoArgs;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.RequireOpenedFile;
import de.akubix.keyminder.shell.annotations.RequiredOptions;
import de.akubix.keyminder.shell.io.CommandInput;

public abstract class AbstractShellCommand implements ShellCommand{
	@Override
	public CommandInput parseArguments(ApplicationInstance instance, List<String> args) throws CommandException {

		if(getClass().getAnnotationsByType(RequireOpenedFile.class).length > 0 && instance.currentFile == null){
			throw new CommandException("Error: You have to open a password file before you can use this command.");
		}

		if(getClass().getAnnotationsByType(NoArgs.class).length > 0 && args.size() > 0){
			throw new CommandException("Error: This command does not take any arguments.");
		}

		Map<String, Option> knownOptions = new HashMap<>();
		Map<String, String[]> parameters = new HashMap<>();

		Operands operands = getOperandAnnotation();
		Option[] optionList = getClass().getAnnotationsByType(Option.class);
		List<String> requiredOptions = getRequiredOptionsList();

		for(Option o: optionList){
			knownOptions.put(o.name(), o);
			for(String alias: o.alias()){
				knownOptions.put(alias, o);
			}
		}

		TreeNode node = instance.getTree().getSelectedNode();

		int operandArgIndex = 0;

		for(int i = 0; i < args.size(); i++){
			// This could be a parameter
			if(args.get(i).startsWith("-")){
				if(knownOptions.containsKey(args.get(i))){
					Option o = knownOptions.get(args.get(i));
					String[] values = new String[o.paramCnt()];
					for(int j = 0; j < o.paramCnt(); j++){
						values[j] = args.get(++i);
					}
					parameters.put(o.name(), values);
					continue;
				}
				// maybe it was not - try to use argument as command line option
			}

			// Are there any arguments without parameters?
			if(operands == null || operandArgIndex >= operands.cnt()){
				// To many anonymous arguments
				throw new CommandException(String.format("Unknown argument '%s'.", args.get(i)));
			}
			else{
				if(operands.nodeArgAt() == operandArgIndex){
					node = instance.getTree().getNodeByPath(args.get(i));
					if(node != null){
						parameters.put("$" + operandArgIndex, new String[]{args.get(i)});
					}
					else{
						if(operands.optionalNodeArg()){
							parameters.put("$" + (++operandArgIndex), new String[]{args.get(i)});
						}
						else {
							throw new CommandException(String.format("Node '%s' does not exist.", args.get(i)));
						}
					}
				}
				else{
					parameters.put("$" + operandArgIndex, new String[]{args.get(i)});
				}

				operandArgIndex++;
			}
		}

		if(operands != null){
			int hasOptionalNodeArg = (operands.nodeArgAt() >= 0 && operands.optionalNodeArg()) ? 1 : 0;
			if(operandArgIndex < (operands.cnt() - hasOptionalNodeArg)){
				// Not enough operands for this command
				throw new CommandException("Cannot execute command: Missing operand. Try 'man <command>' for more information.");
			}
		}

		for(String requiredSwitch: requiredOptions){
			if(!parameters.containsKey(requiredSwitch)){
				throw new CommandException(String.format("Required option '%s' is missing.", requiredSwitch));
			}
		}

		for(Option o: optionList){
			if(o.defaultValue().length > 0 && !parameters.containsKey(o.name())){
				parameters.put(o.name(), o.defaultValue());
			}
		}

		return new CommandInput(parameters, node);
	}

	private Operands getOperandAnnotation(){
		Operands[] anonymousArgs = getClass().getAnnotationsByType(Operands.class);
		if(anonymousArgs.length > 0){
			return anonymousArgs[0];
		}
		return null;
	}

	private List<String> getRequiredOptionsList(){
		RequiredOptions[] requiredOptions = getClass().getAnnotationsByType(RequiredOptions.class);
		List<String> list = new ArrayList<>();
		if(requiredOptions.length > 0){
			for(int i = 0; i < requiredOptions[0].names().length; i++){
				list.add(requiredOptions[0].names()[i]);
			}
		}
		return list;
	}
}
