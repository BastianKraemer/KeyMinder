package de.akubix.keyminder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.Launcher;
import de.akubix.keyminder.core.db.StandardTree;
import de.akubix.keyminder.core.db.Tree;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.encryption.EncryptionManager;
import de.akubix.keyminder.lib.gui.ImageSelector;

public class TreeTest {

	@Test
	public void test() {
		
		
		// Prepare environment
		final String os = System.getProperty("os.name").toLowerCase();
		if(os.indexOf("linux") >= 0){Launcher.environment.put("os", "Linux"); Launcher.environment_isLinux = true;}
		else if(os.indexOf("win") >= 0){Launcher.environment.put("os", "Windows");}
		else {Launcher.environment.put("os", "Unknown");}

		Launcher.environment.put("cmd.settingsfile", "./zpm_settings.xml");
		
		Launcher.verbose_mode = false;
		
		// Initialize EncryptionManager
		EncryptionManager.loadDefaultCiphers();

		// Initialize application core (but not "startup" it)
		ApplicationInstance app = new ApplicationInstance();

		// Provide all icons by keywords
		ImageSelector.buildIndex();

		app.startup();
	
		runSimpleTreeTest(app.getTree());

	}

	private void runSimpleTreeTest(Tree tree)
	{
		StandardTree defaultTree = (StandardTree) tree;
		defaultTree.enableEventFireing(true);
		defaultTree.enableNodeTimestamps(true);
		defaultTree.undoManager.setEnable(true);
		
		tree.removeAllChildNodes(tree.getRootNode());
		
		final String nodePrefix = "RootNode_";
		final int number_of_nodes = 10;
		// Create some root nodes
		for(int i = 0; i < number_of_nodes; i++){
			TreeNode n = tree.createNode(nodePrefix + i);
			assertTrue("Adding Node #" + i, tree.addNode(n, tree.getRootNode()));
		}
		
		assertEquals(number_of_nodes + 1, tree.getRootNode().getChildNodes().length);

		tree.removeNode(tree.getRootNode().getChildNodeByIndex(0));
		assertEquals(number_of_nodes, tree.getRootNode().getChildNodes().length);
		
		// Test Add...
		for(int i = 0; i < number_of_nodes; i++){
			assertTrue("Verifying RootNodes", tree.getRootNode().getChildNodeByIndex(i).getText().equals(nodePrefix + i));
		}
		
		// Test Undo
		assertTrue("Undoing last action", tree.undo());
		
		assertEquals(number_of_nodes + 1, tree.getRootNode().getChildNodes().length);
		
		TreeNode x = tree.getRootNode().getChildNodeByIndex(1);
		final String nodeName = "HelloWorld";
		TreeNode newNode = tree.createNode(nodeName);
		assertTrue("Adding another node...", tree.addNode(newNode, x));
		
		// Testing getNodeByPath()
		TreeNode myNode = tree.getNodeByPath("/" + x.getText() + "/" + newNode.getText());
		
		assertEquals("Testing method 'getNodeByPath()'", newNode.getId(), myNode.getId());
		
		assertEquals(number_of_nodes + 3, defaultTree.countAllNodes()); // +3 because the root node has to be included
		
		int indexOfNodeX = x.getIndex();
		int idOfNodeX = x.getId();
		tree.removeNode(x.getId());
		
		if(tree.getRootNode().getChildNodeByIndex(indexOfNodeX).getId() == idOfNodeX){
			fail("Method 'removeNode()' doesn't work.");
		}
	}
	
}
