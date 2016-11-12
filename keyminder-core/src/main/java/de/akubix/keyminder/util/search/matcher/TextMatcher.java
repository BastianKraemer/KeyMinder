package de.akubix.keyminder.util.search.matcher;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.util.search.NodeMatchResult;

public class TextMatcher implements NodeMatcher {

	private Pattern pattern;
	private Pattern attributeRegExFilter;
	private NodeMatcherOption option;

	public TextMatcher(String regEx, boolean simpleSearching, boolean ignoreCase) throws PatternSyntaxException {
		this(regEx, simpleSearching, NodeMatcherOption.ALL, null, ignoreCase);
	}

	public TextMatcher(String regEx, boolean simpleSearching, NodeMatcherOption option, boolean ignoreCase) throws PatternSyntaxException {

		this(regEx, simpleSearching, option, null, ignoreCase);
	}

	public TextMatcher(String regEx, boolean simpleSearching, NodeMatcherOption option, Pattern attributeRegExFilter, boolean ignoreCase) throws PatternSyntaxException {

		if(simpleSearching){
			regEx = ".*" + regularExpressionSpecialCharacterEscaping(regEx) + ".*";
		}

		if(ignoreCase){
			regEx = "(?i)" + regEx;
		}

		this.attributeRegExFilter = attributeRegExFilter;
		this.pattern = Pattern.compile(regEx);
		this.option = option;
	}

	@Override
	public NodeMatchResult matches(TreeNode node) {

		NodeMatchResult result = null;
		Matcher m;

		if(this.option != NodeMatcherOption.ATTRIBUTES_ONLY){
			m = pattern.matcher(node.getText());
			if(m.matches()){
				if(result == null){result = new NodeMatchResult(node);}
				result.addTextMatch(m);
			}
		}

		if(this.option != NodeMatcherOption.TEXT_ONLY){;
			for(Map.Entry<String, String> attrib: node.getAttributes()){
				if(attributeRegExFilter == null || attributeRegExFilter.matcher(attrib.getKey()).matches()){
					m = pattern.matcher(attrib.getValue());
					if(m.matches()){
						if(result == null){result = new NodeMatchResult(node);}
						result.addAttributeMatch(attrib.getKey(), m);
					}
				}
			}
		}

		return result == null ? NodeMatchResult.noMatch() : result;
	}

	public static String regularExpressionSpecialCharacterEscaping(String str){
		return str.replaceAll("(\\.|\\[|\\]|\\(|\\)|\\{|\\}|\\^|\\$|\\?|\\+|\\-|\\\\)", "\\\\$0").replace("*", ".*");
	}

	public static enum NodeMatcherOption {
		ALL, TEXT_ONLY, ATTRIBUTES_ONLY
	}
}
