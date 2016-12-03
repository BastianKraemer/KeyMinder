package de.akubix.keyminder.core.tree;

import java.util.Map;

public class UndoStep {
	private Map<String, TreeNode> changedNodesMap;
	private String previouslySelectedNodeId;
	private Runnable postUndoActions;

	UndoStep(Map<String, TreeNode> changedNodesMap, String previouslySelectedNodeId, Runnable postUndoActions) {
		super();
		this.changedNodesMap = changedNodesMap;
		this.previouslySelectedNodeId = previouslySelectedNodeId;
		this.postUndoActions = postUndoActions;
	}

	UndoStep(Map<String, TreeNode> changedNodesMap, String previouslySelectedNodeId) {
		this(changedNodesMap, previouslySelectedNodeId, null);
	}

	Map<String, TreeNode> getChangedNodesMap() {
		return changedNodesMap;
	}

	String getPreviouslySelectedNodeId(){
		return previouslySelectedNodeId;
	}

	void runPostUndoActions(){
		if(postUndoActions != null){
			postUndoActions.run();
		}
	}

	void clear(){
		changedNodesMap.clear();
		postUndoActions = null;
		previouslySelectedNodeId = null;
	}
}
