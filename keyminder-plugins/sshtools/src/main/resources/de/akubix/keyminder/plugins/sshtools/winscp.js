function runWinSCP(commandLine){
	//The result will look like this "sftp://[${myuser}[:${mypw}]@]${ssh_host}[:${ssh_port}]"

	var str = "sftp://";

	if(hasUsername()){
		str += getUsername();
		if(hasPassword()){
			str += ":" + getPassword();
		}
		str += "@";
	}

	str += lookup("ssh_host");
	if(hasValue("ssh_port")){
		str += ":" + lookup("ssh_port");
	}

	commandLine.addOption(str);
	return commandLine;
}
