/*	KeyMinder
 * Copyright (C) 2015-2016 Bastian Kraemer
 *
 * TreeNode.java
 *
 * KeyMinder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * KeyMinder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with KeyMinder.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.akubix.keyminder.core.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.events.EventTypes.TreeNodeEvent;
import de.akubix.keyminder.util.Utilities;

/**
 * Base class for every tree node
 */
public abstract class TreeNode {

	private String identifier;
	private String parentNodeIdentifer;
	private TreeStore tree;
	private List<String> childNodes;
	private boolean isExpanded;

	protected TreeNode(){
		this.childNodes = canHaveChildNodes() ? new ArrayList<>() : null;
		this.isExpanded = false;
	}

	/**
	 * Returns the unique identifier of this TreeNode
	 * @return the id of the node
	 */
	public final String getId(){
		return this.identifier;
	}

	final void setId(String id){
		this.identifier = id;
	}

	private final void setParentNodeId(String parentNodeId){
		this.parentNodeIdentifer = parentNodeId;
	}

	final void setTree(TreeStore tree){
		this.tree = tree;
	}

	public abstract boolean isGenerated();
	public abstract boolean canHaveChildNodes();
	public abstract boolean canHaveAttributes();

	/**
	 * Returns the current index of the TreeNode.
	 * For example: The node is the second node below its parent, then the index will be "1" .
	 * @return the current index of this TreeNode.
	 */
	public final int getIndex(){
		if(this.identifier == null){
			return -1;
		}
		if(!this.identifier.equals("0") && this.parentNodeIdentifer != null){
			int index = 0;

			for(String childNodeIdentifier: getParentNode().childNodes){
				if(childNodeIdentifier.equals(this.identifier)){
					return index;
				}
				index++;
			}
		}

		return -1;
	}

	/**
	 * @return the isExpanded
	 */
	public boolean isExpanded() {
		return isExpanded;
	}

	/**
	 * @param isExpanded the isExpanded to set
	 */
	public void setExpanded(boolean isExpanded) {
		this.isExpanded = isExpanded;
	}

	/**
	 * Returns the current text of this node
	 * @return the text of this node
	 */
	public abstract String getText();

	/**
	 * Sets a new text
	 * @param text the new text
	 * @return the TreeNode itself (maybe you want to perform more actions in a single line)
	 */
	public abstract TreeNode setText(String text);

	/**
	 * Returns the color that has been assigned to this node
	 * @return the color of this node
	 */
	public abstract String getColor();

	/**
	 * Assigns another color to this TreeNode
	 * @param color the new color for this node (as HTML-Value)
	 * @return the TreeNode itself (maybe you want to perform more actions in a single line)
	 */
	public abstract TreeNode setColor(String color);

	/**
	 * Returns all child nodes of this node
	 * @return an array of all child nodes
	 */
	public final List<TreeNode> getChildNodes(){
		requireRegisteredNode();
		if(this.childNodes != null){
			synchronized (childNodes) {
				return this.childNodes.stream().map(tree::getNodeById).collect(Collectors.toList());
			}
		}
		else{
			return Collections.emptyList();
		}
	}

	/**
	 * This method allows you to use a lambda expression to iterate over all child nodes
	 * @param lambda the lambda expression
	 */
	public final void forEachChildNode(Consumer<? super TreeNode> lambda){
		requireRegisteredNode();
		if(this.childNodes != null){ // this.childNodes == null is equal to canHaveAttributes()
			synchronized (childNodes) {
				this.getChildNodes().forEach(lambda);
			}
		}
	}

	/**
	 * Returns the child node at a specific index
	 * @param index the index of the child node
	 * @return the child node at this index
	 * @throws IndexOutOfBoundsException if the index is lower than zero or greater than the number of child nodes
	 */
	public final TreeNode getChildNodeByIndex(int index) throws IndexOutOfBoundsException {
		requireRegisteredNode();
		if(this.childNodes != null){
			return this.tree.getNodeById(this.childNodes.get(index));
		}
		else{
			throw new IndexOutOfBoundsException("This tree node does not have any child nodes.");
		}
	}

	/**
	 * Counts the number of child nodes that has been assigned to this node
	 * This number does not include any child node below these child nodes
	 * @return the number of child nodes
	 */
	public final int countChildNodes(){
		return this.childNodes != null ? this.childNodes.size() : 0;
	}

	/**
	 * Returns the parent bode of this tree node
	 * @return the parent node
	 */
	public TreeNode getParentNode(){
		requireRegisteredNode();
		if(this.parentNodeIdentifer != null){
			return tree.getNodeById(parentNodeIdentifer);
		}
		return null;
	}

	final String getParentNodeIdentifier(){
		return this.parentNodeIdentifer;
	}

	public final void addChildNode(TreeNode treeNode) throws IllegalStateException, UnsupportedOperationException {
		if(!canHaveChildNodes()){throw new UnsupportedOperationException("This node cannot have child nodes.");}

		requireRegisteredNode();

		synchronized (childNodes) {

			tree.registerNode(treeNode);

			if(this.tree.isUndoEnabled()){
				this.tree.getUndoBuilder().addNull(treeNode.getId()).addNode(this).commit();
			}

			treeNode.setParentNodeId(this.getId());
			this.childNodes.add(treeNode.getId());
			this.tree.setTreeChangedStatus(true);
			this.tree.fireNodeEvent(treeNode, TreeNodeEvent.OnNodeAdded);
		}
	}

	public final void insertChildNode(TreeNode treeNode, int index) throws IllegalStateException, UnsupportedOperationException {

		if(!canHaveChildNodes()){throw new UnsupportedOperationException("This node cannot have child nodes.");}

		requireRegisteredNode();

		synchronized (childNodes) {
			tree.registerNode(treeNode);
			if(this.tree.isUndoEnabled()){
				this.tree.getUndoBuilder().addNull(treeNode.getId()).addNode(this).commit();
			}

			treeNode.setParentNodeId(this.getId());
			this.childNodes.add(index, treeNode.getId());
			this.tree.setTreeChangedStatus(true);
			this.tree.fireNodeEvent(treeNode, TreeNodeEvent.OnNodeAdded);
			this.tree.fireNodeEvent(treeNode, TreeNodeEvent.OnNodeVerticallyMoved);
		}
	}

	public final void remove(){
		if(isRootNode()){throw new UnsupportedOperationException("You cannot remove the root node.");}

		requireRegisteredNode();

		if(this.tree.isUndoEnabled()){
			this.tree.getUndoBuilder().addNode(getParentNode()).addNodeIncludeChildNodes(this).commit();
		}

		final TreeStore myTree = this.tree;
		this.tree.fireNodeEvent(this, TreeNodeEvent.OnNodeRemoved);
		unregister();
		setParentNodeId(null);

		myTree.verifyNotEmptyTree();
	}

	final void unregister(){
		if(isRootNode()){throw new UnsupportedOperationException("You cannot unregister the root node.");}

		requireRegisteredNode();

		synchronized (childNodes) {
			this.tree.unregisterNode(this);
			getParentNode().childNodes.remove(getId());
			this.tree = null;
		}
	}

	private void requireRegisteredNode() throws IllegalStateException {
		if(this.tree == null){
			throw new IllegalStateException("The node must be assigned to a tree at frist.");
		}
	}

	public void move(Direction dir){

		requireRegisteredNode();

		if(!isRootNode()){
			synchronized (childNodes) {

				if(this.tree.isUndoEnabled()){
					this.tree.getUndoBuilder().addNode(getParentNode()).commit();
				}

				final boolean reselectNode = (this.tree.getSelectedNode() == this);

				TreeNode parentNode = this.getParentNode();

				int index = 0;
				for(String childNodeId: parentNode.childNodes){
					if(childNodeId == this.identifier){break;}
					index++;
				}

				int newIndex = index + (dir == Direction.UP ? -1 : 1);
				if(newIndex < 0){
					if(index == 0){return;} // Node is already at the beginning
					newIndex = 0;
				}
				else{
					int childNodeCnt = parentNode.countChildNodes();
					if(newIndex >= childNodeCnt){
						if(index == childNodeCnt){return;} // Node is already at the end
						newIndex = childNodeCnt - 1;
					}
				}

				parentNode.childNodes.set(index, parentNode.childNodes.get(newIndex));
				parentNode.childNodes.set(newIndex, getId());

				nodeUpdated();
				if(reselectNode){
					tree.setSelectedNode(this);
				}
			}
		}
	}

	/**
	 * Returns the tree to which the node has been assigned.
	 * This value can be {@code null} if the node has not been assigned to tree yet.
	 * @return the tree of this node
	 */
	public final TreeStore getTree(){
		return this.tree;
	}

	/**
	 * Checks if the attribute hash contains a specified key
	 * @param name the name of the attribute
	 * @return {@code true} if the attribute exists, {@code false} if not
	 */
	public abstract boolean hasAttribute(String name);

	/**
	 * Returns the value of an attribute of the this tree node
	 * @param name the name of this attribute
	 * @return the requested value or an empty string ("") if there is no attribute with this name
	 */
	public abstract String getAttribute(String name);

	/**
	 * Stores a value in the attribute hash of this tree node
	 * @param name the name of the attribute
	 * @param value the value for this attribute
	 */
	public void setAttribute(String name, String value){
		setAttribute(name, value, false);
	}

	/**
	 * Stores a value in the attribute hash of this tree node
	 * @param name the name of the attribute
	 * @param value the value for this attribute
	 * @param silent if {@code true} the method should not call {@link #preNodeUpdate()} or {@link #nodeUpdated()}
	 */
	protected abstract void setAttribute(String name, String value, boolean silent);

	/**
	 * Removes an attribute from the internal hash
	 * @param name the name of the attribute
	 */
	public abstract void removeAttribute(String name);

	/**
	 * Returns a list (respectively a {@link Set}) of all attributes of this tree node
	 * @return the {@link Set} of attributes
	 */
	public abstract Set<String> listAttributes();

	/**
	 * Returns a set with all node attributes
	 * @return
	 */
	public abstract Set<Map.Entry<String, String>> getAttributes();

	public abstract void onParentUpdate();

	public final void sortChildNodes(final boolean recursive){

		synchronized (childNodes) {
			requireRegisteredNode();

			if(countChildNodes() > 0){
				 getTree().transaction(() -> sortChildNodesAsTransaction(recursive));
			}
		}
	}

	private final void sortChildNodesAsTransaction(final boolean recursive){

		if(countChildNodes() > 0){
			this.tree.getUndoBuilder().addNode(this).commit();

			final Map<String, String> uniqueNodeTextMap = new HashMap<>();
			getChildNodes().forEach((node) -> {
				String uniqueNodeText = node.getText().toLowerCase();
				int ext = 0;
				while(uniqueNodeTextMap.containsKey(uniqueNodeText)){
					uniqueNodeText = node.getText().toLowerCase() + ext;
					ext++;
				}

				uniqueNodeTextMap.put(uniqueNodeText, node.getId());
			});

			this.childNodes.clear();
			Utilities.asSortedList(uniqueNodeTextMap.keySet()).stream().map(uniqueNodeTextMap::get).forEach(this.childNodes::add);

			nodeUpdated();

			if(recursive){
				this.forEachChildNode((node) -> node.sortChildNodesAsTransaction(recursive));
			}

			if(getTree().areEventsEnabled()){
				getTree().fireNodeEvent(this, TreeNodeEvent.OnNodeVerticallyMoved);
			}
		}
	}

	protected void preNodeUpdate(){
		if(this.tree != null && this.tree.isUndoEnabled()){
			this.tree.getUndoBuilder().addNode(this).commit();
		}
	}

	protected void nodeUpdated(){
		if(tree != null){
			if(tree.areNodeTimestampsEnabled() && this.canHaveAttributes()){
				this.setAttribute(ApplicationInstance.NODE_ATTRIBUTE_MODIFICATION_DATE, Long.toString(System.currentTimeMillis()), true);
			}
			notifyChildNodes();
			this.tree.setTreeChangedStatus(true);
			this.tree.fireNodeEvent(this, TreeNodeEvent.OnNodeEdited);
		}
	}

	protected void notifyChildNodes(){
		this.forEachChildNode(childNode -> childNode.onParentUpdate());
	}

	public boolean isRootNode(){
		if(this.identifier == null){
			return false;
		}
		return this.identifier.equals(TreeStore.ROOT_NODE_IDENTIFIER);
	}

	public String getNodePath(){
		return getNodePath("/");
	}

	public String getNodePath(String pathSeperator){
		if(isRootNode()){return pathSeperator + getText();}

		StringBuilder path = new StringBuilder(pathSeperator).append(getText());
		TreeNode currentNode = this;
		while(true){
			currentNode = currentNode.getParentNode();
			if(!currentNode.isRootNode()){
				path.insert(0, pathSeperator + currentNode.getText());
			}
			else{
				return path.toString();
			}
		}
	}

	@Override
	public TreeNode clone(){
		return clone(CloneMode.IDENTICAL);
	}

	public final TreeNode clone(CloneMode cloneMode){

		TreeNode clone = cloneNode();

		clone.setId(this.getId());
		clone.setText(this.getText());
		clone.setColor(this.getColor());
		clone.setExpanded(isExpanded);

		if(cloneMode != CloneMode.NODE_ONLY){
			this.childNodes.forEach(clone.childNodes::add);
		}

		if(cloneMode == CloneMode.IDENTICAL){
			clone.setParentNodeId(this.parentNodeIdentifer);
		}

		return clone;
	}

	protected abstract TreeNode cloneNode();

	@Override
	public String toString(){
		return getText();
	}
}
