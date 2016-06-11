package de.akubix.keyminder.modules.sshtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.KeyMinder;
import de.akubix.keyminder.core.KeyMinderInstanceBuilder;
import de.akubix.keyminder.core.db.StandardTree;
import de.akubix.keyminder.core.db.TreeNode;

public class XMLApplicationProfileParserTest {

	@Test
	public void test() {
		final String txt =	"<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" +
							"<ApplicationProfile name=\"PuTTY\" socks=\"using_socks\" icon=\"icon_bash\" version=\"2.0\">" +
							"	<Configuration id=\"default\" execute=\"${sshtools.puttypath}\" require=\"${sshtools.puttypath}; ${ssh_host}; ${ssh_port}\">" +
							"		<!-- Do not enter this section if ${ssh_user} == '.' -->" +
							"		<if not_equals=\"${ssh_user} == .\">" +
							"			<var name=\"myuser\">" +
							"				<if exists=\"${ssh_user}\" else=\"${sshtools.defaultuser}\">${ssh_user}</if>" +
							"			</var>" +
							"		</if>" +
							"		<!-- Do not enter this section if ${ssh_password} == '.' -->" +
							"		<if not_equals=\"${ssh_password} == .\">" +
							"			<if exists=\"${myuser}\">" +
							"				<var name=\"mypw\">" +
							"					<if exists=\"${ssh_password}\" else=\"${sshtools.defaultpassword}\">${ssh_password}</if>" +
							"				</var>" +
							"			</if>" +
							"		</if>" +
							"		<option>-ssh</option>" +
							"		<option if=\"${ssh_x11}\">-X</option>" +
							"		<if exists=\"${socks_profileID}\">" +
							"			<option>-load</option>" +
							"			<option>${socks_profileID}</option>" +
							"		</if>"+
							"		<option if=\"${myuser}\">-l</option>" +
							"		<option if=\"${myuser}\">${myuser}</option>" +
							"		<option>${ssh_host}</option>" +
							"		<option>-P</option>" +
							"		<option>${ssh_port}</option>" +
							"		<if exists=\"${ssh_portforwarding}\">" +
							"			<split src=\"${ssh_portforwarding}\" delimiter=\"\\n\" var=\"single_forward\">" +
							"				<option>${single_forward}</option>" +
							"			</split>" +
							"		</if>" +
							"		<option if=\"${mypw}\">-pw</option>" +
							"		<option if=\"${mypw}\">${mypw}</option>" +
							"	</Configuration>" +
							"</ApplicationProfile>";

		ApplicationInstance app = KeyMinderInstanceBuilder.getNewInstance();
		StandardTree tree = new StandardTree(app);
		KeyMinder.setVerboseMode(false);
		TreeNode node = tree.createNode("Test");
		Map<String, String> var = new HashMap<>();
		node.setAttribute("ssh_host", "localhost");

		// Not enough data, should lead to a IllegalArgumentException.

		try{
			runParser(app, txt, var, node);
			fail("Breakout doesn't throw an exception, although it has not enugh data");
		}
		catch(IllegalArgumentException e){	}

		app.setSettingsValue("sshtools.puttypath", "/var/tools/putty");

		try{
			runParser(app, txt, var, node);
			fail("Breakout doesn't throw an exception, although it has not enugh data");
		}
		catch(IllegalArgumentException e){	}

		var.put("sshtools.defaultuser", "defaultuser");

		node.setAttribute("ssh_port", "22");
		node.setAttribute("ssh_user", "root");
		node.setAttribute("ssh_password", "mypassword");

		assertEquals("", "[/var/tools/putty, -ssh, -l, root, localhost, -P, 22, -pw, mypassword]", runParser(app, txt, var, node).toString());

		node.removeAttribute("ssh_password");
		assertEquals("", "[/var/tools/putty, -ssh, -l, root, localhost, -P, 22]", runParser(app, txt, var, node).toString());

		var.put("sshtools.defaultpassword", "defaultpassword");
		assertEquals("", "[/var/tools/putty, -ssh, -l, root, localhost, -P, 22, -pw, defaultpassword]", runParser(app, txt, var, node).toString());

		node.removeAttribute("ssh_user");
		assertEquals("", "[/var/tools/putty, -ssh, -l, defaultuser, localhost, -P, 22, -pw, defaultpassword]", runParser(app, txt, var, node).toString());

		node.setAttribute("socks_profileID", "my_scocks_profile");
		assertEquals("", "[/var/tools/putty, -ssh, -load, my_scocks_profile, -l, defaultuser, localhost, -P, 22, -pw, defaultpassword]", runParser(app, txt, var, node).toString());

		node.setAttribute("ssh_user", "root");
		node.setAttribute("ssh_password", "mypassword");

		node.setAttribute("ssh_portforwarding", "-a param1\n--b=param2\n-c param3");
		assertEquals("", "[/var/tools/putty, -ssh, -load, my_scocks_profile, -l, root, localhost, -P, 22, -a param1, --b=param2, -c param3, -pw, mypassword]", runParser(app, txt, var, node).toString());
	}

	private List<String> runParser(ApplicationInstance app, String txt, Map<String, String> var, TreeNode node) throws IllegalArgumentException{
		XMLApplicationProfileParser xapp = XMLApplicationProfileParser.createInstance(app, txt, var);
		return xapp.generateCommandLineParameters("default", node);
	}
}
