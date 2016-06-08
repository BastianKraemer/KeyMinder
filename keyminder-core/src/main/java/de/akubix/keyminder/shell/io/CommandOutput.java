package de.akubix.keyminder.shell.io;

public class CommandOutput {
	private int exitCode;
	private Object outputData;

	public CommandOutput(int exitCode, Object outputData){
		this.exitCode = exitCode;
		this.outputData = outputData;
	}

	/**
	 * @return the exit code of the command
	 */
	public int getExitCode() {
		return this.exitCode;
	}

	/**
	 * @return the command output data
	 */
	public Object getOutputData() {
		return this.outputData;
	}

	public static CommandOutput success(){
		return new CommandOutput(0, null);
	}

	public static CommandOutput success(Object data){
		return new CommandOutput(0, data);
	}

	public static CommandOutput error(){
		return new CommandOutput(1, null);
	}

	public static CommandOutput error(Object data){
		return new CommandOutput(1, data);
	}
}
