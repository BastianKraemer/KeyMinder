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
	public static boolean replaceContent(final NodeMatchResult matchResult, final String replacement, final boolean allowRegularExpression) throws IllegalArgumentException {

		try{
			boolean anyReplacePerformed[] = {false};
			final String replacementStr = allowRegularExpression ? replacement : TextMatcher.regularExpressionSpecialCharacterEscaping(replacement);

			matchResult.getNode().getTree().transaction(() -> {

				if(matchResult.nodeMatches()){
					for(MatchElement match: matchResult.getMatchElements()){
						if(match.hasMatcher()){
							String newValue = match.getMatcher().replaceAll(replacementStr);

							anyReplacePerformed[0] = true;
							if(match.isTextMatch()){
								matchResult.getNode().setText(newValue);
							}
							else{
								matchResult.getNode().setAttribute(match.getAttributeName(), newValue);
							}
						}
					}
				}
			});

			return anyReplacePerformed[0];
		}
		catch(Exception ex){
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
	}
}
