/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	Tree.java

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
package de.akubix.keyminder.core.db;

import java.util.Map;
import java.util.function.Consumer;

/**
 * @see StandardTree
 */
public interface Tree {
	/**
	 * This method is used to detect if a file has unsaved changes
	 * The tree implementation (@link StandardTree) detects most tree changes automatically
	 * @see StandardTree
	 * @return {@code true} if the tree has been updated, {@code false} if not.
	 */
	public boolean treeHasBeenUpdated();

	/**
	 * This method allows you to set the value which declares whether the tree has been updated or not
	 * @param value the value for the "tree changed status"
	 * @see #treeHasBeenUpdated()
	 */
	public void setTreeChangedStatus(boolean value);

	/**
	 * Specify whether the tree should update the node modification date while someone edits any tree node
	 * @param value {@code true} if you want to enable time stamps, {@code false} if not
	 */
	public void enableNodeTimestamps(boolean value);

	/**
	 * You can use this method to enable or disable the  event firing.
	 * For example the treeNodeOnChange event won't occur anymore if {@code value} is {@code false}
	 * @param value use {@code true} if you want to enable tree node events, {@code false} if not
	 */
	public void enableEventFireing(boolean value);

	/**
	 * Gets the current status of the event handling
	 * @return {@code true} if the event firing is enabled, {@code false} if not.
	 * * @see enableEventFireing(boolean)
	 */
	public boolean isEventFireingEnabled();

	/**
	 * This method allows you to fire any TreeNode events manually.
	 * @param node the tree node that has been edited
	 * @see de.akubix.keyminder.core.events.TreeNodeEventHandler
	 * @see de.akubix.keyminder.core.events.EventTypes
	 */
	public void fireEditedEventForTreeNode(TreeNode node);

	/**
	 * Gets a TreeNode by using its unique identifier
	 * @param id the identifier of the node
	 * @return The requested TreeNode or {@code null} if the tree does not contain a node with this id
	 */
	public TreeNode getNodeById(int id);

	/**
	 * Returns the root node of this tree
	 * @return the root node of this tree
	 */
	public TreeNode getRootNode();

	/**
	 * Checks if a node exists. The node must be specified by its id.
	 * @param nodeId the node identifier
	 * @return {@code true} if the node exists, {@code false} if not.
	 */
	public boolean nodeExists(int nodeId);

	/**
	 * Returns the currently selected tree node
	 * @return the currently selected tree node
	 */
	public TreeNode getSelectedNode();

	/**
	 * Selects another tree node
	 * @param node The node that should be selected
	 */
	public void setSelectedNode(TreeNode node);

	/**
	 * This method allows you to get a child node identifying it by its text.
	 * @param nodetext the text of the node you are searching for
	 * @param parentNode the parent node of the node you are searching for
	 * @return the node id, {@code -1} if there is no node with this text or {@code -2} if more than one node has this text
	 */
	public int getChildNodeIdByNodeText(String nodetext, TreeNode parentNode);

	/**
	 * Returns the node that is next to the given node. This means that you will get the node with the next index at the same level.
	 * Be careful using this method while your node is already the last child node: in this case you will get the next node of the parent node.
	 * @param node the start point for this action
	 * @return the "next tree node" or the value of {@code node} if it is the "last" node of the tree
	 */
	public TreeNode getNextNode(TreeNode node);

	/**
	 * Returns the previous node of the given node. This means that you will get the node with the previous index at the same level.
	 * Be careful using this method while your node is already the last child node: in this case you will get node from your parameter itself.
	 * @param node the start point for this action
	 * @return the "previous tree node" or the value of {@code node} if it is the "first" node of the tree
	 */
	public TreeNode getPreviousNode(TreeNode node);

	/**
	 * Creates a new {@link TreeNode} Each node will automatically get a unused, unique identifier.
	 * @param text The text of your new TreeNode
	 * @return the new tree node
	 */
	public TreeNode createNode(String text);

	/**
	 * This method can be used to create new tree nodes with a predefined identifier (id)
	 * Note: If the identifier is already in use, the conflict will be detected and automatically fixed.
	 * @param text The text of your new tree node
	 * @param id The unique identifier for this node. This has to be a positive value.
	 * @param color the color
	 * @param nodeAttributes some predefined attributes
	 * @return the new tree node
	 */
	public TreeNode loadNode(String text, int id, String color, Map<String, String> nodeAttributes);

	/**
	 * Adds an existing tree node to the tree
	 * @param node the tree node that should be added
	 * @param parentNode The parent node of the TreeNode
	 * @return {@code true} if the operation was successful, or {@code false} if not has been already added to a tree
	 */
	public boolean addNode(TreeNode node, TreeNode parentNode);

	/**
	 * Adds a node to the tree. Compared to {@link #addNode(TreeNode, TreeNode)} this method allow you to specify the index of the node below its parent.
	 * @param node the node you want to insert
	 * @param parentNode the parent node
	 * @param insertAtIndex the index of the node that will be insert
	 * @return {@code true} if the operation was successful, or {@code false} if not has been already added to a tree
	 */
	public boolean insertNode(TreeNode node, TreeNode parentNode, int insertAtIndex);

	/**
	 * This function can be used to clone a TreeNode. The result is a new node which can be added to the tree and not just another reference. The cloned node will have the same attributes, but another id.
	 * @param node2clone the node you want to clone
	 * @param includeChildNodes {@code true} if you want to clone its child nodes too, {@code false} if not
	 * @return the cloned node
	 */
	public TreeNode cloneTreeNode(TreeNode node2clone, boolean includeChildNodes);

	/**
	 * Removes a TreeNode from the tree
	 * @param id the id of the node you want to remove
	 */
	public void removeNode(int id);

	/**
	 * Removes a TreeNode from the tree
	 * @param node the node you want to remove
	 */
	public void removeNode(TreeNode node);

	/**
	 * Removes all child nodes of a TreeNode
	 * @param node the tree node for this operation
	 */
	public void removeAllChildNodes(TreeNode node);

	/**
	 * Moves a node at the same level up or down. In other words: The index of the node will be changed.
	 * @param node the tree node you want to "move"
	 * @param offSet the offset, example: The node is currently the fourth child node. By using this method with offset "-2" it will be the second one.
	 */
	public void moveNodeVertical(TreeNode node, int offSet);

	/**
	 * The tree provides a clip board for internal node copy actions.
	 * This method can be used to store a node in this area, all nodes will be cloned
	 * @param node the tree node you want to copy to the internal clip board
	 */
	public void copyNodeToInternalClipboard(TreeNode node);

	/**
	 * The tree provides a clip board for internal node copy actions.
	 * This method can be used to add a node, which is stored in this area, to the tree.
	 * The new node will be added beyond the 'parentNode'.
	 * @param parentNode the parent tree node
	 * @return {@code true} if the operation finished successful, {@code false} if the clip board was empty
	 */
	public boolean pasteNodeFromInternalClipboard(TreeNode parentNode);

	/**
	 * Gets a String that specifies the 'path' of this tree node
	 * For example: The root node is called 'root' and has the child node 'NodeA' has a child node called 'NodeB'. The 'NodePath' of 'NodeB' will be '/root/NodeA/NodeB' (using "/" as separator)
	 * @param node the node you want to have the path of
	 * @param pathSeperator your path separator ('/' by default)
	 * @return the node path
	 */
	public String getNodePath(TreeNode node, String pathSeperator);

	/**
	 * Selects a node by a given path. The path can be relative to the node position or to the root node.
	 * All nodes have to be separated by a slash ('/'). For example: "/path/to/node".
	 * @param path The path of the node you want to have
	 * @return the node you wanted to have or {@code null} if the path is incorrect
	 */
	public TreeNode getNodeByPath(String path);

	/**
	 * Sorts all child nodes below a custom node
	 * @param parentNode the tree node to perform this operation
	 * @param recursive use {@code true} if you want to sort child nodes of child nodes too
	 */
	public void sortChildNodes(TreeNode parentNode, boolean recursive);

	/**
	 * This method allows you to define a lambda expression which will access all nodes in the whole tree.
	 * Note: The nodes may be accessed in an unpredictable order.
	 * @param lambda your lambda expression
	 */
	public void allNodes(Consumer<? super TreeNode> lambda);

	/**
	 * Instructs the tree to undo the last change (if possible)
	 * @return {@code true} if the last action has been undone, {@code false} if the undo list is already used up
	 */
	public boolean undo();

	/**
	 * Instructs the tree to capture all following changes will as single undo action. It's important to call 'endUpdate()' after your changes finished!
	 * Warning: Do not use this if you can't be sure that no one else will edit the tree before you finished your tree update!
	 * @see #endUpdate()
	 */
	public void beginUpdate();

	/**
	 * Finish the chain of tree changes that will be undone in a single step. (This method must be called after any use of 'beginUpdate()')
	 */
	public void endUpdate();
}
