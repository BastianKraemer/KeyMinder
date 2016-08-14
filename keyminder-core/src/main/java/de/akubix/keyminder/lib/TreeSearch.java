/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	TreeSearch.java

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.akubix.keyminder.lib;

import java.util.Map;

import de.akubix.keyminder.core.db.Tree;
import de.akubix.keyminder.core.db.TreeNode;
/**
 * This class doesn't need to be instanced. Every method of this class is static.
 * It provides some methods to find tree nodes using a regular expression.
 * All search methods will select the next matching tree node (if available) and exit.
 */
@Deprecated
public class TreeSearch {

	private TreeSearch(){}

	/**
	 * Find a node in a tree that contains a specified text (the search includes node attributes)
	 * @param find Text to find in any node
	 * @param tree the tree you want to search in
	 * @param ignoreCase use {@code false} if the search should be case sensitive
	 * @return the search result
	 */
	public static SearchResult find(String find, Tree tree, boolean ignoreCase){
		return find(find, tree, ignoreCase, new NodeTimeCondition[0]);
	}

	/**
	 * Find a node in a tree that contains a specified text (the search includes note attributes)
	 * @param find Text to find in any node
	 * @param tree the tree you want to search in
	 * @param ignoreCase use {@code false} if the search should be case sensitive
	 * @param timeConditions additional conditions for some time factors
	 * @return the search result
	 */
	public static SearchResult find(String find, Tree tree, boolean ignoreCase, NodeTimeCondition... timeConditions){
		if(tree.getRootNode().countChildNodes() > 0){
			return continueSearch(tree.getSelectedNode(), tree.getSelectedNode(), prepareInput(find), (ignoreCase ? "(?i)": ""), timeConditions, true, true);
		}
		else{
			return SearchResult.NOT_FOUND;
		}
	}

	/**
	 * Find a node in a tree that contains a specified text (the search includes note attributes)
	 * @param startNode The node where the search should start
	 * @param find Text to find in any node
	 * @param ignoreCase use {@code false} if the search should be case sensitive
	 * @return the search result
	 */
	public static SearchResult find(TreeNode startNode, String find, boolean ignoreCase){
		return find(startNode, find, new NodeTimeCondition[0], ignoreCase);
	}

	/**
	 * Find a node in a tree that contains a specified text (the search includes note attributes)
	 * @param startNode The node where the search should start
	 * @param find Text to find in any node
	 * @param timeConditions additional conditions for some time factors
	 * @param ignoreCase use {@code false} if the search should be case sensitive
	 * @return the search result
	 */
	public static SearchResult find(TreeNode startNode, String find, NodeTimeCondition[] timeConditions, boolean ignoreCase){
		if(startNode.getTree().getRootNode().countChildNodes() > 0){
			return continueSearch(startNode, startNode, prepareInput(find), (ignoreCase ? "(?i)": ""), timeConditions, true, true);
		}
		else{
			return SearchResult.NOT_FOUND;
		}
	}

	private static String prepareInput(String in){
		return in.replace("*", ".*");
	}

	private static SearchResult continueSearch(TreeNode parentNode, TreeNode startNode, String find, String regExPattern, NodeTimeCondition[] timeConditions, boolean init, boolean enableParentSearch) {
		if(parentNode.getId() == startNode.getId() && !init){return SearchResult.END_REACHED;}

		if(!regExMatch(parentNode, find, regExPattern, timeConditions) || init){
			init = false;

			for(int i = 0; i < parentNode.countChildNodes(); i++){
				TreeNode childNode = parentNode.getChildNodeByIndex(i);

				if(childNode.getId() == startNode.getId() && !init){return SearchResult.END_REACHED;}

				if(childNode.countChildNodes() == 0){
					if(regExMatch(childNode, find, regExPattern, timeConditions)){
						parentNode.getTree().setSelectedNode(childNode);
						return SearchResult.FOUND;
					}
				}
				else{
					SearchResult result = continueSearch(childNode, startNode, find, regExPattern, timeConditions, false, false);

					// Cancel search if the a node has been found or the end of the document has been reached
					if(result != SearchResult.NOT_FOUND){
						return result;
					}
				}
			}

			if(parentNode.getId() != 0 && enableParentSearch){
				TreeNode parentParentNode = parentNode.getParentNode();
				int nextIndex = parentNode.getIndex() + 1;

				if(parentParentNode.countChildNodes() > nextIndex){
					return continueSearch(parentParentNode.getChildNodeByIndex(nextIndex), startNode, find, regExPattern, timeConditions, false, enableParentSearch);
				}
				else{
					return parentSearch(parentNode, startNode, find, regExPattern, timeConditions);
				}
			}
			else{
				return SearchResult.NOT_FOUND;
			}
		}
		else{
			startNode.getTree().setSelectedNode(parentNode);

			return SearchResult.FOUND;
		}
	}

	private static SearchResult parentSearch(TreeNode childNode, TreeNode startNode, String find, String regExPattern, NodeTimeCondition[] timeConditions){
		TreeNode nextitem = childNode.getParentNode();
		if(nextitem.getId() == 0){ // Verify that it is not the root node
			if(startNode.getTree().getRootNode().countChildNodes() > 0){
				return continueSearch(startNode.getTree().getRootNode().getChildNodeByIndex(0), startNode, find, regExPattern, timeConditions, false, true);
			}
			else{
				return SearchResult.NOT_FOUND;
			}
		}
		else{
			TreeNode parentParentNode = nextitem.getParentNode();

			int nextIndex = nextitem.getIndex() + 1;

			if(parentParentNode.countChildNodes() > nextIndex)
			{
				return continueSearch(parentParentNode.getChildNodeByIndex(nextIndex), startNode, find, regExPattern, timeConditions, false, true);
			}
			else
			{
				return parentSearch(nextitem, startNode, find, regExPattern, timeConditions);
			}
		}
	}

	private static boolean regExMatch(TreeNode node, String find, String regExPattern, NodeTimeCondition[] timeConditions){
		if(node.getText().matches(regExPattern + ".*" + find + ".*")){
			if(checkAdditionalConditions(node, timeConditions)){return true;}
		}
		else{
			for(Map.Entry<String, String> entry: node.getAttributeSet()){
				if(entry.getValue().matches(regExPattern + find)){
					if(checkAdditionalConditions(node, timeConditions)){return true;}
				}
			}
		}
		return false;
	}

	private static boolean checkAdditionalConditions(TreeNode node, NodeTimeCondition[] timeConditions){
		if(timeConditions.length == 0){return true;}

		for(NodeTimeCondition ntc: timeConditions){
			if(!ntc.compareTo(node)){return false;}
		}

		return true;
	}

	/**
	 * Replaces (or substitutes) the text of the node by another one
	 * @param node the tree node which contains the text that should be replaced
	 * @param find the string you want to find
	 * @param replaceWith the string that will be the replacement
	 * @param ignoreCase use {@code false} if the operation should be case sensitive
	 * @return {@code true} if there has been anything replaced, {@code false} if not
	 */
	public static boolean replaceTextOfNode(TreeNode node, String find, String replaceWith, boolean ignoreCase){
		if(node.getText().matches((ignoreCase ? "(?i)" : "") + ".*" + find + ".*")){
			node.setText(node.getText().replaceAll((ignoreCase ? "(?i)" : "")  + find.replace("*", ".*"), replaceWith));
			return true;
		}
		return false;
	}

	public static enum SearchResult {
		FOUND, NOT_FOUND, END_REACHED
	}
}
