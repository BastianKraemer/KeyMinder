/* KeyMinder
 * Copyright (C) 2015-2016 Bastian Kraemer
 *
 * TreeStore.java
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.function.Consumer;

import javax.xml.bind.DatatypeConverter;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.events.EventTypes;
import de.akubix.keyminder.core.events.EventTypes.TreeNodeEvent;

/**
 * This class is the core of the whole database. All data stored in this applications will be assigned to the HashMap {@link TreeStore#treeNodeMap} in this class
 * @see TreeStore
 * @see TreeNode
 */
public class TreeStore {

	private static final int DEFAULT_IDENTIFIER_LENGTH_IN_BYTES = 6;

	private static final String SETTINGS_KEY_MAX_UNDO_HISTORY =  "tree.undo.limit";
	private static final int DEFAULT_MAX_UNDO_HINSTORY_SIZE = 20;

	static final String ROOT_NODE_IDENTIFIER = "0";

	private ApplicationInstance app;
	private HashMap<String, TreeNode> treeNodeMap = new HashMap<>();
	private TreeNode nodePointer;
	private Random random;
	private boolean hasUnsavedChanges = false;
	private boolean enableEvents = false;
	private boolean enableNodeTimestamps = false;

	private boolean enableUndo = false;
	private UndoBuilder undoBuilder;
	private LinkedList<UndoStep> undoHistory;
	private int maxUndoHistorySize = DEFAULT_MAX_UNDO_HINSTORY_SIZE;

	public TreeStore(ApplicationInstance instance){
		app = instance;
		random = new Random();
		createRoot();
		undoHistory = new LinkedList<>();
		reloadConfig();
	}

	public void reloadConfig(){
		try{
			if(app.settingsContainsKey(SETTINGS_KEY_MAX_UNDO_HISTORY)){
				maxUndoHistorySize = Integer.parseInt(app.getSettingsValue(SETTINGS_KEY_MAX_UNDO_HISTORY));
			}
		}
		catch(NumberFormatException e){
			maxUndoHistorySize = DEFAULT_MAX_UNDO_HINSTORY_SIZE;
			app.alert(String.format("Invalid value for setting '%s' (Integer): '%s'", SETTINGS_KEY_MAX_UNDO_HISTORY, app.getSettingsValue(SETTINGS_KEY_MAX_UNDO_HISTORY)));
		}
	}

	private void createRoot(){
		synchronized (treeNodeMap) {
			TreeNode rootNode = new DefaultTreeNode("root");
			rootNode.setExpanded(true);
			restoreNode(rootNode, "0");
			rootNode.setTree(this);
			treeNodeMap.put("0", rootNode);
			nodePointer = rootNode;
		}
	}

	public static final TreeNode restoreNode(TreeNode node, String identifier){

		if(node.getId() == null){
			node.setId(identifier);
			return node;
		}

		throw new IllegalStateException("Another id is already assigned to the tree node.");
	}

	final void registerNode(TreeNode node) throws IllegalStateException {
		registerNode(node, false);
	}

	final void registerNode(TreeNode node, boolean forceOverwrite) throws IllegalStateException {
		synchronized (treeNodeMap) {

			if(node.getTree() == null){
				String id;
				if(node.getId() == null){
					id = generateIdentifier(node, DEFAULT_IDENTIFIER_LENGTH_IN_BYTES);
					node.setId(id);
				}
				else{
					if(treeNodeMap.containsKey(node.getId()) && !forceOverwrite){
						throw new IllegalStateException("The node identifier is already in use.");
					}

					id = node.getId();
				}

				node.setTree(this);
				treeNodeMap.put(id, node);

				if(enableNodeTimestamps && node.canHaveAttributes()){
					if(!node.hasAttribute(ApplicationInstance.NODE_ATTRIBUTE_CREATION_DATE)){
						node.setAttribute(ApplicationInstance.NODE_ATTRIBUTE_CREATION_DATE, Long.toString(System.currentTimeMillis()), true);
					}

					if(!node.hasAttribute(ApplicationInstance.NODE_ATTRIBUTE_MODIFICATION_DATE)){
						node.setAttribute(ApplicationInstance.NODE_ATTRIBUTE_MODIFICATION_DATE, Long.toString(System.currentTimeMillis()), true);
					}
				}

			}
			else {
				throw new IllegalStateException("The tree node is already assigned to the tree.");
			}

		}
	}

	final void unregisterNode(TreeNode node){
		synchronized (treeNodeMap) {
			node.forEachChildNode(childNode -> childNode.unregister());

			if(this.nodePointer == node){
				moveNodePointerUpwards();
			}

			treeNodeMap.remove(node.getId());
		}
	}

	final void verifyNotEmptyTree(){
		if(getRootNode().countChildNodes() == 0){
			TreeNode newNode = new DefaultTreeNode(ApplicationInstance.APP_NAME);
			getRootNode().addChildNode(newNode);
			setSelectedNode(newNode);
		}
	}

	private String generateIdentifier(final TreeNode treeNode, final int length){

		byte[] generatedId = new byte[length];
		this.random.nextBytes(generatedId);

		// This is something like a name space
		// Generated tree nodes have the prefix '02', regular node a '01'
		generatedId[0] = (byte) (treeNode.isGenerated() ? 2 : 1);

		String identifier = DatatypeConverter.printHexBinary(generatedId);

		if(treeNodeMap.containsKey(identifier)){
			return generateIdentifier(treeNode, length + 1);
		}

		return identifier;
	}

	public void reset(){

		synchronized (treeNodeMap) {
			treeNodeMap.clear();
			System.gc();
			createRoot();

			hasUnsavedChanges = false;
			enableEvents = false;
			enableNodeTimestamps = false;
			undoHistory = new LinkedList<>();
		}
	}

	public boolean hasUnsavedChanges(){
		return hasUnsavedChanges;
	}

	public void setTreeChangedStatus(boolean value){
		synchronized (treeNodeMap) {
			hasUnsavedChanges = value;
		}
	}

	public void enableNodeTimestamps(boolean value){
		enableNodeTimestamps = value;
	}

	public boolean areNodeTimestampsEnabled(){
		return enableNodeTimestamps;
	}

	public void enableEvents(boolean value){
		synchronized (treeNodeMap) {
			this.enableEvents = value;
		}
	}

	public boolean areEventsEnabled(){
		return enableEvents;
	}

	final void fireNodeEvent(TreeNode node, TreeNodeEvent event){
		if(enableEvents){
			synchronized (treeNodeMap) {
				app.fireEvent(event, node);
			}
		}
	}

	public int countAllNodes(){
		return treeNodeMap.size();
	}


	public boolean nodeExists(int nodeId){
		return treeNodeMap.containsKey(nodeId);
	}


	public TreeNode getNodeById(String id){
		return treeNodeMap.get(id);
	}

	public TreeNode getSelectedNode(){
		return nodePointer;
	}

	public synchronized void setSelectedNode(TreeNode node){
		if(node.getId() != nodePointer.getId()){
			setNodePointer(node);
		}
	}

	// The functionality "setSelectedNode" has been split into two methods, because there has to be a private "setNodePointer()" method which will force firing an event, even if it is the same node
	protected synchronized void setNodePointer(TreeNode node){
		if(node != null){
			nodePointer = node;
			if(enableEvents){app.fireEvent(EventTypes.TreeNodeEvent.OnSelectedItemChanged, node);}
		}
	}

	private void moveNodePointerUpwards(){
		if(nodePointer.isRootNode()){return;}

		TreeNode parentNode = nodePointer.getParentNode();
		if(parentNode.isRootNode()){

			if(getRootNode().countChildNodes() <= 1){
				setSelectedNode(getNodeById(ROOT_NODE_IDENTIFIER));
				return;
			}

			// Node is a child node of root
			TreeNode prevNode = getPreviousNode(nodePointer);
			if(prevNode.isRootNode()){
				resetNodePointer();
			}
			else{
				setSelectedNode(prevNode);
			}
		}
		else {
			final int index = nodePointer.getIndex();
			final int nodeCount = getRootNode().countChildNodes();
			if(nodeCount > 1){
				setSelectedNode(getRootNode().getChildNodeByIndex(index == 0 ? 1 : index - 1));
			}
			else{
				resetNodePointer();
			}
		}
	}

	/**
	 * Reset the NodePointer to its default position, this is the first node below the root node, or the root node if the tree is empty
	 */
	public synchronized void resetNodePointer(){
		if(getRootNode().countChildNodes() > 0){
			if(treeNodeMap.containsKey(getRootNode().getChildNodeByIndex(0).getId())){
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


	public TreeNode getRootNode(){
		return getNodeById(ROOT_NODE_IDENTIFIER);
	}

	public TreeNode getNextNode(TreeNode node){
		TreeNode x = getNextNode(node, 1);
		return x;
	}

	public TreeNode getPreviousNode(TreeNode node){
		TreeNode x = getNextNode(node, -1);
		return x;
	}

	private synchronized TreeNode getNextNode(TreeNode node, int offset){
		if(!node.isRootNode()){
			try{
				int i = node.getIndex();
				TreeNode parentNode = node.getParentNode();
				if(i + offset < 0){
					return parentNode.isRootNode() ? node : parentNode;
				}

				if(parentNode.countChildNodes() > i + offset){
					return node.getParentNode().getChildNodeByIndex(i + offset);
				}
				else{
					return getNextNode(parentNode, 1);
				}
			}
			catch(IndexOutOfBoundsException ex){
				return (!node.getParentNode().isRootNode() ? getNextNode(node.getParentNode(), 1) : node);
			}
		}
		else{
			return node;
		}
	}

	public List<TreeNode> exportNodeStructure(TreeNode node){

		List<TreeNode> export = new ArrayList<>();
		export.add(node.clone(CloneMode.WITHOUT_PARENT));
		node.forEachChildNode((childNode) -> exportNodeSubStructure(childNode, export));
		return export;
	}

	private void exportNodeSubStructure(TreeNode node, List<TreeNode> exportList){
		exportList.add(node.clone());
		node.forEachChildNode((childNode) -> exportNodeSubStructure(childNode, exportList));
	}

	public void importNodeStructure(TreeNode defaultParentNode, List<TreeNode> importList){
		importNodeStructure(defaultParentNode, importList, -1);
	}

	public void importNodeStructure(TreeNode defaultParentNode, List<TreeNode> importList, int insertAt){

		final Map<String, String> idTranslationMap = new HashMap<>();
		final List<TreeNode> importedNodes = new ArrayList<>(importList.size());

		for(TreeNode node: importList){
			TreeNode clone = node.clone(CloneMode.NODE_ONLY);

			String parentNodeIdentifier = node.getParentNodeIdentifier();

			if(treeNodeMap.containsKey(clone.getId())){
				// The id is already in use, a new one must be generated
				String oldId = clone.getId();
				String newId = generateIdentifier(clone, DEFAULT_IDENTIFIER_LENGTH_IN_BYTES);
				clone.setId(newId);
				idTranslationMap.put(oldId, newId);
			}

			if(parentNodeIdentifier == null){
				if(insertAt >= 0){
					defaultParentNode.insertChildNode(clone, insertAt++);
				}
				else{
					defaultParentNode.addChildNode(clone);
				}
			}
			else{
				if(idTranslationMap.containsKey(parentNodeIdentifier)){
					parentNodeIdentifier = idTranslationMap.get(parentNodeIdentifier);
				}

				TreeNode parentNode = getNodeById(parentNodeIdentifier);
				if(parentNode == null){
					parentNode = defaultParentNode;
					app.alert(String.format("Unable to locate parent node of '%s'. The identifier '%s' is not referenced to a node.", clone.getText(), parentNodeIdentifier));
				}
				parentNode.addChildNode(clone);
			}

			importedNodes.add(clone);
		}

		for(TreeNode node: importedNodes){
			node.getAttributes().forEach((entry) -> {
				if(idTranslationMap.containsKey(entry.getValue())){
					node.setAttribute(entry.getKey(), idTranslationMap.get(entry.getValue()));
				}
			});
		}
	}

	public TreeNode getNodeByPath(String path){
		TreeNode node = (path.charAt(0) == '/' ? getRootNode() : getSelectedNode());
		for(String p: path.split("/")){
			if(p.equals("..")){
				if(node.isRootNode()){return getRootNode();} //This node is already the Root-Node...
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

	public synchronized void allNodes(Consumer<? super TreeNode> lambda){
		allNodes(getRootNode(), lambda);
	}

	private synchronized void allNodes(TreeNode parent, Consumer<? super TreeNode> lambda){
		parent.forEachChildNode((node) -> {lambda.accept(node); allNodes(node, lambda);});
	}

	final void captureNodeState(UndoStep step){
		synchronized (undoHistory) {
			if(undoBuilder == null && !step.getChangedNodesMap().isEmpty() && isUndoEnabled()){
				undoHistory.add(step);

				if(undoHistory.size() > maxUndoHistorySize){
					undoHistory.removeFirst();
				}
			}
		}
	}

	public final void enableUndo(boolean value){
		this.enableUndo = value;
		if(value == false){
			this.undoHistory.clear();
			this.undoBuilder = null;
		}
	}

	public final boolean isUndoEnabled(){
		return enableUndo;
	}

	final UndoBuilder getUndoBuilder(){
		return undoBuilder == null ? new UndoBuilder(this) : undoBuilder;
	}

	public final void transaction(Runnable transaction){
		transaction(transaction, null);
	}

	public final void transaction(Runnable transaction, Runnable postUndoActions){
		synchronized (treeNodeMap) {
			this.undoBuilder = new UndoBuilder(this);

			transaction.run();

			final UndoBuilder undoData = this.undoBuilder;
			this.undoBuilder = null;
			undoData.commit(postUndoActions);
		}
	}

	public final boolean undo(boolean selectChangedNode){
		synchronized (treeNodeMap) {
			synchronized (undoHistory) {
				if(undoHistory.size() > 0){
					undo(undoHistory.getLast(), selectChangedNode);
					undoHistory.getLast().clear();
					undoHistory.removeLast();
					return true;
				}
			}
		}
		return false;
	}

	private void undo(UndoStep step, boolean selectChangedNode){

		final boolean eventsEnabled = this.areEventsEnabled();
		final boolean undoEnabled = this.isUndoEnabled();
		this.enableEvents(false);
		this.enableUndo = false;

		final LinkedList<TreeNode> nodeResetEvents = new LinkedList<>();

		for(Entry<String, TreeNode> entry: step.getChangedNodesMap().entrySet()){
			if(entry.getValue() == null){
				getNodeById(entry.getKey()).remove();
			}
			else{
				registerNode(entry.getValue(), true);
				nodeResetEvents.add(entry.getValue());
			}
		}

		step.runPostUndoActions();

		TreeNode newSelectedNode = getSelectedNode();
		if(newSelectedNode != null){
			newSelectedNode = getNodeById(newSelectedNode.getId());
		}

		// First the node point must be set to a valid TreeNode reference (maybe the node was updated)
		if(newSelectedNode != null){
			setNodePointer(newSelectedNode);
		}
		else{
			resetNodePointer();
		}

		if(eventsEnabled){
			this.enableEvents(eventsEnabled);

			nodeResetEvents.stream().filter((node) -> !node.isRootNode()).forEach((node) -> {
				fireNodeEvent(node, TreeNodeEvent.OnNodeReset);
			});

			// Now fire the node select event
			if(selectChangedNode && nodeResetEvents.size() > 0){
				setNodePointer(nodeResetEvents.getFirst());
			}
			else{
				setNodePointer(newSelectedNode);
			}
		}

		this.enableUndo = undoEnabled;
	}
}
