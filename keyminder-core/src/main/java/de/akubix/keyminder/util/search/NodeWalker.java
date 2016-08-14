package de.akubix.keyminder.util.search;

import java.util.List;

import de.akubix.keyminder.core.db.Tree;
import de.akubix.keyminder.core.db.TreeNode;
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
	 * Find a node in a tree that contains a specified text (the search includes note attributes)
	 * @param find Text to find in any node
	 * @param tree the tree you want to search in
	 * @param ignoreCase use {@code false} if the search should be case sensitive
	 * @param timeConditions additional conditions for some time factors
	 * @return the search result
	 */
	public static SearchResult find(Tree tree, List<NodeMatcher> matchConditions){

		NodeWalker walker = new NodeWalker(tree.getSelectedNode(), matchConditions);

		if(tree.getRootNode().countChildNodes() > 0){
			return walker.findNext(tree.getSelectedNode(), true, true);
		}
		else{
			return SearchResult.none();
		}
	}

	private NodeMatchResult nodeMatches(TreeNode node){

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

		NodeMatchResult result = nodeMatches(parentNode);
		if(result != null){
			startNode.getTree().setSelectedNode(parentNode);
			return SearchResult.match(result);
		}

		init = false;

		for(int i = 0; i < parentNode.countChildNodes(); i++){
			TreeNode childNode = parentNode.getChildNodeByIndex(i);

			if(childNode.getId() == startNode.getId() && !init){return SearchResult.endReached();}

			if(childNode.countChildNodes() == 0){
				result = nodeMatches(parentNode);
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

		if(parentNode.getId() != 0 && enableParentSearch){
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
		if(nextitem.getId() == 0){ // Verify that it is not the root node
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
			return new SearchResult(SearchState.NOT_FOUND, matchResult);
		}

		private static SearchResult none(){
			return new SearchResult(SearchState.NOT_FOUND, null);
		}

		private static SearchResult endReached(){
			return new SearchResult(SearchState.END_REACHED, null);
		}
	}


}
