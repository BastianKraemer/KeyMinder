package de.akubix.keyminder.modules.sshtools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.KeyMinder;
import de.akubix.keyminder.core.KeyMinderInstanceBuilder;
import de.akubix.keyminder.core.db.StandardTree;
import de.akubix.keyminder.core.db.Tree;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.io.XML;

public class CommandLineGeneratorTest {

	private static final String DEFAULT_PROFILE = "default";
	private static final String USING_SOCKS_PROFILE = "using_socks";

	private static Supplier<InputStream> puttyCmdDescriptor = () -> CommandLineGeneratorTest.class.getResourceAsStream("/de/akubix/keyminder/modules/sshtools/putty.xml");
	private static Supplier<InputStream>  winscpCmdDescriptor = () -> CommandLineGeneratorTest.class.getResourceAsStream("/de/akubix/keyminder/modules/sshtools/winscp.xml");

	private ApplicationInstance app;
	private Tree tree;

	@Before
	public void prepare(){
		KeyMinder.setVerboseMode(false);
		app = KeyMinderInstanceBuilder.getNewInstance();
		tree = new StandardTree(app);
	}

	@Test
	public void testPuttyProfile() {

		Map<String, String> var = new HashMap<>();
		TreeNode node = tree.createNode("Test");
		node.setAttribute("ssh_host", "localhost");

		// Not enough data, should lead to an IllegalArgumentException.
		try{
			runParser(var, node, puttyCmdDescriptor, DEFAULT_PROFILE);
			fail("Expected IllegalArgumentException did not occur.");
		}
		catch(IllegalArgumentException e){	}

		app.setSettingsValue("sshtools.puttypath", "/var/tools/putty");

		try{
			runParser(var, node, puttyCmdDescriptor, DEFAULT_PROFILE);
			fail("Expected IllegalArgumentException did not occur.");
		}
		catch(IllegalArgumentException e){	}

		var.put("sshtools.defaultuser", "defaultuser");

		node.setAttribute("ssh_port", "22");
		node.setAttribute("ssh_user", "root");
		node.setAttribute("ssh_password", "mypassword");

		assertEquals("[/var/tools/putty, -ssh, -l, root, localhost, -P, 22, -pw, mypassword]", runParser(var, node, puttyCmdDescriptor, DEFAULT_PROFILE).toString());

		node.removeAttribute("ssh_password");
		assertEquals("[/var/tools/putty, -ssh, -l, root, localhost, -P, 22]", runParser(var, node, puttyCmdDescriptor, DEFAULT_PROFILE).toString());

		var.put("sshtools.defaultpassword", "defaultpassword");
		assertEquals("[/var/tools/putty, -ssh, -l, root, localhost, -P, 22, -pw, defaultpassword]", runParser(var, node, puttyCmdDescriptor, DEFAULT_PROFILE).toString());

		node.removeAttribute("ssh_user");
		assertEquals("[/var/tools/putty, -ssh, -l, defaultuser, localhost, -P, 22, -pw, defaultpassword]", runParser(var, node, puttyCmdDescriptor, DEFAULT_PROFILE).toString());

		node.setAttribute("putty_sessionname", "my_socks_profile");
		assertEquals("[/var/tools/putty, -ssh, -load, my_socks_profile, -l, defaultuser, localhost, -P, 22, -pw, defaultpassword]", runParser(var, node, puttyCmdDescriptor, USING_SOCKS_PROFILE).toString());

		node.setAttribute("ssh_user", "root");
		node.setAttribute("ssh_password", "mypassword");

		node.setAttribute("ssh_portforwarding", "param1\nb=param2\nparam3");
		assertEquals("[/var/tools/putty, -ssh, -load, my_socks_profile, -l, root, localhost, -P, 22, -L, param1, -L, b=param2, -L, param3, -pw, mypassword]", runParser(var, node, puttyCmdDescriptor, USING_SOCKS_PROFILE).toString());
	}

	@Test
	public void testWinScpProfile(){

		app.setSettingsValue("sshtools.winscppath", "WinSCP.exe");

		Map<String, String> var = new HashMap<>();
		TreeNode node = tree.createNode("Test");
		node.setAttribute("ssh_host", "localhost");

		assertEquals("[WinSCP.exe, sftp://localhost]", runParser(var, node, winscpCmdDescriptor, DEFAULT_PROFILE).toString());

		node.setAttribute("ssh_port", "22");

		assertEquals("[WinSCP.exe, sftp://localhost:22]", runParser(var, node, winscpCmdDescriptor, DEFAULT_PROFILE).toString());

		node.setAttribute("ssh_user", "root");

		assertEquals("[WinSCP.exe, sftp://root@localhost:22]", runParser(var, node, winscpCmdDescriptor, DEFAULT_PROFILE).toString());

		node.setAttribute("ssh_password", "mypassword");

		assertEquals("[WinSCP.exe, sftp://root:mypassword@localhost:22]", runParser(var, node, winscpCmdDescriptor, DEFAULT_PROFILE).toString());
	}

	private List<String> runParser(Map<String, String> var, TreeNode treeNode, Supplier<InputStream> cmdDescriptor, String profileName) throws IllegalArgumentException {
		try {
			CommandLineGenerator xapp = new CommandLineGenerator(app, XML.loadXmlDocument(cmdDescriptor.get()), var, CommandLineGenerator._DEFAULT_RESOURCE_CONTENT_LOADER);
			return xapp.generateCommandLineParameters(profileName, treeNode);
		} catch (SAXException | IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
			return null;
		}
	}
}
