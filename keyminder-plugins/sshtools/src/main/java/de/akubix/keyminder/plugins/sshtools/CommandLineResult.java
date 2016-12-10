package de.akubix.keyminder.plugins.sshtools;

import java.util.ArrayList;
import java.util.List;

public class CommandLineResult {
	private String failed = null;
	private List<String> argumentList = new ArrayList<>();
	CommandLineResult(String executable){
		argumentList.add(executable);
	}

	public void addOption(String argument){
		argumentList.add(argument);
	}

	public void addOptions(String... arguments){
		for(String arg: arguments){
			argumentList.add(arg);
		}
	}

	public void setFailed(String reason){
		this.failed = reason;
	}

	public boolean hasFailed(){
		return this.failed != null;
	}

	public String getFailReason(){
		return this.failed;
	}

	public List<String> getArguments(){
		return argumentList;
	}
}
