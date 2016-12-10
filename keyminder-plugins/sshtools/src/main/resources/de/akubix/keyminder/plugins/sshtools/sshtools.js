// Some default functions for every application profile

function hasUsername(){
	var user = lookup("ssh_user") 
	
	if(user == "."){
		// login without username
		return false;
	}

	if(user == "" && lookup("sshtools.defaultuser") == ""){
		// No username and no default username
		return false
	}
	return true;
}

function getUsername(){
	var user = lookup("ssh_user") 
	return user == "" ? lookup("sshtools.defaultuser") : user;
}

function hasPassword(){
	var pw = lookup("ssh_password");
	if(pw == "."){
		// use no password
		return false;
	}

	if(pw == "" && lookup("sshtools.defaultpassword") == ""){
		// No password an no default password
		return false
	}
	return true;
}

function getPassword(){
	var pw = lookup("ssh_password");
	return pw == "" ? lookup("sshtools.defaultpassword") : pw;
}

function hasValue(varName){
	return lookup(varName) != "";
}
