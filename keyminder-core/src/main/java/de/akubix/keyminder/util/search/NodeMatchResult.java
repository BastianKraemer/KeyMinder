package de.akubix.keyminder.util.search;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import de.akubix.keyminder.core.tree.TreeNode;

public class NodeMatchResult {
	private TreeNode node;
	private List<MatchElement> matchList;

	public NodeMatchResult(){
		this(null);
	}

	public NodeMatchResult(TreeNode node){
		this.node = node;
		this.matchList = new ArrayList<>();
	}

	public NodeMatchResult addTextMatch(Matcher matcher){
		matchList.add(new MatchElement(matcher));
		return this;
	}

	public NodeMatchResult addAttributeMatch(String attributeName, Matcher matcher){
		matchList.add(new MatchElement(attributeName, matcher));
		return this;
	}

	/**
	 * @return Specifies whether the node matches or not
	 */
	public boolean nodeMatches(){
		return this.node != null;
	}

	/**
	 * @return the node
	 */
	public TreeNode getNode() {
		return node;
	}

	/**
	 * @return the matcher
	 */
	public List<MatchElement> getMatchElements() {
		return this.matchList;
	}

	/**
	 * Creates a {@link NodeMatchResult} that represents 'no match'
	 * @return
	 */
	public static NodeMatchResult noMatch(){
		return new NodeMatchResult();
	}

	public static final class MatchElement {
		private final Matcher matcher;
		private final String attribName;
		private final boolean isTextMatch;

		private MatchElement(String attribName, Matcher matcher){
			super();
			this.attribName = attribName;
			this.matcher = matcher;
			this.isTextMatch = false;
		}

		private MatchElement(Matcher matcher){
			super();
			this.attribName = null;
			this.matcher = matcher;
			this.isTextMatch = true;
		}

		/**
		 * @return the matcher
		 */
		public Matcher getMatcher() {
			return matcher;
		}

		/**
		 * @return the attribName
		 */
		public String getAttributeName() {
			return attribName;
		}

		/**
		 * @return the isTextMatch
		 */
		public boolean isTextMatch() {
			return isTextMatch;
		}

		/**
		 * @return Specifies if the matchResult contains a {@link Matcher}
		 */
		public boolean hasMatcher(){
			return matcher != null;
		}
	}
}
