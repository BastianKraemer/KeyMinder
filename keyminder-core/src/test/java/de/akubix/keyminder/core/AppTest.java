package de.akubix.keyminder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.junit.After;
import org.junit.Test;

import de.akubix.keyminder.core.db.Tree;
import de.akubix.keyminder.core.interfaces.events.EventTypes.DefaultEvent;
import de.akubix.keyminder.core.interfaces.events.EventTypes.TreeNodeEvent;

public class AppTest {

	private boolean closedEventOccured = false;
	private boolean event_selectednode_changed = false;
	private boolean event_node_added = false;
	private boolean event_node_removed = false;
	private boolean event_node_edited = false;

	private File autoGeneratedTestFile = null;

	@Test
	public void test() {

		// Prepare environment
		final String os = System.getProperty("os.name").toLowerCase();
		if(os.indexOf("linux") >= 0){KeyMinder.environment.put("os", "Linux"); KeyMinder.environment_isLinux = true;}
		else if(os.indexOf("win") >= 0){KeyMinder.environment.put("os", "Windows");}
		else {KeyMinder.environment.put("os", "Unknown");}

		KeyMinder.environment.put("cmd.settingsfile", "./keyminder_settings.xml");

		// Initialize EncryptionManager
		de.akubix.keyminder.core.encryption.EncryptionManager.loadDefaultCiphers();

		// Initialize application core (but not "startup" it)
		ApplicationInstance app = new ApplicationInstance();

		app.startup(false);

		final String testFileName = "keyminder_testfile.xml";

		URL url = this.getClass().getResource("/de/akubix/keyminder/" + testFileName);
		assertTrue("Open a XML File", app.openFile(new File(url.getFile())));

		assertTrue("File successfully opened", app.currentFile != null);

		// Move to node example@somedomain.com

		Tree tree = app.getTree();

		app.addEventHandler(TreeNodeEvent.OnNodeAdded, (node) -> {event_node_added = true;});
		app.addEventHandler(TreeNodeEvent.OnNodeEdited, (node) -> {event_node_edited = true;});
		app.addEventHandler(TreeNodeEvent.OnNodeRemoved, (node) -> {event_node_removed = true;});
		app.addEventHandler(TreeNodeEvent.OnSelectedItemChanged, (node) -> {event_selectednode_changed = true;});
		// Testing some commands

		app.execute("cdi", "0");

		assertEquals("E-Mail", tree.getSelectedNode().getText());

		app.execute("cdi", "0");

		assertEquals("example@somedomain.com", tree.getSelectedNode().getText());
		assertTrue("Check event 'onSelectedNodeChanged'", event_selectednode_changed);

		// Test if attributes has been successfully restored
		assertEquals("Verify that all attributes has been restored", "example", tree.getSelectedNode().getAttribute("username"));
		assertEquals("Verify that all attributes has been restored", "secret_password", tree.getSelectedNode().getAttribute("password"));

		// Check "NextNode" feature

		tree.setSelectedNode(tree.getNextNode(tree.getSelectedNode()));
		assertEquals("Check method 'getNextNode()'", "example@anotherdomain.org", tree.getSelectedNode().getText());
		assertEquals("Verify that all attributes has been restored", "example", tree.getSelectedNode().getAttribute("username"));
		assertEquals("Verify that all attributes has been restored", "other_secret_password", tree.getSelectedNode().getAttribute("password"));

		// Test "copy" command

		int prevNumberOfRootNodes = tree.getRootNode().countChildNodes();

		app.execute("cp", ".", "/");

		assertEquals(prevNumberOfRootNodes + 1, tree.getRootNode().countChildNodes());
		assertEquals(tree.getRootNode().getChildNodeByIndex(prevNumberOfRootNodes).getText(), tree.getSelectedNode().getText());

		// Test "rm"

		app.execute("rm", "/" +  tree.getSelectedNode().getText());
		assertEquals(prevNumberOfRootNodes, tree.getRootNode().countChildNodes());
		assertTrue("Check event 'onNodeRemoved", event_node_removed);

		// Test "mv"

		int currentSelectedNodeId = tree.getSelectedNode().getId();
		app.execute("mv", ".", "/");

		assertEquals(prevNumberOfRootNodes + 1, tree.getRootNode().countChildNodes());
		assertNotEquals(currentSelectedNodeId, tree.getSelectedNode().getId());
		tree.setSelectedNode(tree.getRootNode().getChildNodeByIndex(prevNumberOfRootNodes));
		assertEquals("Test of command 'move''", "example@anotherdomain.org", tree.getSelectedNode().getText());
		assertEquals("Test of command 'move'", "example", tree.getSelectedNode().getAttribute("username"));
		assertEquals("Test of command 'move'", "other_secret_password", tree.getSelectedNode().getAttribute("password"));

		// Test "set"
		final String newNodeText = "Hello World";
		final String prevText = tree.getSelectedNode().getText();
		app.execute("set", "text", newNodeText);
		assertEquals(newNodeText, tree.getSelectedNode().getText());

		assertTrue("Check event 'onNodeEdited", event_node_edited);

		assertTrue(tree.undo());
		assertEquals(prevText, tree.getSelectedNode().getText());

		// Test "Add"
		int numberOfChildNodes = tree.getSelectedNode().countChildNodes();
		app.execute("add", newNodeText);
		assertEquals(numberOfChildNodes + 1, tree.getSelectedNode().countChildNodes());
		assertEquals(newNodeText, tree.getSelectedNode().getChildNodeByIndex(0).getText());

		assertTrue("Check event 'onNodeAdded", event_node_added);

		app.addEventHandler(DefaultEvent.OnFileClosed, () -> {closedEventOccured = true;});

		tree.setTreeChangedStatus(false);

		app.execute("file", "close");

		assertTrue("File closed event did not occur", closedEventOccured);
		assertTrue("File successfully closed.", app.currentFile == null);

		// ----------------------------------------------

		autoGeneratedTestFile = new File(url.getFile().replace(testFileName, "keyminder_file_creation_test.xml"));

		app.createNewFile(autoGeneratedTestFile, false);
		app.execute("add", "Hello World");
		tree.setSelectedNode(tree.getRootNode().getChildNodeByIndex(0).getChildNodeByIndex(0));
		app.execute("set", "hello", "world");

		assertTrue(tree.treeHasBeenUpdated());
		assertTrue("Save file", app.saveFile());
		assertTrue(app.closeFile());

		assertTrue("File successfully closed.", app.currentFile == null);

		assertTrue("Re-Open file", app.openFile(autoGeneratedTestFile));

		assertEquals(1, tree.getRootNode().countChildNodes());
		assertEquals(1, tree.getRootNode().getChildNodeByIndex(0).countChildNodes());

		tree.setSelectedNode(tree.getRootNode().getChildNodeByIndex(0).getChildNodeByIndex(0));

		assertEquals("Hello World", tree.getSelectedNode().getText());

		assertEquals("world", tree.getSelectedNode().getAttribute("hello"));

		assertTrue(app.closeFile());
	}

	@After
	public void after() {
		// Cleanup
		if(autoGeneratedTestFile != null){
			if(autoGeneratedTestFile.exists()){
				if(!autoGeneratedTestFile.delete()){
					System.out.println(String.format("WARNING: Cannot delete file '%s'\n",  autoGeneratedTestFile.getAbsolutePath()));
				}
			}
		}
	}

}