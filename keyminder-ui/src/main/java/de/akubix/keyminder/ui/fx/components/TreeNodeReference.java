package de.akubix.keyminder.ui.fx.components;

import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.core.tree.TreeStore;

public class TreeNodeReference {

	private TreeStore treeStore;
	private String nodeId;

	public TreeNodeReference(TreeStore treeStore, String nodeId){
		this.treeStore = treeStore;
		this.nodeId = nodeId;
	}

	public TreeNodeReference(TreeNode treeNode){
		this.treeStore = treeNode.getTree();
		this.nodeId = treeNode.getId();
	}

	/**
	 * @return the treeStore
	 */
	public TreeStore getTreeStore() {
		return treeStore;
	}

	/**
	 * @return the nodeId
	 */
	public String getNodeId() {
		return nodeId;
	}

	public TreeNode getTreeNode(){
		return treeStore.getNodeById(getNodeId());
	}
}
