function runPutty(commandLine, loadPuttySession){
	commandLine.addOption("-ssh");

	if(lookup("ssh_x11") == "true"){
		commandLine.addOption("-X");
	}

	if(loadPuttySession && hasValue("putty_sessionname")){
		commandLine.addOptions("-load", lookup("putty_sessionname"));
	}

	if(hasUsername()){
		commandLine.addOptions("-l", getUsername());
	}

	commandLine.addOption(lookup("ssh_host"));
	commandLine.addOptions("-P", lookup("ssh_port"));

	if(isDefined("ssh_portforwarding")){
		var portForwardings = lookup("ssh_portforwarding").split("\n");
		for(var i = 0; i < portForwardings.length; i++){
			commandLine.addOptions("-L", portForwardings[i]);
		}
	}

	if(hasUsername() && hasPassword()){
		commandLine.addOptions("-pw", getPassword());
	}

	return commandLine;
}

function runSocksProfile(commandLine){
		/* 
		 * This profile will be started without an assigned tree node. The following variables will be available
		 * ${socks_ssh_host}, ${socks_ssh_port}, ${socks_ssh_user}, ${socks_ssh_password}, ${socks_proxyport}
		 */
				
		commandLine.addOption("-ssh");
		commandLine.addOption("-N");
		commandLine.addOptions("-D", lookup("socks_proxyport"));

		if(hasValue("socks_ssh_port")){
			commandLine.addOptions("-P", lookup("socks_ssh_port"));
		}

		if(hasValue("socks_ssh_user")){
			var sshSocksPw = lookup("socks_ssh_password");
			if(sshSocksPw != "." && sshSocksPw != ""){
				commandLine.addOptions("-pw", lookup("socks_ssh_password"));
			}
			
			commandLine.addOptions("-l", lookup("socks_ssh_user"));
		}

		commandLine.addOption(lookup("socks_ssh_host"));
		return commandLine;
}

