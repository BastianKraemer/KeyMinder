/* KeyMinder
 * Copyright (C) 2015-2016 Bastian Kraemer
 *
 * Utilities.java
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.akubix.keyminder.util;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.util.Pair;

/**
 * This class is just a collection of some useful methods
 */
public final class Utilities {

	private Utilities(){}

	public static <T1, T2> void hashCopy(Map<T1, T2> source, Map<T1, T2> target){
		target.clear();
		for(T1 key: source.keySet()){
			target.put(key, source.get(key));
		}
	}

	/* The following method/function was written by StackOverflow (stackoverflow.com) user Umesh Awasthi and is licensed under CC BY-SA 3.0
	 * "Creative Commons Attribution-ShareAlike 3.0 Unported", http://creativecommons.org/licenses/by-sa/3.0/)
	 *
	 * Source: http://stackoverflow.com/questions/4165832/sorting-values-of-set
	 * The code has not been modified.
	 */

	public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
		  List<T> list = new ArrayList<>(c);
		  Collections.sort(list);
		  return list;
	}

	public static String getFileExtension(String absoluteFileName){
		if(absoluteFileName.contains(System.getProperty("file.separator"))){
			absoluteFileName = absoluteFileName.substring(absoluteFileName.lastIndexOf(System.getProperty("file.separator")) + 1);
		}

		if(absoluteFileName.contains(".")){
			return absoluteFileName.substring(absoluteFileName.lastIndexOf("."));
		}
		else{
			return "";
		}
	}

	public static Process runProcess(List<String> commands) throws IOException {
		 ProcessBuilder pb = new ProcessBuilder(commands);
		 Process p = pb.start();
		 return p;
	}

	public static boolean isYes(String s){
		return s.matches("(?i)^ *(true|yes|ja|j|y|1) *$");
	}

	/**
	 * Converts a number of milliseconds since 1970 to an regular date
	 * @param value The number of milliseconds as string
	 * @param shortFormat Controls the output format: "dd. MMMM yyyy, HH:mm" (false) or "dd.MM.yy HH:mm" (true)
	 * @param errorText The string the should be returned if the conversion fails
	 * @return The date as string with the selected format or the text specified by "errorText"
	 */
	public static String getTimeFromEpochMilli(String value, boolean shortFormat, String errorText){
		try{
			return Instant.ofEpochMilli(Long.parseLong(value)).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(shortFormat ? "dd.MM.yy HH:mm" : "dd. MMMM yyyy, HH:mm"));
		}
		catch(NumberFormatException | DateTimeException ex){
			return errorText;
		}
	}

	/**
	 * This method splits a string like {@code 'key=value'}  into a {@link Pair}.
	 * @param input The input string
	 * @param keyRegEx The regular expression for the key (do not use any match groups)
	 * @param valueRegEx The regular expression for the value (do not use any match groups)
	 * @param separatorRegEx The separator as regular expression
	 * @return
	 * @throws IllegalArgumentException if the input value does not match with the regular expression
	 */
	public static Pair<String, String> splitKeyAndValue(String input, String keyRegEx,  String separatorRegEx, String valueRegEx) throws IllegalArgumentException {
		Pattern p = Pattern.compile(String.format("^(%s) *%s *(%s)$", keyRegEx, separatorRegEx, valueRegEx));
		Matcher matcher = p.matcher(input);

		if(!matcher.matches()){
			throw new IllegalArgumentException(String.format("The input value does not match the following pattern: %s%s%s", keyRegEx, separatorRegEx, valueRegEx));
		}

		return new Pair<>(matcher.group(1), matcher.group(2));
	}
}
