/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	UndoManager.java

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

import java.util.LinkedList;
import java.util.List;

public class UndoManager {
	private StandardTree tree;
	private List<UndoData> buffer = new LinkedList<>();
	private boolean multipleChanges = false;
	private int undoIdCounter = -1;
	private static int maximalNumberOfUndoActions = 16;
	
	private boolean enableUndoCapturing = false;
	private int currentlyUndoableActions = 0;
	
	// This variables are needed to decide whether two sequently performed attribute changes should be captured as a single undo action or separately stored as two actions.
	private boolean lastActionWasAttributeChange = false;
	private String lastChangedAttributeKey = "";
	private int lastChangedAttributeValueLength = -1;
	private int lastChangedNodeId = -1;
	private long lastChangeTime = -1;
	
	public UndoManager(StandardTree tree)
	{
		this.tree = tree;
	}
	
	public synchronized void captureMulitpleChanges()
	{
		if(!multipleChanges){
			multipleChanges = true;
			currentlyUndoableActions++;
			undoIdCounter++;
		}
	}
	
	public synchronized void commitChanges()
	{
		multipleChanges = false;
	}
	
	public void setEnable(boolean enabled){
		this.enableUndoCapturing = enabled;
	}
	
	public boolean isEnabled(){
		return this.enableUndoCapturing;
	}

	private boolean captureCheck(int nodeId, String attributeKey, int AttributeValueLength){
		if(this.lastActionWasAttributeChange){
			//This will prevent that every "OnKeyReleaseEvent" of any Sidebar element will add a new UndoAction
			// Only capture this event IF the node is the same, the last action is less than 3 seconds ago, the attribute is the same and the length of the attribute changed by less than four characters
			if(nodeId == this.lastChangedNodeId && System.currentTimeMillis() - this.lastChangeTime < 3000 && attributeKey.equals(this.lastChangedAttributeKey) && Math.abs(this.lastChangedAttributeValueLength - AttributeValueLength) <= 4){
				this.lastChangedAttributeValueLength = AttributeValueLength;
				this.lastChangeTime = System.currentTimeMillis();
				return true;
			}
		}
		return false;
	}
	
	private void updateDataForCaptureCheck(String attributeKey, int nodeId, int attributeValueLength){
		this.lastActionWasAttributeChange = true;
		this.lastChangedAttributeKey = attributeKey;
		this.lastChangedNodeId = nodeId;
		this.lastChangedAttributeValueLength = attributeValueLength;
		this.lastChangeTime = System.currentTimeMillis();
	}
	
	// =======================================================================================================================================================
	
	public synchronized void recordTextChange(TreeNode node, String previousText)
	{
		if(enableUndoCapturing){
			lastActionWasAttributeChange = false;
			prepareBuffer();
			buffer.add(new UndoData(() -> {node.setText(previousText); return node;}));
		}
	}
	
	public synchronized void recordColorChange(TreeNode node, String previousColor)
	{
		if(enableUndoCapturing){
			lastActionWasAttributeChange = false;
			prepareBuffer();
			buffer.add(new UndoData(() -> {node.setColor(previousColor); return node;}));
		}
	}
	
	public synchronized void recordAttributeChange(TreeNode node, String attributeKey, String attributeValue)
	{
		if(enableUndoCapturing){
			if(captureCheck(node.getId(), attributeKey, attributeValue.length())){return;} //Cancel because the attribute value of the same node changed only slightly
			prepareBuffer();

			updateDataForCaptureCheck(attributeKey, node.getId(), attributeValue.length());
			buffer.add(new UndoData(() -> {node.setAttribute(attributeKey, attributeValue); return node;}));
		}
	}
	
	public synchronized void recordAttributeAdded(TreeNode node, String attributeKey)
	{
		if(enableUndoCapturing){
			lastActionWasAttributeChange = true;
			prepareBuffer();
			updateDataForCaptureCheck(attributeKey, node.getId(), 0);
			buffer.add(new UndoData(() -> {node.removeAttribute(attributeKey); return node;}));
		}
	}
	
	public synchronized void recordNodeAdded(TreeNode node)
	{
		if(enableUndoCapturing){
			lastActionWasAttributeChange = false;
			prepareBuffer();
			buffer.add(new UndoData(() -> {TreeNode parent = node.getParentNode(); tree.removeNode(node); return parent;}));
		}
	}
	
	public synchronized void recordVerticalNodeMove(TreeNode node, int inverseOffset)
	{
		if(enableUndoCapturing){
			lastActionWasAttributeChange = false;
			buffer.add(new UndoData(() -> {tree.moveNodeVertical(node, inverseOffset); return node;}));
		}
	}
	
	public synchronized void recordNodeRemoved(TreeNode node, int atIndex)
	{
		if(enableUndoCapturing){
			lastActionWasAttributeChange = false;
			buffer.add(new UndoData(() -> {
				TreeNode parent = node.getParentNode();
				node.getUnrestrictedAccess().parentNodeID = -1;
				//TreeNode newNode = tree.loadNode(node.getText(), node.getId(), node.getColor(), node.rootAccess().attributes);
				tree.insertNode(node, parent, atIndex);
				return node;
			}));
		}
	}

	// =======================================================================================================================================================

	private synchronized void prepareBuffer()
	{
		if(!multipleChanges){
			if(currentlyUndoableActions >= maximalNumberOfUndoActions){
				// cleanup
				int oldestUndoId = buffer.get(0).undoId;
				do{
					buffer.remove(0);
				}while(buffer.size() > 0 && buffer.get(0).undoId == oldestUndoId);
			}
			else{
				currentlyUndoableActions++;
			}
		}
	}
	
	/**
	 * Undo a single action
	 * @return 'true' if the last action has been undone, 'false' if the undo list is already used up
	 */
	public synchronized boolean undo()
	{
		boolean value = false;
		if(enableUndoCapturing){
			enableUndoCapturing = false;
			value = undo(-1);
			enableUndoCapturing = true;
		}
		return value;
	}
	
	private boolean undo(int undoId)
	{
		TreeNode node2select = null;
		if(buffer.size() == 0){return false;}
		if(undoId == -1){undoId = buffer.get(buffer.size() - 1).undoId;}
		do{
			TreeNode node = buffer.get(buffer.size() - 1).action.undo();
			if(node != null && node2select == null){node2select = node;}
			buffer.remove(buffer.size() - 1);
			currentlyUndoableActions--;
		} while(buffer.size() > 0 && (undoId == buffer.get(buffer.size() - 1).undoId));
		
		if(node2select != null){tree.setNodePointer(node2select);}
		return true;
	}
	
	class UndoData {
		private final UndoAction action;
		public final int undoId;
		public UndoData(UndoAction action){
			this.action = action;
			if(multipleChanges){
				this.undoId = undoIdCounter;
			}
			else{
				this.undoId = ++undoIdCounter;
			}
		}
	}
	
	public void clear()
	{
		buffer.clear();
		currentlyUndoableActions = 0;
	}
}

interface UndoAction {
	public TreeNode undo();
}
