package de.akubix.keyminder.core.tree;

import java.util.HashMap;
import java.util.Map;

public class UndoBuilder {
	private TreeStore tree;
	private Map<String, TreeNode> changedNodesMap;

	UndoBuilder(TreeStore tree){
		this.tree = tree;
		this.changedNodesMap = new HashMap<>();
	}

	public UndoBuilder addNode(TreeNode node){
		changedNodesMap.put(node.getId(), node.clone(CloneMode.IDENTICAL));
		return this;
	}

	public UndoBuilder addNodeIncludeChildNodes(TreeNode node){
		changedNodesMap.put(node.getId(), node.clone(CloneMode.IDENTICAL));
		captureEachChildNode(node);
		return this;
	}

	private final void captureEachChildNode(final TreeNode treeNode){
		treeNode.forEachChildNode((childNode) -> {
			changedNodesMap.put(childNode.getId(), childNode.clone(CloneMode.IDENTICAL));
			captureEachChildNode(childNode);
		});
	}

	public UndoBuilder addNull(String id){
		changedNodesMap.put(id, null);
		return this;
	}

	public void commit(){
		commit(null);
	}

	void commit(Runnable postUndoActions){
		tree.captureNodeState(new UndoStep(changedNodesMap, postUndoActions));
	}
}
