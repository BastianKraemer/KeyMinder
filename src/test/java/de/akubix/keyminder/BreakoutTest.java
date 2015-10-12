package de.akubix.keyminder;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.Breakout;
import de.akubix.keyminder.core.Launcher;
import de.akubix.keyminder.core.db.StandardTree;
import de.akubix.keyminder.core.db.TreeNode;

public class BreakoutTest {

	@Test
	public void test() {
	 	final String txt = 	"<ApplicationConfiguration name=\"Putty\" socks=\"using_socks\" version=\"1.0\">" +
	 						"	<Configuration id=\"default\" execute=\"${sshtools.puttypath}\" require=\"${sshtools.puttypath}; ${ssh_host}; ${ssh_port}\" version=\"1.0\">" +
				 			"		<var name=\"myuser\" defaultValue=\"${ssh_user}\" alternative=\"${sshtools.defaultuser}\" ignoreOn=\".\" />" +
				 			"		<var name=\"mypw\" if=\"${myuser}\" defaultValue=\"${ssh_password}\" alternative=\"${sshtools.defaultpassword}\" ignoreOn=\".\"/>" +
				 			"		<option>-ssh</option>" +
				 			"		<option if=\"${ssh_x11}\">-X</option>" +
				 			"		<option if=\"${socks_profileID}\">-load</option>" +
				 			"		<option if=\"${socks_profileID}\">${socks_profileID}</option>" +
				 			"		<option if=\"${myuser}\">-l</option>" +
				 			"		<option if=\"${myuser}\">${myuser}</option>" +
				 			"		<option>${ssh_host}</option>" +
				 			"		<option>-P</option>" +
				 			"		<option>${ssh_port}</option>" +
				 			"		<option if=\"${ssh_portforwarding}\" parameterSeperator=\"\\n\">${ssh_portforwarding}</option>" +
				 			"		<option if=\"${mypw}\">-pw</option>" +
				 			"		<option if=\"${mypw}\">${mypw}</option>" +
				 			"	</Configuration>" +
				 			"</ApplicationConfiguration>";
		
	 	
	 	ApplicationInstance app = new ApplicationInstance();
	 	StandardTree tree = new StandardTree(app);
	 	Launcher.verbose_mode = false;
	 	TreeNode node = tree.createNode("Test");
	 	Map<String, String> var = new HashMap<>();

	 	node.setAttribute("ssh_host", "localhost");
	 	
	 	// Not enough data, should lead to a IllegalArgumentException.
	 	
	 	try{
	 		Breakout.generateParameterListForProfile(app, txt, "default", node, var);
	 		fail("Breakout doesn't throw an exception, although it has not enugh data");
	 	}
	 	catch(IllegalArgumentException e){	}
	 	
	 	var.put("sshtools.puttypath", "/var/tools/putty");
	 	
	 	try{
	 		Breakout.generateParameterListForProfile(app, txt, "default", node, var);
	 		fail("Breakout doesn't throw an exception, although it has not enugh data");
	 	}
	 	catch(IllegalArgumentException e){	}
	 	
	 	var.put("sshtools.defaultuser", "defaultuser");
	 	
	 	node.setAttribute("ssh_port", "22");
	 	node.setAttribute("ssh_user", "root");
	 	node.setAttribute("ssh_password", "mypassword");
	 	
	 	assertEquals("", "[/var/tools/putty, -ssh, -l, root, localhost, -P, 22, -pw, mypassword]", Breakout.generateParameterListForProfile(app, txt, "default", node, var).toString());
	
	 	node.removeAttribute("ssh_password");
	 	assertEquals("", "[/var/tools/putty, -ssh, -l, root, localhost, -P, 22]", Breakout.generateParameterListForProfile(app, txt, "default", node, var).toString());

	 	var.put("sshtools.defaultpassword", "defaultpassword");
	 	assertEquals("", "[/var/tools/putty, -ssh, -l, root, localhost, -P, 22, -pw, defaultpassword]", Breakout.generateParameterListForProfile(app, txt, "default", node, var).toString());

	 	node.removeAttribute("ssh_user");
	 	assertEquals("", "[/var/tools/putty, -ssh, -l, defaultuser, localhost, -P, 22, -pw, defaultpassword]", Breakout.generateParameterListForProfile(app, txt, "default", node, var).toString());

	 	node.setAttribute("socks_profileID", "my_scocks_profile");
		assertEquals("", "[/var/tools/putty, -ssh, -load, my_scocks_profile, -l, defaultuser, localhost, -P, 22, -pw, defaultpassword]", Breakout.generateParameterListForProfile(app, txt, "default", node, var).toString());

	 	node.setAttribute("ssh_user", "root");
	 	node.setAttribute("ssh_password", "mypassword");
	 	
		node.setAttribute("ssh_portforwarding", "-a param1\n--bparam2\n-c param3");	
		assertEquals("", "[/var/tools/putty, -ssh, -load, my_scocks_profile, -l, root, localhost, -P, 22, -a param1, --bparam2, -c param3, -pw, mypassword]", Breakout.generateParameterListForProfile(app, txt, "default", node, var).toString());
	}
}
