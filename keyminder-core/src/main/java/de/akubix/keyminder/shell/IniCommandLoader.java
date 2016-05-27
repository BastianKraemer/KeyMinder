package de.akubix.keyminder.shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IniCommandLoader {

	/* This class is based on an example written by StackOverflow (stackoverflow.com) user Aerospace and is licensed
	 * under CC BY-SA 3.0 "Creative Commons Attribution-ShareAlike 3.0 Unported", http://creativecommons.org/licenses/by-sa/3.0/)
	 *
	 * Source: http://stackoverflow.com/questions/190629/what-is-the-easiest-way-to-parse-an-ini-file-in-java
	 * The code has been modified.
	 */

	private Pattern sectionPattern  = Pattern.compile( "\\s*\\[([^]]*)\\]\\s*" );
	private Pattern keyValuePattern = Pattern.compile( "\\s*([^=]*)=(.*)" );

	public void load(String resourcePath, Shell shell) throws IOException {
		ClassLoader classLoader = shell.getClass().getClassLoader();
		try(BufferedReader br = new BufferedReader(
	 		  new InputStreamReader(getClass().getResourceAsStream(resourcePath)))){
			String line;
			String section = "";
			boolean aliasSection = false;
			while((line = br.readLine()) != null) {
				Matcher m = sectionPattern.matcher(line);
				if(m.matches()) {
					section = m.group(1).trim();
					aliasSection = section.toLowerCase().equals("alias");
					if(!aliasSection){
						section += ".";
					}
				}
				else{
					m = keyValuePattern.matcher(line);
					if(m.matches()){
						if(!aliasSection){
							shell.addCommand(m.group(1).trim(), section + m.group(2).trim(), classLoader);
						}
						else{
							shell.addAlias(m.group(1).trim(), m.group(2).trim());
						}
					}
				}
			}
		}
	}
}
