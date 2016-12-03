/* KeyMinder
 * Copyright (C) 2016 Bastian Kraemer
 *
 * TreeNodeItem.java
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
package de.akubix.keyminder.ui.fx.components;

import de.akubix.keyminder.core.tree.TreeNode;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;

public class TreeNodeItem extends TreeItem<TreeNodeReference> {

	public TreeNodeItem(TreeNodeReference ref) {
		super(ref);

		this.expandedProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
			expandChanged(newValue);
		});

		setExpanded(ref.getTreeStore().getNodeById(ref.getNodeId()).isExpanded());
	}

	private void expandChanged(boolean value) {
		TreeNode node = getValue().getTreeNode();
		if(node != null){
			node.setExpanded(value);
		}
	}
}
