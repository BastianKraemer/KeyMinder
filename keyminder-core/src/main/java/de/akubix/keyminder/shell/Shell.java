/*	KeyMinder
	Copyright (C) 2016 Bastian Kraemer

	Shell.java

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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.KeyMinder;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.PipeInfo;
import de.akubix.keyminder.shell.annotations.Usage;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;
import de.akubix.keyminder.shell.parse.CommandBuilder;
import de.akubix.keyminder.shell.parse.ParsedCommand;
import de.akubix.keyminder.shell.parse.ShellExecOption;

/**
 * Core of the KeyMinder Terminal.
 * This class handles parsing and execution of every input string.
 *
 * <p>To run any command, the shell has to know it, therefore you have to add
 * commands to the shell, for example using {@link Shell#addCommand(String, String)}
 * </p>
 * <p>After this you can execute commands using {@link Shell#runShellCommand(String)}.
 * Here are some examples for possible command expressions:
 * </p><ul>
 * <li>{@code echo "Hello world"}</li>
 * <li>{@code echo Hello; echo world}</li>
 * <li>{@code echo Hello && echo world"}</li>
 * <li>{@code add "New node" | set hello world}</li>
 * </ul>
 */
public class Shell {

	private ApplicationInstance instance;

	Map<String, Class<? extends ShellCommand>> availableCommands = new HashMap<>();
	Map<String, String> aliasMap = new HashMap<>();

	/**
	 * Create a new Shell instance
	 * @param instance The application instance that is bound to the shell
	 */
	public Shell(ApplicationInstance instance){
		this.instance = instance;
	}

	/**
	 * Add a command to the shell
	 * @param name The name of the command
	 * @param classPath
	 */
	public void addCommand(String name, String classPath){
		ClassLoader classLoader = this.getClass().getClassLoader();
		addCommand(name, classPath, classLoader);
	}

	/**
	 * Loads a command list from an ini file
	 * @param resourcePath
	 */
	public void loadCommandsFromIniFile(String resourcePath){
		try {
			new IniCommandLoader().load(resourcePath, this);
		}
		catch (IOException e) {
			if(KeyMinder.verbose_mode){
				instance.printf("Error: Cannot load commands from resource file '%s': %s",
								resourcePath, e.getMessage());
			}
		}
	}

	/**
	 * Adds an alias command to the shell
	 * @param alias
	 * @param value
	 */
	public void addAlias(String alias, String value){
		aliasMap.put(alias, value);
	}

	@SuppressWarnings("unchecked")
	protected void addCommand(String name, String classPath, ClassLoader classLoader){
		name = name.toLowerCase();
		try {
			if(!availableCommands.containsKey(name)){
				Class<?> loadedClass = classLoader.loadClass(classPath);
				if(ShellCommand.class.isAssignableFrom(loadedClass)){
					availableCommands.put(name, (Class<? extends ShellCommand>) loadedClass);
				}
			}
		} catch (ClassNotFoundException e){
			if(KeyMinder.verbose_mode){
				instance.printf("Cannot load command '%s'. Unable to find class '%s'", name, classPath);
			}
		}
	}

	/**
	 * Returns the description an usage of a command (the values are taken from the annotations {@link Description} and {@link Usage})
	 * @param cmd The name of the command
	 * @return String array with the description (index 0) and the usage definition (index 1). Both values can be {@code null} if there was no annotation
	 * @throws CommandException if the command does not exist
	 */
	public String[] getManual(String cmd) throws CommandException {
		if(availableCommands.containsKey(cmd.toLowerCase())){
			Description desc = availableCommands.get(cmd.toLowerCase()).getAnnotation(Description.class);
			Usage usage = availableCommands.get(cmd.toLowerCase()).getAnnotation(Usage.class);

			return new String[]{
				desc != null ? desc.value() : null,
				usage != null ? usage.value() : null
			};
		}
		else{
			if(aliasMap.containsKey(cmd)){
				return new String[]{
					String.format("'%s' is an alias for '%s'", cmd, aliasMap.get(cmd)),
					null};
			}
			throw new CommandException(String.format("Unknown command '%s'", cmd));
		}
	}

	/**
	 * Returns the pipe information stored in the {@link PipeInfo} annotation of each command)
	 * @param cmd The name of the command
	 * @return The pipe information
	 * @throws CommandException if the command does not exist (or is just an alias)
	 */
	public PipeInfo getPipeInformation(String cmd) throws CommandException {
		if(availableCommands.containsKey(cmd.toLowerCase())){
			return availableCommands.get(cmd.toLowerCase()).getAnnotation(PipeInfo.class);
		}
		else{
			if(aliasMap.containsKey(cmd)){
				throw new CommandException("Pipe informations are not availabe for alias commands");
			}
			throw new CommandException(String.format("Unknown command '%s'", cmd));
		}
	}

	/**
	 * Executes a command string
	 * @param commandLineInput
	 * @throws CommandException
	 * @throws UserCanceledOperationException when the user entered the 'exit' command
	 */
	public void runShellCommand(ShellOutputWriter outWriter, String commandLineInput) throws CommandException, UserCanceledOperationException {
		List<ParsedCommand> cmdList = parseCommandLineString(commandLineInput.trim());

		for(int i = 0; i < cmdList.size(); i++){
			ParsedCommand p = cmdList.get(i);
			if(aliasMap.containsKey(p.getCommand())){
				List<ParsedCommand> aliasCmd = parseCommandLineString(aliasMap.get(p.getCommand()) + " " + p.getUnparsedArgs());
				if(aliasCmd.size() == 1){
					cmdList.set(i, aliasCmd.get(0));
				}
				else{
					throw new CommandException(String.format("Invalid alias '%s'.", aliasMap.get(p.getCommand())));
				}
			}

			if(!availableCommands.containsKey(cmdList.get(i).getCommand())){
				if(cmdList.get(i).getCommand().toLowerCase().equals("exit")){
					throw new UserCanceledOperationException("Exit command called.");
				}
				throw new CommandException(String.format("Unknown command '%s'", cmdList.get(i).getCommand()));
			}
		}

		CommandOutput out = null;
		ShellExecOption prevExecOption = ShellExecOption.NONE;
		for(ParsedCommand cmd: cmdList){
			if(prevExecOption != ShellExecOption.PIPE){
				out = null;
			}

			out = executeCommand(outWriter, cmd, out, cmd.getExecOption() == ShellExecOption.PIPE);

			if(cmd.getExecOption() == ShellExecOption.REQUIRE_EXIT_0){
				if(out.getExitCode() != 0){
					throw new CommandException(String.format("Command '%s' has exit with code %s. Aborting...", cmd.getCommand(), out.getExitCode()));
				}
			}

			prevExecOption = cmd.getExecOption();
		}
	}

	/**
	 * Executes a parsed command
	 * @param cmd The parsed command
	 * @param pipedOut The output of the previous command
	 * @return The output of this command
	 * @throws CommandException
	 */
	private CommandOutput executeCommand(ShellOutputWriter outWriter, ParsedCommand cmd, CommandOutput pipedOut, boolean setPipeIndicator) throws CommandException {
		try {
			Constructor<?> constructor = availableCommands.get(cmd.getCommand()).getConstructor();
			ShellCommand sc = (ShellCommand) constructor.newInstance();
			CommandInput in = sc.parseArguments(instance, cmd.getArguments());

			if(pipedOut != null){
				in.setInputData(pipedOut.getOutputData());
			}

			if(setPipeIndicator){in.setPipedOutputInidcator();}

			CommandOutput out = sc.exec(outWriter, instance, in);
			outWriter.setColor(AnsiColor.RESET);
			return out;

		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException	| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new CommandException(e.getMessage());
		}
	}

	/**
	 * Parses a command line string to generate a list of {@link ParsedCommand}.
	 * Therefore the string is split by spaces that are not quoted or command separators like ';', '|' or '&&'.
	 * @param input The string which should be split
	 * @return The list of parsed commands
	 */
	protected static List<ParsedCommand> parseCommandLineString(String input){
		if(input.equals("")){return new ArrayList<ParsedCommand>(0);}
		CommandBuilder cb = new CommandBuilder();

		/* The following code is based on an example written by StackOverflow (stackoverflow.com) user Jan Goyvaerts and is licensed
		 * under CC BY-SA 3.0 "Creative Commons Attribution-ShareAlike 3.0 Unported", http://creativecommons.org/licenses/by-sa/3.0/)
		 *
		 * Source: http://stackoverflow.com/questions/366202/regex-for-splitting-a-string-using-space-when-not-surrounded-by-single-or-double
		 * The code has been modified.
		 */
		Pattern regex = Pattern.compile("[^\\s\"';&\\|]+|\"([^\"]*)\"|'([^']*)'|(;)|(\\&\\&)|(\\|)");
		//Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
		Matcher regexMatcher = regex.matcher(escape(input));
		search:
		while (regexMatcher.find()) {
			for(int i = 1; i <= regexMatcher.groupCount(); i++){
				if(regexMatcher.group(i) != null) {
					// Add double-quoted string without the quotes
					cb.addCommandPart(unescape(regexMatcher.group(i)));
					continue search;
				}
			}

			cb.addCommandPart(unescape(regexMatcher.group()));
		}
		return cb.getList();
	}

	private static String escape(String str){
		return str.replaceAll("#", "#U+0023").replaceAll("\\\\\\\\", "#U+005C").replaceAll("\\\\\"", "#U+0022").replaceAll("\\\\'", "#U+0027");
	}

	private static String unescape(String str){
		return str.replaceAll("#U\\+005C", "\\\\").replaceAll("#U\\+0022", "\"").replaceAll("#U\\+0027", "'").replaceAll("#U\\+0023", "#");
	}
}
