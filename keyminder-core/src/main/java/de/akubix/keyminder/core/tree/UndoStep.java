package de.akubix.keyminder.core.tree;

import java.util.Map;

public class UndoStep {
	private Map<String, TreeNode> changedNodesMap;
	private Runnable postUndoActions;

	UndoStep(Map<String, TreeNode> changedNodesMap, Runnable postUndoActions) {
		super();
		this.changedNodesMap = changedNodesMap;
		this.postUndoActions = postUndoActions;
	}

	UndoStep(Map<String, TreeNode> changedNodesMap) {
		super();
		this.changedNodesMap = changedNodesMap;
		this.postUndoActions = null;
	}

	Map<String, TreeNode> getChangedNodesMap() {
		return changedNodesMap;
	}

	void runPostUndoActions(){
		if(postUndoActions != null){
			postUndoActions.run();
		}
	}

	void clear(){
		changedNodesMap.clear();
		postUndoActions = null;
	}
}
