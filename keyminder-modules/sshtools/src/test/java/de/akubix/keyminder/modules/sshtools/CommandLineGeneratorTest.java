package de.akubix.keyminder.modules.sshtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.KeyMinder;
import de.akubix.keyminder.core.KeyMinderInstanceBuilder;
import de.akubix.keyminder.core.db.StandardTree;
import de.akubix.keyminder.core.db.TreeNode;

public class CommandLineGeneratorTest {

	@Test
	public void test() throws IOException, URISyntaxException {

		String txt = new String(Files.readAllBytes(Paths.get(getClass().getResource("/de/akubix/keyminder/modules/sshtools/putty.xml").toURI())));

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

		node.setAttribute("putty_sessionname", "my_socks_profile");
		assertEquals("", "[/var/tools/putty, -ssh, -load, my_socks_profile, -l, defaultuser, localhost, -P, 22, -pw, defaultpassword]", runParser(app, txt, var, node).toString());

		node.setAttribute("ssh_user", "root");
		node.setAttribute("ssh_password", "mypassword");

		node.setAttribute("ssh_portforwarding", "param1\nb=param2\nparam3");
		assertEquals("", "[/var/tools/putty, -ssh, -load, my_socks_profile, -l, root, localhost, -P, 22, -L, param1, -L, b=param2, -L, param3, -pw, mypassword]", runParser(app, txt, var, node).toString());
	}

	private List<String> runParser(ApplicationInstance app, String txt, Map<String, String> var, TreeNode node) throws IllegalArgumentException{
		CommandLineGenerator xapp = CommandLineGenerator.createInstance(app, txt, var);
		return xapp.generateCommandLineParameters("using_socks", node);
	}
}
