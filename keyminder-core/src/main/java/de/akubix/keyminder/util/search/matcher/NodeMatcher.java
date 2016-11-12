package de.akubix.keyminder.util.search.matcher;

import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.util.search.NodeMatchResult;

public interface NodeMatcher {
	public NodeMatchResult matches(TreeNode treeNode);
}
