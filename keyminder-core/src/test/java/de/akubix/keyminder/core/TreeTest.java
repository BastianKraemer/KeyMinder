package de.akubix.keyminder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import de.akubix.keyminder.core.tree.DefaultTreeNode;
import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.core.tree.TreeStore;

public class TreeTest {

	private ApplicationInstance app;

	@Before
	public void prepareInstance(){
		app = KeyMinderInstanceBuilder.getNewInstance("./keyminder_settings.xml");
	}

	@Test
	public void test() {
		runSimpleTreeTest(app.getTree());
	}

	private void runSimpleTreeTest(TreeStore tree){

		app.getTree().enableEvents(true);
		app.getTree().enableNodeTimestamps(true);

		final String nodePrefix = "RootNode_";
		final int number_of_nodes = 10;
		// Create some root nodes
		for(int i = 0; i < number_of_nodes; i++){
			tree.getRootNode().addChildNode(new DefaultTreeNode(nodePrefix + i));
		}

		assertEquals(number_of_nodes + 1, tree.getRootNode().getChildNodes().size());

		tree.getRootNode().getChildNodeByIndex(0).remove();
		assertEquals(number_of_nodes, tree.getRootNode().getChildNodes().size());

		// Test Add...
		for(int i = 0; i < number_of_nodes; i++){
			assertTrue("Verifying RootNodes", tree.getRootNode().getChildNodeByIndex(i).getText().equals(nodePrefix + i));
		}

		// Test Undo
		assertTrue("Undoing last action", tree.undo(false));

		assertEquals(number_of_nodes + 1, tree.getRootNode().getChildNodes().size());

		TreeNode x = tree.getRootNode().getChildNodeByIndex(1);
		final String nodeName = "HelloWorld";
		TreeNode newNode = new DefaultTreeNode(nodeName);
		x.addChildNode(newNode);

		// Testing getNodeByPath()
		TreeNode myNode = tree.getNodeByPath("/" + x.getText() + "/" + newNode.getText());

		assertEquals("Testing method 'getNodeByPath()'", newNode.getId(), myNode.getId());

		assertEquals(number_of_nodes + 3, app.getTree().countAllNodes()); // +3 because the root node has to be included

		int indexOfNodeX = x.getIndex();
		String idOfNodeX = x.getId();
		tree.getNodeById(idOfNodeX).remove();

		if(tree.getRootNode().getChildNodeByIndex(indexOfNodeX).getId().equals(idOfNodeX)){
			fail("Method 'TreeNode.remove()' doesn't work.");
		}
	}

}
