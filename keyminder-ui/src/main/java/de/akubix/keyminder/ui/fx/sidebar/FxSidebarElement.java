package de.akubix.keyminder.ui.fx.sidebar;

import de.akubix.keyminder.core.tree.TreeNode;
import javafx.scene.Node;

public interface FxSidebarElement {
	/**
	 * Write another text to this UI element
	 * @param value the new text
	 */
	public void setUIValue(String value);

	/**
	 * Returns the text of this UI element
	 * @return the text of this element
	 */
	public String getUIValue();

	/**
	 * Loads all data you need from the attributes of this tree node
	 * @param node the tree node which contains the data
	 * @return {@code true} if the data has been loaded or {@code false} if the tree node doesn't contain the requested information
	 */
	public boolean loadData(TreeNode node);

	/**
	 * Writes all data of this UI element back to the attributes of the tree node
	 * @param node the tree node which will get the new data
	 */
	public void storeData(TreeNode node);

	/**
	 * This method must returns the root element of your side bar element group.
	 * The returned node will be added to the real side bar tab page.
	 * @return the root node of your elements (for example a {@link javafx.scene.layout.BorderPane})
	 */
	public Node getFxRootNode();
}
