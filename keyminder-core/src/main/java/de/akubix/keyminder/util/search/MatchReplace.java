package de.akubix.keyminder.util.search;

import de.akubix.keyminder.util.search.NodeMatchResult.MatchElement;
import de.akubix.keyminder.util.search.matcher.TextMatcher;

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
	public static boolean replaceContent(NodeMatchResult matchResult, String replacement, boolean allowRegularExpression) throws IllegalArgumentException {

		try{
			boolean anyReplacePerformed = false;

			if(!allowRegularExpression){
				replacement = TextMatcher.regularExpressionSpecialCharacterEscaping(replacement);
			}

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
		catch(Exception ex){
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
	}
}
