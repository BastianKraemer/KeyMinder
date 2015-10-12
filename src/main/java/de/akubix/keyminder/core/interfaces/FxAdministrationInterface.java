package de.akubix.keyminder.core.interfaces;

import de.akubix.keyminder.core.db.TreeNode;

public interface FxAdministrationInterface extends FxUserInterface{
	public void setTitle(String title);
	
	public void buildTree();
	public void updateTree();
	public void displayNewTreePart(TreeNode newNode);
	public void rebuildTreePart(TreeNode parentNode);

	public void onFileOpenedHandler();

	public TreeNode getSelectedTreeNode();

	public void buildFavoriteNodeList(int[] favoriteNodes);
}
