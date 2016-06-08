package de.akubix.keyminder.shell.parse;

import java.util.List;

public class ParsedCommand {
	private final String command;
	private final List<String> arguments;
	private final ShellExecOption execOption;
	private final String unparsedArgs;
	public ParsedCommand(String cmd, List<String> args, ShellExecOption execOption, String unparsedArgs){
		this.command = cmd;
		this.arguments = args;
		this.execOption = execOption;
		this.unparsedArgs = unparsedArgs;
	}
	/**
	 * @return the arguments
	 */
	public List<String> getArguments() {
		return arguments;
	}
	/**
	 * @return the command
	 */
	public String getCommand() {
		return command;
	}
	/**
	 * @return the execution option
	 */
	public ShellExecOption getExecOption() {
		return execOption;
	}

	/**
	 * @return the unparsed command
	 */
	public String getUnparsedArgs() {
		return unparsedArgs;
	}
}
