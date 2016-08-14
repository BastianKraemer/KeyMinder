package de.akubix.keyminder.util.search;

import de.akubix.keyminder.util.search.NodeMatchResult.MatchElement;

public final class MatchReplace {

	private MatchReplace(){
		super();
	}

	/**
	 * Replaces (or substitutes) the text of the node by another one
	 * @param node the tree node which contains the text that should be replaced
	 * @param NodeMatchResult the match result
	 * @param replacement the string that will be the replacement
	 * @return {@code true} if there has been anything replaced, {@code false} if not
	 */
	public static boolean simpleReplace(NodeMatchResult matchResult, String replacement) throws IllegalArgumentException {

		boolean anyReplacePerformed = false;

		if(matchResult.nodeMatches()){
			for(MatchElement match: matchResult.getMatchElements()){
				if(match.hasMatcher()){
					String newValue = match.getMatcher().replaceAll(replacement);

					matchResult.getNode().getTree().beginUpdate();
					anyReplacePerformed = true;
					if(match.isTextMatch()){
						matchResult.getNode().setText(newValue);
					}
					else{
						matchResult.getNode().setAttribute(match.getAttributeName(), newValue);
					}
				}
			}
		}

		matchResult.getNode().getTree().endUpdate();
		return anyReplacePerformed;
	}
}
