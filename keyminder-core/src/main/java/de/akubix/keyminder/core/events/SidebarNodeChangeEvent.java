package de.akubix.keyminder.core.events;

import de.akubix.keyminder.core.db.TreeNode;

public interface SidebarNodeChangeEvent {
	/**
	 * This interface has to be implemented by every side bar panel.
	 * You'll get the currently selected node and have to return {@code false} if your panel is empty, or "true" if not.
	 * @param selectedNode the currently selected tree node
	 * @return {@code false} if your panel is empty, or {@code true} if not.
	 */
	public boolean selectedNodeChanged(TreeNode selectedNode);
}
