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
import de.akubix.keyminder.core.exceptions.InvalidValueException;
import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.shell.annotations.AllowCallWithoutArguments;
import de.akubix.keyminder.shell.annotations.NoArgs;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.RequireOpenedFile;
import de.akubix.keyminder.shell.annotations.RequiredOptions;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;

public abstract class AbstractShellCommand implements ShellCommand{

	public static final String REFERENCE_TO_STDIN_KEYWORD = "@-";

	@Override
	public CommandInput parseArguments(ApplicationInstance instance, List<String> args) throws CommandException {

		if(getClass().getAnnotation(RequireOpenedFile.class) != null && !instance.isAnyFileOpened()){
			throw new CommandException("You have to open a password file before you can use this command.");
		}

		NoArgs noArgs = getClass().getAnnotation(NoArgs.class);
		if(noArgs != null && args.size() > 0){
			throw new CommandException("This command does not take any arguments.");
		}

		try{
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
						TreeNode tmp = instance.getTree().getNodeByPath(args.get(i));
						if(tmp != null){
							parameters.put("$" + operandArgIndex, new String[]{args.get(i)});
							node = tmp;
						}
						else{
							if(operands.optionalNodeArg()){
								// Set this argument as the value of the next operand
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

			if(args.size() > 0 || getClass().getAnnotation(AllowCallWithoutArguments.class) == null){
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
			}

			for(Option o: optionList){
				if(o.defaultValue().length > 0 && !parameters.containsKey(o.name())){
					parameters.put(o.name(), o.defaultValue());
				}
			}

			return new CommandInput(parameters, node);
		}
		catch(IndexOutOfBoundsException e){
			throw new CommandException("Malformed command syntax. Try 'man <command>' for more information.");
		}
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

	protected static void checkInputObjectArgumentConflict(ShellOutputWriter out, CommandInput in, String... argumentKeys){
		for(String arg: argumentKeys){
			if(in.getParameters().containsKey(arg)){
				printInputObjectArgumentConflictWarning(out);
				return;
			}
		}
	}

	private static void printInputObjectArgumentConflictWarning(ShellOutputWriter out){
		out.setColor(AnsiColor.YELLOW);
		out.println("WARNING: The tree node of the input data conflicts with the parameters.\n" +
					"         In this case the input data will be used instead as data source.");
		out.setColor(AnsiColor.RESET);
	}

	protected static void printUnusableInputWarning(ShellOutputWriter out){
		out.setColor(AnsiColor.YELLOW);
		out.println("WARNING: Invalid input data for this command. Object type is not supported.");
		out.setColor(AnsiColor.RESET);
	}

	protected static void printNoInputDataError(ShellOutputWriter out){
		out.setColor(AnsiColor.RED);
		out.println("ERROR: Referenced input data is not available.");
		out.setColor(AnsiColor.RESET);
	}

	protected static TreeNode getNodeFromPathOrStdIn(ApplicationInstance instance, CommandInput in, String nodePathString) throws InvalidValueException {

		if(nodePathString.equals(REFERENCE_TO_STDIN_KEYWORD)){
			if(in.getInputData() instanceof TreeNode){
				return (TreeNode) in.getInputData();
			}
			else if(in.getInputData() instanceof String){
				nodePathString = (String) in.getInputData();
			}
			else{
				throw new IllegalArgumentException("Input data object is not a 'TreeNode' nor a 'String'");
			}
		}

		TreeNode node = instance.getTree().getNodeByPath(nodePathString);
		if(node == null){
			throw new InvalidValueException(String.format("Node '%s' does not exist.\n", nodePathString));
		}
		return node;
	}
}
