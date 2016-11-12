package de.akubix.keyminder.util.search;

import java.util.Arrays;
import java.util.List;

import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.core.tree.TreeStore;
import de.akubix.keyminder.util.search.matcher.NodeMatcher;

public final class NodeWalker {

	private List<NodeMatcher> matchConditions;
	private TreeNode startNode;

	private NodeWalker(TreeNode startNode, List<NodeMatcher> matchConditions){
		super();
		this.matchConditions = matchConditions;
		this.startNode = startNode;
	};

	/**
	 * Find a node in a tree that contains a specified text (the search includes node attributes)
	 * @param tree the tree you want to search in
	 * @param matchConditions the match conditions
	 * @return the search result
	 */
	public static SearchResult find(TreeStore tree, NodeMatcher... matchConditions){
		return find(tree, Arrays.asList(matchConditions));
	}

	/**
	 * Find a node in a tree that contains a specified text (the search includes node attributes)
	 * @param tree the tree you want to search in
	 * @param matchConditions the match conditions
	 * @return the search result
	 */
	public static SearchResult find(TreeStore tree, List<NodeMatcher> matchConditions){

		NodeWalker walker = new NodeWalker(tree.getSelectedNode(), matchConditions);

		if(tree.getRootNode().countChildNodes() > 0){
			return walker.findNext(tree.getSelectedNode(), true, true);
		}
		else{
			return SearchResult.none();
		}
	}

	public static NodeMatchResult nodeMatches(TreeNode node, NodeMatcher... matchConditions){
		return nodeMatches(node, Arrays.asList(matchConditions));
	}

	public static NodeMatchResult nodeMatches(TreeNode node, List<NodeMatcher> matchConditions){

		NodeMatchResult result = null;
		for(NodeMatcher m: matchConditions){
			NodeMatchResult tmpResult = m.matches(node);
			if(!tmpResult.nodeMatches()){
				return null;
			}
			else if(result == null){
				result = tmpResult;
			}
		}

		return result;
	}

	private SearchResult findNext(TreeNode parentNode, boolean init, boolean enableParentSearch) {

		if(parentNode.getId() == startNode.getId() && !init){
			return SearchResult.endReached();
		}

		if(!init){
			NodeMatchResult result = nodeMatches(parentNode, matchConditions);
			if(result != null){
				startNode.getTree().setSelectedNode(parentNode);
				return SearchResult.match(result);
			}
		}

		init = false;

		for(int i = 0; i < parentNode.countChildNodes(); i++){
			TreeNode childNode = parentNode.getChildNodeByIndex(i);

			if(childNode.getId() == startNode.getId() && !init){return SearchResult.endReached();}

			if(childNode.countChildNodes() == 0){
				NodeMatchResult result = nodeMatches(childNode, matchConditions);
				if(result != null){
					parentNode.getTree().setSelectedNode(childNode);
					return SearchResult.match(result);
				}
			}
			else{
				SearchResult searchResult = findNext(childNode, false, false);

				// Cancel search if the a node has been found or the end of the document has been reached
				if(searchResult.getState() != SearchState.NOT_FOUND){
					return searchResult;
				}
			}
		}

		if(!parentNode.isRootNode() && enableParentSearch){
			TreeNode parentParentNode = parentNode.getParentNode();
			int nextIndex = parentNode.getIndex() + 1;

			if(parentParentNode.countChildNodes() > nextIndex){
				return findNext(parentParentNode.getChildNodeByIndex(nextIndex), false, enableParentSearch);
			}
			else{
				return findNextUsingParentNode(parentNode);
			}
		}
		else{
			return SearchResult.none();
		}
	}

	private SearchResult findNextUsingParentNode(TreeNode childNode){
		TreeNode nextitem = childNode.getParentNode();
		if(nextitem.isRootNode()){ // Verify that it is not the root node
			if(startNode.getTree().getRootNode().countChildNodes() > 0){
				return findNext(startNode.getTree().getRootNode().getChildNodeByIndex(0), false, true);
			}
			else{
				return SearchResult.none();
			}
		}
		else{
			TreeNode parentParentNode = nextitem.getParentNode();

			int nextIndex = nextitem.getIndex() + 1;

			if(parentParentNode.countChildNodes() > nextIndex){
				return findNext(parentParentNode.getChildNodeByIndex(nextIndex), false, true);
			}
			else{
				return findNextUsingParentNode(nextitem);
			}
		}
	}

	public static enum SearchState {
		FOUND, NOT_FOUND, END_REACHED
	}

	public static final class SearchResult {

		private final SearchState state;
		private final NodeMatchResult matchResult;

		private SearchResult(SearchState state, NodeMatchResult matchResult){
			this.state = state;
			this.matchResult = matchResult;
		}

		/**
		 * @return the state
		 */
		public SearchState getState() {
			return state;
		}

		/**
		 * @return the matchResult
		 */
		public NodeMatchResult getMatchResult() {
			return matchResult;
		}

		private static SearchResult match(NodeMatchResult matchResult){
			return new SearchResult(SearchState.FOUND, matchResult);
		}

		private static SearchResult none(){
			return new SearchResult(SearchState.NOT_FOUND, null);
		}

		private static SearchResult endReached(){
			return new SearchResult(SearchState.END_REACHED, null);
		}
	}


}
