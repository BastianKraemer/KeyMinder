/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	StandardNode.java

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * This class is part of the database an contains all data of a single node.
 *
 * @see TreeNode
 * @see StandardTree
 * @see Tree
 */
public class StandardNode implements TreeNode{
	protected int nodeID;
	private StandardTree myTree;
	private String text;
	private String color = "";
	protected Map<String, String> attributes;
	protected ArrayList<Integer> childNodes = new ArrayList<>();
	protected int parentNodeID = -1;

	protected StandardNode(StandardTree myTree, int nodeId) {
		this(myTree, nodeId, "");
	}

	protected StandardNode(StandardTree myTree, int nodeId, String text) {
		this.myTree = myTree;
		this.nodeID = nodeId;
		this.text = text;
		this.attributes = new HashMap<>();
		if(myTree.enableNodeTimestamps)
		{
			attributes.put(de.akubix.keyminder.core.ApplicationInstance.NODE_ATTRIBUTE_CREATION_DATE, Long.toString(System.currentTimeMillis()));
			attributes.put(de.akubix.keyminder.core.ApplicationInstance.NODE_ATTRIBUTE_MODIFICATION_DATE, Long.toString(System.currentTimeMillis()));
		}
	}

	protected StandardNode(StandardTree myTree, int nodeId, String text, String color, Map<String, String> nodeAttributes) {
		this.myTree = myTree;
		this.nodeID = nodeId;
		this.text = text;
		this.color = color;
		this.attributes = nodeAttributes;
		if(myTree.enableNodeTimestamps)
		{
			if(!attributes.containsKey(de.akubix.keyminder.core.ApplicationInstance.NODE_ATTRIBUTE_CREATION_DATE)){attributes.put(de.akubix.keyminder.core.ApplicationInstance.NODE_ATTRIBUTE_CREATION_DATE, Long.toString(System.currentTimeMillis()));}
			if(!attributes.containsKey(de.akubix.keyminder.core.ApplicationInstance.NODE_ATTRIBUTE_MODIFICATION_DATE)){attributes.put(de.akubix.keyminder.core.ApplicationInstance.NODE_ATTRIBUTE_MODIFICATION_DATE, Long.toString(System.currentTimeMillis()));}
		}
	}

	private void updateModificationDate()
	{
		if(myTree.enableNodeTimestamps)
		{
			attributes.put(de.akubix.keyminder.core.ApplicationInstance.NODE_ATTRIBUTE_MODIFICATION_DATE, Long.toString(System.currentTimeMillis()));
		}
	}

	@Override
	public int getId(){
		return nodeID;
	}

	// ===== Attribute text
	@Override
	public synchronized String getText() {
		return this.text;
	}

	@Override
	public synchronized TreeNode setText(String newText) {
		myTree.undoManager.recordTextChange(this, this.text);
		this.text = newText;

		updateModificationDate();
		myTree.fireEditedEventForTreeNode(this);
		myTree.setTreeChangedStatus(true);
		return this;
	}

	// ===== Attribute color
	@Override
	public synchronized String getColor() {
		return this.color;
	}

	@Override
	public synchronized TreeNode setColor(String newColor){
		myTree.undoManager.recordColorChange(this, this.color);
		this.color = newColor;
		updateModificationDate();
		myTree.setTreeChangedStatus(true);
		myTree.fireEditedEventForTreeNode(this);

		return this;
	}

	// ===== Specific String Attributes

	@Override
	public boolean hasAttribute(String name){
		return attributes.containsKey(name);
	}

	@Override
	public synchronized String getAttribute(String name){
		return attributes.containsKey(name) ? attributes.get(name) : "";
	}

	@Override
	public synchronized void setAttribute(String name, String value){
		if(!(name.equals("") || name.toLowerCase().equals("id")))
		{
			if(attributes.containsKey(name)){
				myTree.undoManager.recordAttributeChange(this, name, attributes.get(name));
			}
			else{
				myTree.undoManager.recordAttributeAdded(this, name);
			}

			attributes.put(name, value);
			updateModificationDate();
			myTree.setTreeChangedStatus(true);
		}
	}

	@Override
	public synchronized void removeAttribute(String name){
		if(attributes.containsKey(name)){
			myTree.undoManager.recordAttributeChange(this, name, attributes.get(name));
			attributes.remove(name);
			updateModificationDate();
			myTree.setTreeChangedStatus(true);
		}
	}

	@Override
	public Set<String> listAttributes(){
		return attributes.keySet();
	}

	@Override
	public synchronized TreeNode[] getChildNodes(){
		TreeNode[] childnodeArray = new StandardNode[childNodes.size()];
		for(int i = 0; i < childNodes.size(); i++){
			childnodeArray[i] = myTree.getNodeById(childNodes.get(i));
		}
		return childnodeArray;
	}

	@Override
	public synchronized void forEachChildNode(Consumer<? super TreeNode> lambda){
		childNodes.forEach((id) -> lambda.accept(myTree.getNodeById(id)));
	}

	@Override
	public TreeNode getChildNodeByIndex(int index) throws IndexOutOfBoundsException	{
			return myTree.getNodeById(childNodes.get(index));
	}

	@Override
	public int countChildNodes(){
		return childNodes.size();
	}

	public synchronized boolean setParentNode(int id){
		if(parentNodeID == -1){
			parentNodeID = id;
			return true;
		}
		return false;
	}

	@Override
	public synchronized TreeNode getParentNode(){
		if(parentNodeID >= 0){
			return myTree.getNodeById(parentNodeID);
		}
		else{
			return null;
		}
	}

	@Override
	public StandardNode getUnrestrictedAccess(){
		return this;
	}

	@Override
	public Tree getTree() {
		return (Tree) myTree;
	}

	@Override
	public synchronized int getIndex(){
		if(nodeID != 0){
			int index = 0;
			for(int childnodeID: getParentNode().getUnrestrictedAccess().childNodes){
				if(childnodeID == nodeID){return index;}
				index++;
			}
		}

		return 0;
	}

	@Override
	public Set<Map.Entry<String, String>> getAttributeSet(){
		return Collections.unmodifiableSet(this.attributes.entrySet());
	}

	public synchronized void clearAttributes(boolean doAutoCommitForUndo){
		myTree.undoManager.captureMulitpleChanges();
		attributes.keySet().forEach((key) -> myTree.undoManager.recordAttributeChange(this, key, attributes.get(key)));
		if(doAutoCommitForUndo){myTree.undoManager.commitChanges();}
		this.attributes.clear();
	}

	@Override
	public String toString(){
		return this.text;
	}
}
