package de.akubix.keyminder.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import de.akubix.keyminder.core.db.StandardTree;
import de.akubix.keyminder.core.db.Tree;
import de.akubix.keyminder.core.db.TreeNode;

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
