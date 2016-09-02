package de.akubix.keyminder.ui.fx.components;

import de.akubix.keyminder.core.db.TreeNode;
import javafx.scene.control.TreeView;

/*
 * ======================================================================================================================================================
 * A TreeView-Skin to check if a node is visible on screen
 * ======================================================================================================================================================
 */

/* The following code was written by StackOverflow (stackoverflow.com) user Ahmed and is licensed under CC BY-SA 3.0
 * "Creative Commons Attribution-ShareAlike 3.0 Unported", http://creativecommons.org/licenses/by-sa/3.0/)
 *
 * Source: http://stackoverflow.com/questions/27059701/javafx-in-treeview-need-only-scroll-to-index-number-when-treeitem-is-out-of-vie
 * The code has not been modified.
 */

/**
 * Only done as a workaround. If the selected node changes (maybe because of a "cd" command) the tree view might not scroll to it.
 *
 * WARNING: This method relies on classes, which does not contain to the Java API
 */
@SuppressWarnings("restriction")
public final class VisibleTreeNodesSkin extends com.sun.javafx.scene.control.skin.TreeViewSkin<TreeNode> {
	public VisibleTreeNodesSkin(TreeView<TreeNode> treeView){
		super(treeView);
	}

	public boolean isIndexVisible(int index){
		if (flow.getFirstVisibleCell() != null &&
			flow.getLastVisibleCell() != null &&
			flow.getFirstVisibleCell().getIndex() <= index &&
			flow.getLastVisibleCell().getIndex() >= index){
			return true;
		}
		return false;
	}
}
