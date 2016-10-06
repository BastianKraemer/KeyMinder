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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.KeyMinder;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.shell.annotations.Alias;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Example;
import de.akubix.keyminder.shell.annotations.Note;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.annotations.PipeInfo;
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

	private static final Pattern COMMAND_PATTERN = Pattern.compile("^[a-z0-9-_]+$");
	private static final Pattern ALIAS_PATTERN = Pattern.compile("^([a-z0-9-_]+) *= *(.+)$");

	private final ApplicationInstance instance;

	private final Map<String, Class<? extends ShellCommand>> availableCommands = new HashMap<>();
	private final Map<String, String> aliasMap = new HashMap<>();
	private final Map<String, String> runtimeVariables = new HashMap<>();

	/**
	 * Creates a new {@link Shell} instance
	 * @param instance The application instance that is bound to the shell
	 */
	public Shell(ApplicationInstance instance){
		this.instance = instance;
	}

	/**
	 * Adds a command to the shell
	 * @param name The name of the command
	 * @param packageAndClassName the complete package and class name
	 */
	@SuppressWarnings("unchecked")
	public boolean addCommand(String packageAndClassName){

		try {
			assert !packageAndClassName.equals("") : "Empty class name";

			final Class<?> loadedClass = this.getClass().getClassLoader().loadClass(packageAndClassName);
			assert ShellCommand.class.isAssignableFrom(loadedClass) : String.format("Command does not implement interface '%s'.", ShellCommand.class.getName());

			final Command commandAnnotation = loadedClass.getAnnotation(Command.class);
			assert commandAnnotation != null : String.format("Annotation '%s' is missing.", Command.class.getSimpleName());

			final String cmdName = commandAnnotation.value();
			assert COMMAND_PATTERN.matcher(cmdName).matches() : String.format("Command name '%s' is not valid.", cmdName);
			assert !availableCommands.containsKey(cmdName) : String.format("Duplicate entry for command name '%s'.", cmdName);

			availableCommands.put(cmdName, (Class<? extends ShellCommand>) loadedClass);

			Alias aliasAnnotation = loadedClass.getAnnotation(Alias.class);
			if(aliasAnnotation != null){
				for(String aliasDefinition: aliasAnnotation.value()){
					loadAliasByDefinition(aliasDefinition);
				}
			}

			return true;

		} catch (ClassNotFoundException e){
			instance.println(String.format("Cannot load shell command class '%s': Class not found.", packageAndClassName));
		}
		catch(AssertionError e) {
			instance.println(String.format("Cannot load shell command class '%s': %s", packageAndClassName, e.getMessage()));
		}

		return false;
	}

	public void loadAliasByDefinition(String aliasDefinition){

		Matcher m = ALIAS_PATTERN.matcher(aliasDefinition);
		if(m.matches()){
			addAlias(m.group(1), m.group(2));
		}
		else{
			instance.println(String.format("Invalid alias definition: '%s'", aliasDefinition));
		}
	}

	/**
	 * Loads a command list from a file (listed in a file with one line per class)
	 * @param resourcePath the path to the file
	 */
	public void loadCommandsFromFile(String resourcePath){

		try {
			try(BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(resourcePath)))){
				br.lines().map((str) -> str.trim())
						.filter((str) -> !str.equals(""))
						.forEach((line) -> addCommand(line));
			}
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
	 * @param alias the alias
	 * @param value the command that should be executed instead
	 */
	public void addAlias(String alias, String value){
		aliasMap.put(alias, value);
	}

	public void setRuntimeVariable(String key, String value){
		runtimeVariables.put(key, value);
	}

	public void removeRuntimeVariable(String key){
		runtimeVariables.remove(key);
	}

	public Set<String> getCommandSet(){
		return this.availableCommands.keySet();
	}

	public Set<Entry<String, String>> getAliasSet(){
		return this.aliasMap.entrySet();
	}

	/**
	 * Generates a manual for a command based on the command annotations
	 * @param cmd The name of the command
	 * @return String the manual
	 * @throws CommandException if the command does not exist
	 */
	public String getManual(String cmdName) throws CommandException {

		if(availableCommands.containsKey(cmdName.toLowerCase())){

			final StringBuilder manualStr = new StringBuilder();

			Class<?> cmdClass = availableCommands.get(cmdName.toLowerCase());

			final Description desc = cmdClass.getAnnotation(Description.class);
			final Operands operands =  cmdClass.getAnnotation(Operands.class);
			final Option[] optionList = cmdClass.getAnnotationsByType(Option.class);

			if(desc != null){
				manualStr.append(desc.value()).append("\n\n");
			}

			manualStr.append("USAGE: ").append(cmdName).append(" ");

			if(operands != null){
				boolean skipOptionsText = false;
				if(!operands.description().equals("")){
					manualStr.append(operands.description());
					skipOptionsText = operands.description().contains("\n");
				}
				else{
					for(int i = 0; i < operands.cnt(); i++){
						if(i == operands.nodeArgAt()){
							if(operands.optionalNodeArg()){
								manualStr.append(" {tree node}");
							}
							else{
								manualStr.append(" [tree node]");
							}
						}
						else{
							manualStr.append(" [argument ").append(i).append("]");
						}
					}
				}

				if(optionList.length > 0 && !skipOptionsText){
					manualStr.append(" ...OPTIONS");
				}
			}
			else if(optionList.length > 0){
				manualStr.append("{OPTIONS}");
			}

			if(optionList.length > 0){
				for(Option option: optionList){
					manualStr.append("\n\n ").append(option.name());

					for(String alias: option.alias()){
						manualStr.append(", ").append(alias);
					}

					if(!option.description().equals("")){
						manualStr.append("\n    ").append(option.description());
					}
				}
			}

			final PipeInfo pipeInfo = cmdClass.getAnnotation(PipeInfo.class);
			if(pipeInfo != null){
				manualStr.append("\n\n\nINPUT TYPES:\n  ")
						.append(pipeInfo.in().equals("") ? "-" : pipeInfo.in())
						.append("\n\nOUTPUT TYPES:\n  ")
						.append(pipeInfo.out().equals("") ? "-" : pipeInfo.out());
			}

			final Example examples = cmdClass.getAnnotation(Example.class);
			if(examples != null){
				manualStr.append("\n\n\nEXAMPLES:");
				for(String example: examples.value()){
					manualStr.append("\n  ").append(example.replace("\n", "\n  ")).append("\n");
				}
			}

			final Note note = cmdClass.getAnnotation(Note.class);
			if(note != null){
				manualStr.append("\n").append(note.value());
			}

			return manualStr.toString();
		}
		else if(aliasMap.containsKey(cmdName)){
			return String.format("'%s' is an alias for '%s'", cmdName, aliasMap.get(cmdName));
		}
		else{
			throw new CommandException(String.format("Unknown command '%s'", cmdName));
		}
	}

	/**
	 * Executes a command string
	 * @param commandLineInput The command string
	 * @throws CommandException if something went wrong (for example an invalid user input)
	 * @throws UserCanceledOperationException when the user entered the 'exit' command
	 */
	public void runShellCommand(String commandLineInput) throws CommandException, UserCanceledOperationException {
		runShellCommand(this.instance, commandLineInput);
	}

	/**
	 * Checks whether a command exists or not
	 * @param commandName the comamnd name
	 * @return {@code true} if the command exists, {@code false} if not
	 */
	public boolean commandExists(String commandName){
		return availableCommands.containsKey(commandName);
	}

	/**
	 * Executes a command string
	 * @param outWriter The output writer
	 * @param commandLineInput The command string
	 * @throws CommandException if something went wrong (for example an invalid user input)
	 * @throws UserCanceledOperationException when the user entered the 'exit' command
	 */
	public void runShellCommand(ShellOutputWriter outWriter, String commandLineInput) throws CommandException, UserCanceledOperationException {

		final TreeNode selectedNode = instance.getTree().getSelectedNode();

		List<ParsedCommand> cmdList = parseCommandLineString(
			replaceVariables(
				commandLineInput.trim(),
				(var) -> instance.lookup(var, selectedNode.getId() != 0 ? selectedNode : null, runtimeVariables)));

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
	 * Therefore the string is split by spaces that are not quoted or command separators like {@code ';'}, {@code '|'} or {@code '&&'}.
	 * @param input The string which should be split
	 * @return The list of parsed commands
	 */
	protected static List<ParsedCommand> parseCommandLineString(String input){
		if(input.equals("")){return new ArrayList<>(0);}

		CommandBuilder cb = new CommandBuilder();

		/* The following code is based on an example written by StackOverflow (stackoverflow.com) user Jan Goyvaerts and is licensed
		 * under CC BY-SA 3.0 "Creative Commons Attribution-ShareAlike 3.0 Unported", http://creativecommons.org/licenses/by-sa/3.0/)
		 *
		 * Source: http://stackoverflow.com/questions/366202/regex-for-splitting-a-string-using-space-when-not-surrounded-by-single-or-double
		 * The code has been modified.
		 */
		Pattern regex = Pattern.compile("[^\\s\"';&\\|]+|\"([^\"]*)\"|'([^']*)'|(;)|(\\&\\&)|(\\|)");
		Matcher regexMatcher = regex.matcher(escape(input.replace("\\$", "$")));
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

	/**
	 * Replaces all Variables (${variable}) in a string
	 * @param source The String the variables should be replaced in
	 * @return The String with all replaced variables (if possible)
	 */
	public static String replaceVariables(String source, Function<String, String> lookupFunction){
		// Regular expression to allow all characters in variables except "$"
		//	-> \\$\\{[^\\$]+\\}
		// Regular expression to allow only A-Z, a-z and 0-9 for variable names
		// 	-> \\$\\{[a-zA-Z0-9]*\\}

		for(MatchResult match : allMatches(Pattern.compile("[^\\\\]?(\\$\\{([^\\$]+)\\})"), source)){
			String replacement;
			try{
				replacement = lookupFunction.apply(match.group(2));
			}
			catch(IllegalArgumentException e){
				replacement = "";
			}

			source = source.replace(match.group(1), replacement);
		}
		return source;
	}

	/* The following code was written by StackOverflow (stackoverflow.com) user Mike Samuel and is licensed under CC BY-SA 3.0
	 * "Creative Commons Attribution-ShareAlike 3.0 Unported", http://creativecommons.org/licenses/by-sa/3.0/)
	 *
	 * Source: http://stackoverflow.com/questions/6020384/create-array-of-regex-matches
	 * The code has not been modified.
	 */
	private static Iterable<MatchResult> allMatches(final Pattern p, final CharSequence input){
		return new Iterable<MatchResult>() {
			@Override
			public Iterator<MatchResult> iterator() {
				return new Iterator<MatchResult>()
				{
					// Use a matcher internally.
					final Matcher matcher = p.matcher(input);
					// Keep a match around that supports any interleaving of hasNext/next calls.
					MatchResult pending;

					@Override
					public boolean hasNext() {
					  // Lazily fill pending, and avoid calling find() multiple times if the
					  // clients call hasNext() repeatedly before sampling via next().
					  if (pending == null && matcher.find()) {
					    pending = matcher.toMatchResult();
					  }
					  return pending != null;
					}

					@Override
					public MatchResult next() {
					  // Fill pending if necessary (as when clients call next() without
					  // checking hasNext()), throw if not possible.
						if (!hasNext()) { throw new NoSuchElementException(); }
						// Consume pending so next call to hasNext() does a find().
						MatchResult next = pending;
						pending = null;
						return next;
					}

					/** Required to satisfy the interface, but unsupported. */
					@Override
					public void remove() { throw new UnsupportedOperationException(); }
				};
			}
		};
	}
}
