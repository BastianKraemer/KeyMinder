package de.akubix.keyminder.util.search.matcher;

import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.util.search.NodeMatchResult;

public interface NodeMatcher {
	public NodeMatchResult matches(TreeNode treeNode);
}
