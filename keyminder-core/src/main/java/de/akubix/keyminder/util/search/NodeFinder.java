package de.akubix.keyminder.util.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.util.search.matcher.NodeMatcher;

public class NodeFinder {

	private final List<NodeMatchResult> resultList;
	private final List<NodeMatcher> matchConditions;

	private NodeFinder(List<NodeMatcher> matchConditions){
		this.resultList = new ArrayList<>();
		this.matchConditions = matchConditions;
	}

	private void findNodes(TreeNode parentNode){
		parentNode.forEachChildNode((node) -> {
			applyMatcher(node);
			findNodes(node);
		});
	}

	private void applyMatcher(TreeNode node){
		NodeMatchResult result = null;
		for(NodeMatcher m: matchConditions){
			NodeMatchResult tmpResult = m.matches(node);
			if(!tmpResult.nodeMatches()){
				return;
			}
			else if(result == null){
				result = tmpResult;
			}
		}

		// The first matcher in the list is stored in the result list
		resultList.add(result);
	}

	public static List<NodeMatchResult> findNodes(TreeNode parentNode, List<NodeMatcher> matchConditions){
		NodeFinder finder = new NodeFinder(matchConditions);
		finder.findNodes(parentNode);
		return finder.resultList;
	}

	public static List<NodeMatchResult> findNodes(TreeNode parentNode, NodeMatcher... matchConditions){
		NodeFinder finder = new NodeFinder(Arrays.asList(matchConditions));
		finder.findNodes(parentNode);
		return finder.resultList;
	}
}
