package de.akubix.keyminder.script;

import java.util.regex.Pattern;

import jdk.nashorn.api.scripting.ClassFilter;

@SuppressWarnings("restriction")
class ScriptClassFilter implements ClassFilter {

	private Pattern classPattern = Pattern.compile("^java\\.lang\\..+$");

	@Override
	public boolean exposeToScripts(String s) {
		return classPattern.matcher(s).matches();
	}
}
