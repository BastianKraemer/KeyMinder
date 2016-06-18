/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	StandardTree.java

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import de.akubix.keyminder.core.interfaces.events.EventTypes;
import de.akubix.keyminder.core.interfaces.events.EventTypes.TreeNodeEvent;

/**
 * This class is the core of the whole database. All data stored in this applications will be assigned to the HashMap treeNodeDB in this class
 * @see StandardNode
 * @see Tree
 * @see TreeNode
 */
public class StandardTree implements Tree {
	private de.akubix.keyminder.core.ApplicationInstance app;
	private HashMap<Integer, StandardNode> treeNodeDB = new HashMap<>();
	private TreeNode nodePointer;
	private boolean treeWasUpdated = false;
	private boolean enableFireEvents = false;
	protected boolean enableNodeTimestamps = false;
	private StandardNode rootNode;
	private int idCounter = 0; //contains the last used id - increment it to get the next free id
	private int idConflicts = 0;
	public UndoManager undoManager;
	public StandardTree(de.akubix.keyminder.core.ApplicationInstance instance)
	{
		app = instance;
		createRoot();
	}

	private void createRoot(){
		rootNode = new StandardNode(this, 0, "root");
		treeNodeDB.put(0, rootNode);
		nodePointer = rootNode;
		undoManager = new UndoManager(this);
	}

	public synchronized void reset(){
		if(treeNodeDB.size() > 1){
			treeNodeDB.clear();
			System.gc(); //After the clear() operation many objects are trash...
			createRoot();
		}

		treeNodeClipboard = null;
		idCounter = 0;
		idConflicts = 0;
		treeWasUpdated = false;
		enableFireEvents = false;
		enableNodeTimestamps = false;;
	}

	@Override
	public boolean treeHasBeenUpdated(){
		return treeWasUpdated;
	}

	@Override
	public void setTreeChangedStatus(boolean value){
		treeWasUpdated = value;
	}

	@Override
	public void enableNodeTimestamps(boolean value){
		enableNodeTimestamps = value;
	}

	@Override
	public void enableEventFireing(boolean value){
		enableFireEvents = value;
	}

	@Override
	public boolean isEventFireingEnabled(){
		return enableFireEvents;
	}

	@Override
	public synchronized void fireEditedEventForTreeNode(TreeNode node){
		if(enableFireEvents){
			app.fireEvent(TreeNodeEvent.OnNodeEdited, node);
		}
	}

	public int countAllNodes(){
		return treeNodeDB.size();
	}

	@Override
	public boolean nodeExists(int nodeId){
		return treeNodeDB.containsKey(nodeId);
	}

	@Override
	public TreeNode getNodeById(int id){
		return treeNodeDB.get(id);
	}

	@Override
	public TreeNode getSelectedNode(){
		return nodePointer;
	}

	@Override
	public void setSelectedNode(TreeNode node){
		if(node.getId() != nodePointer.getId())
		{
			setNodePointer(node);
		}
	}

	// The functionality "setSelectedNode" has been split into two methods, because there has to be a private "setNodePointer()" method which will force firing an event, even if it is the same node
	protected synchronized void setNodePointer(TreeNode node){
		if(node != null){
			nodePointer = node;
			if(enableFireEvents){app.fireEvent(EventTypes.TreeNodeEvent.OnSelectedItemChanged, node);}
		}
	}

	/**
	 * Reset the NodePointer to its default position, this is the first node below the root node, or the root node if the tree is empty
	 */
	public synchronized void resetNodePointer(){
		if(getRootNode().countChildNodes() > 0){
			if(treeNodeDB.containsKey(getRootNode().getChildNodeByIndex(0).getId())){
				// Selecting first node
				setSelectedNode(getRootNode().getChildNodeByIndex(0));
			}
			else{
				// Tree is empty
				setSelectedNode(getRootNode());
			}
		}
		else{
			setSelectedNode(getRootNode());
		}
	}

	@Override
	public TreeNode getRootNode(){
		return (TreeNode) rootNode;
	}

	public StandardNode getDocumentRootNode(){
		return rootNode;
	}

	@Override
	public TreeNode getNextNode(TreeNode node){
		return getNextNode(node, 1);
	}

	@Override
	public TreeNode getPreviousNode(TreeNode node){
			return getNextNode(node, -1);
	}

	private TreeNode getNextNode(TreeNode node, int offset){
		if(node.getId() != 0){
			try{
				int i = node.getIndex();
				if(i + offset < 0){
					if(node.getParentNode().getId() != 0){
						return node.getParentNode();
					}
				}
				return node.getParentNode().getChildNodeByIndex(i + offset);
			}
			catch(IndexOutOfBoundsException ex){
				return (node.getParentNode().getId() != 0 ? getNextNode(node.getParentNode(), 1) : node);
			}
		}
		else{
			return node;
		}
	}

	@Override
	public synchronized boolean addNode(TreeNode node, TreeNode parentNode){
		if(node.getParentNode() == null){
			// Node does not contain to the tree
			treeNodeDB.put(node.getId(), node.getUnrestrictedAccess());
			parentNode.getUnrestrictedAccess().childNodes.add(node.getId());
			node.getUnrestrictedAccess().setParentNode(parentNode.getId());
			treeWasUpdated = true;

			if(enableFireEvents){app.fireEvent(EventTypes.TreeNodeEvent.OnNodeAdded, node);}
			undoManager.recordNodeAdded(node);
			return true;
		}
		else{
			app.log("ERROR: Couldn't append treenode to tree, because the node has been already assigned to it.");
			return false;
		}
	}

	@Override
	public synchronized boolean insertNode(TreeNode node, TreeNode parentNode, int insertAtIndex){
		if(addNode(node, parentNode)){
			StandardNode pNode = parentNode.getUnrestrictedAccess();
			int lastIndex = (pNode.countChildNodes() - 1);
			for(int i = insertAtIndex; i < lastIndex; i++){
				int tmp = pNode.childNodes.get(lastIndex);
				pNode.childNodes.set(lastIndex, pNode.childNodes.get(i));
				pNode.childNodes.set(i, tmp);
			}
			undoManager.recordNodeAdded(node);
			if(enableFireEvents){app.fireEvent(EventTypes.TreeNodeEvent.OnNodeVerticallyMoved, node);}
			return true;
		}
		else{
			return false;
		}
	}

	@Override
	public TreeNode createNode(String text){
		idCounter++;
		return new StandardNode(this, idCounter, text);
	}

	@Override
	public TreeNode loadNode(String text, int predefinedId, String color, Map<String, String> nodeAttributes) {
		if(treeNodeDB.containsKey(predefinedId) || predefinedId <= 0){
			// This is a conflict, the id is negative or already in use.
			idConflicts--;
			predefinedId = idConflicts;
			// A negative id is not really a problem, it will be corrected if someone calls "verify()"
		}
		else if(predefinedId > idCounter){
			idCounter = predefinedId;
		}

		return new StandardNode(this, predefinedId, text, color, nodeAttributes);
	}

	private StandardNode createDefaultNode(String text) {
		idCounter++;
		return new StandardNode(this, idCounter, text);
	}

	@Override
	public void removeNode(int id) {
		removeNode(getNodeById(id).getUnrestrictedAccess());
	}

	@Override
	public void removeNode(TreeNode node) {
		removeNode(node.getUnrestrictedAccess());
	}

	private synchronized void removeNode(StandardNode node){
		if(node.getId() != 0){ //The root node can't be removed
			undoManager.captureMulitpleChanges();
			StandardNode parentNode = node.getParentNode().getUnrestrictedAccess();

			// Fire an event to notify UI and modules
			if(enableFireEvents){app.fireEvent(EventTypes.TreeNodeEvent.OnNodeRemoved, node);}
			if(node.getId() == nodePointer.getId()){
				if(parentNode == rootNode){
					// Node is a child node of root
					TreeNode node2Select = getPreviousNode(node);
					if(node2Select.getId() == 0){resetNodePointer();}else{setSelectedNode(node2Select);}
				}
				else{
					//Node is anywhere, but not a child node of root
					setSelectedNode(parentNode);
				}
			}

			removeChildNodes(node, false);

			for(int i = 0; i < parentNode.childNodes.size(); i++){
				if(parentNode.childNodes.get(i) == node.getId()){
					undoManager.recordNodeRemoved(node, i);
					parentNode.childNodes.remove(i);
					break;
				}
			}

			treeNodeDB.remove(node.getId());
			if(parentNode.getId() == 0){verifyThatTheTreeIsNotEmpty();}
			treeWasUpdated = true;
			undoManager.commitChanges();
		}
	}

	private synchronized void verifyThatTheTreeIsNotEmpty(){
		if(rootNode.countChildNodes() == 0){
			//All nodes are removed, the tree is empty - this is not allowed!
			addNode(createNode(de.akubix.keyminder.core.ApplicationInstance.APP_NAME), rootNode);
			resetNodePointer();
		}
	}

	@Override
	public synchronized void removeAllChildNodes(TreeNode node){
		removeChildNodes(node.getUnrestrictedAccess(), true);
		if(node.getId() == 0){verifyThatTheTreeIsNotEmpty();}
	}

	private synchronized boolean removeChildNodes(StandardNode node, boolean fireEvents){
		boolean resetNodePointerAfterOperation = false;
		boolean resetCalled = false;
		if(fireEvents){undoManager.captureMulitpleChanges();}

		for(int nodeid: node.childNodes){
			StandardNode stdNode = getNodeById(nodeid).getUnrestrictedAccess();
			if(fireEvents && enableFireEvents){
				// Notify UI and modules that the node will be removed
				app.fireEvent(EventTypes.TreeNodeEvent.OnNodeRemoved, stdNode);

				resetNodePointerAfterOperation = stdNode.getId() == nodePointer.getId();
			}
			else{
				if(stdNode.getId() == nodePointer.getId()){resetNodePointerAfterOperation = true;}
			}

			if(removeChildNodes(stdNode, false)){resetNodePointerAfterOperation = true;}

			undoManager.recordNodeRemoved(stdNode, 0);
			treeNodeDB.remove(stdNode.getId());

			// The nodePointer must be something else than null and always has to point to a valid node, if all else fails at least to the root node
			// If not now, the node pointer has to be corrected before this method will be exited
			if(fireEvents && enableFireEvents && resetNodePointerAfterOperation){
				 // "fireEvents" is false in every recursive call
				setSelectedNode(node);
				resetNodePointerAfterOperation = false;
				resetCalled = true;
			}
		}
		node.childNodes.clear();

		// Correct the node pointer to verify that it points to a valid node
		if(fireEvents && resetNodePointerAfterOperation){setSelectedNode(node); resetCalled = true;} // "fireEvents" is false in every recursive call

		treeWasUpdated = true;
		if(fireEvents){undoManager.commitChanges();}
		return fireEvents ? true : resetCalled;
	}

	@Override
	public synchronized TreeNode cloneTreeNode(TreeNode node2clone, boolean includeChildNodes){
		boolean undoWasEnabled = undoManager.isEnabled();
		undoManager.setEnable(false);
		node2clone.getTree().enableEventFireing(false);
		TreeNode ret = cloneDefaultNode(node2clone.getUnrestrictedAccess(), includeChildNodes);
		node2clone.getTree().enableEventFireing(true);
		undoManager.setEnable(undoWasEnabled);
		return ret;
	}

	// Note: Before using this method its indispensable to turn off event firing
	private synchronized TreeNode cloneDefaultNode(StandardNode node2clone, boolean includeChildNodes){
		StandardNode clonedNode = createDefaultNode(node2clone.getText());
		clonedNode.setColor(node2clone.getColor());
		de.akubix.keyminder.lib.Tools.hashCopy(node2clone.getUnrestrictedAccess().attributes, clonedNode.getUnrestrictedAccess().attributes);

		if(includeChildNodes){
			for(int i = 0; i < node2clone.countChildNodes(); i++){
				addNode(cloneDefaultNode(node2clone.getChildNodeByIndex(i).getUnrestrictedAccess(),  true), clonedNode);
			}
		}

		return clonedNode;
	}

	@Override
	public synchronized void moveNodeVertical(TreeNode node, int offSet){
		int nodeID = node.getId();
		boolean reselectNode = (node.getId() == nodePointer.getId());
		if(nodeID != 0) // Verify that the node is not the root node
		{
			StandardNode parentNode = node.getParentNode().getUnrestrictedAccess();
			int index = 0;
			for(int childnodeID: parentNode.childNodes){
				if(childnodeID == nodeID){break;}
				index++;
			}

			int newIndex = index + offSet;
			if(newIndex < 0){
				if(index == 0){return;} // Node is already at the beginning
				newIndex = 0;
			}
			else{
				int childNodeCnt = parentNode.countChildNodes();
				if(newIndex >= childNodeCnt){
					if(index == childNodeCnt){return;} // Node is already at the end
					newIndex = childNodeCnt -1;
				}
			}

			int tmp = parentNode.childNodes.get(newIndex);
			parentNode.childNodes.set(newIndex, parentNode.childNodes.get(index));
			parentNode.childNodes.set(index, tmp);
			undoManager.recordVerticalNodeMove(node, offSet * -1);
			app.fireEvent(TreeNodeEvent.OnNodeVerticallyMoved, node);

			if(reselectNode){setNodePointer(node);}

			treeWasUpdated = true;
		}
	}

	private StandardNode treeNodeClipboard = null;
	@Override
	public synchronized void copyNodeToInternalClipboard(TreeNode node){
		if(node.getId() != 0){ // It is not permitted to copy the root node
			enableEventFireing(false);
			if(treeNodeClipboard != null){
				// Nodes does not really contain to tree it's a relic of a copy action
				removeChildNodes(treeNodeClipboard.getUnrestrictedAccess(), false);
				treeNodeDB.remove(treeNodeClipboard.getId());
			}
			treeNodeClipboard = cloneDefaultNode(node.getUnrestrictedAccess(), true).getUnrestrictedAccess();
			enableEventFireing(true);
		}
	}

	@Override
	public synchronized boolean pasteNodeFromInternalClipboard(TreeNode parentNode){
		if(treeNodeClipboard != null){
			enableEventFireing(false);
			StandardNode tmp = cloneDefaultNode(treeNodeClipboard, true).getUnrestrictedAccess();
			enableEventFireing(true);

			parentNode.getTree().addNode(treeNodeClipboard, parentNode);
			treeNodeClipboard = tmp;
			return true;
		}
		else{
			return false;
		}
	}

	@Override
	public int getChildNodeIdByNodeText(String nodetext, TreeNode parentNode){
		int ret = -1;

		for(TreeNode n: parentNode.getChildNodes()){
			if(nodetext.equals(n.getText())){
				if(ret == -1){
					ret = n.getId();
				}
				else{
					return -2;
				}
			}
		}

		return ret;
	}

	@Override
	public String getNodePath(TreeNode node, String pathSeperator){
		if(node.getId() == 0){return "/" + node.getText();}
		TreeNode tmp = node;
		StringBuilder path = new StringBuilder(pathSeperator + node.getText());
		while(true){
			if(tmp.getParentNode().getId() != 0){
				tmp = tmp.getParentNode();
				path.insert(0, pathSeperator + tmp.getText());
			}
			else{
				return path.toString();
			}
		}
	}

	@Override
	public TreeNode getNodeByPath(String path){
		TreeNode node = (path.charAt(0) == '/' ? getRootNode() : getSelectedNode());
		for(String p: path.split("/")){
			if(p.equals("..")){
				if(node.getId() == 0){return null;} //This node is already the Root-Node...
				node = node.getParentNode();
			}
			else if(p.equals(".") || p.equals("")){
				continue;
			}
			else{
				boolean foundNode = false;
				for(TreeNode childNode: node.getChildNodes()){
					if(childNode.getText().equals(p)){node = childNode; foundNode = true; break;}
				}
				if(!foundNode){return null;}
			}
		}
		return node;
	}

	@Override
	public synchronized void sortChildNodes(TreeNode parentNode, boolean recursive){
		if(parentNode.countChildNodes() > 0)
		{
			Map<String, TreeNode> inverseList = new HashMap<>();
			parentNode.forEachChildNode((node) -> {
				String key = node.getText().toLowerCase();
				int ext = 0;
				while(inverseList.containsKey(key)){
					key = node.getText().toLowerCase() + ext;
					ext++;
				}

				inverseList.put(key, node);
			});

			List<String> sortedList = de.akubix.keyminder.lib.Tools.asSortedList(inverseList.keySet());

			StandardNode parentDefaultNode = parentNode.getUnrestrictedAccess();
			parentDefaultNode.childNodes.clear();
			sortedList.forEach((String str) -> parentDefaultNode.childNodes.add(inverseList.get(str).getId()));

			parentDefaultNode.getTree().setTreeChangedStatus(true);

			if(recursive){
				parentDefaultNode.forEachChildNode((node) -> {sortChildNodes(node, true);});
			}

			if(enableFireEvents){
				app.fireEvent(TreeNodeEvent.OnNodeVerticallyMoved, inverseList.get(sortedList.get(0)));
			}
		}
	}

	@Override
	public synchronized void allNodes(Consumer<? super TreeNode> lambda){
		allNodes(getRootNode(), lambda);
		//treeNodeDB.values().forEach((parentDefaultNode node) -> {TreeNode n = node; lambda.accept(n);}); //Faster, but some nodes in the has may currently not belong to the tree -> internal clipboard
	}

	private synchronized void allNodes(TreeNode parent, Consumer<? super TreeNode> lambda){
		parent.forEachChildNode((node) -> {lambda.accept(node); allNodes(node, lambda);});
	}

	/**
	 * This function will fix any conflicts that will occur if "loadNode()" has been used.
	 */
	public synchronized void verify(){
		//fix conflicts
		if(idConflicts < 0){
			if(de.akubix.keyminder.core.KeyMinder.verbose_mode){app.print(String.format("Fixing conflicts (%d id conflicts detected)... ", idConflicts * -1));}
			for(int i = -1; i >= idConflicts; i--){ //for each conflict
				StandardNode node = getNodeById(i).getUnrestrictedAccess();
				changeNodeId(node, ++idCounter); //Get the next free id for this node and update
			}
			idConflicts = 0;
			if(de.akubix.keyminder.core.KeyMinder.verbose_mode){app.print("done. \n");}
		}
	}

	private synchronized void changeNodeId(StandardNode node, int newId){
		int oldId = node.nodeID;
		node.nodeID = newId;

		treeNodeDB.put(newId, node);
		treeNodeDB.remove(oldId);

		// Update the reference in the child list of the parent node
		StandardNode parent = node.getParentNode().getUnrestrictedAccess();
		for(int j = 0; j < parent.countChildNodes(); j++){
			if(parent.childNodes.get(j) == oldId){
				parent.childNodes.set(j, node.nodeID);
				break;
			}
		}

		// Update the parent id of each child node
		node.childNodes.forEach((id) -> getNodeById(id).getUnrestrictedAccess().parentNodeID = node.nodeID);
		undoManager.clear(); // Some IDs mentioned in the undo history may changed: Its not save anymore to undo something
	}

	@Override
	public synchronized boolean undo(){
		return undoManager.undo();
	}

	@Override
	public void beginUpdate() {
		undoManager.captureMulitpleChanges();
	}

	@Override
	public void endUpdate() {
		undoManager.commitChanges();
	}
}
