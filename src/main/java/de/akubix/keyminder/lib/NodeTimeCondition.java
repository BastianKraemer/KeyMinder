/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	NodeTimeCondition.java

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.akubix.keyminder.lib;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * This class allows you to create rules to search for nodes observing creation or modification date
 * In the future maybe there will be an interface for this to provide more search conditions
 */
public class NodeTimeCondition {
	private Instant referenceDate;
	private long milliSeconds;
	private CompareType compareType;
	private String attributeName;
	
	/**
	 * Create a new time condition
	 * @param attributeName Should be "created" or "modified"
	 * @param compareType created/modified before or after this date?
	 * @param date the date you want to compare to
	 */
	public NodeTimeCondition(String attributeName, CompareType compareType, Instant date)
	{
		this.referenceDate = date;
		this.milliSeconds = date.getEpochSecond() * 1000;
		this.compareType = compareType;
		this.attributeName = attributeName;
	}
	
	/**
	 * Create a new time condition
	 * @param attributeName Should be "created" or "modified"
	 * @param compareType created/modified before or after this date?
	 * @param date the date you want to compare to
	 * @throws ParseException if the entered date is not a valid date
	 */
	public NodeTimeCondition(String attributeName, CompareType compareType, String date) throws ParseException
	{
		this(attributeName, compareType, new SimpleDateFormat("dd.MM.yy").parse(date).toInstant());
	}

	/**
	 * Compare this condition with a tree node
	 * @param node the tree node
	 * @return {@code true} if the condition matches, {@code false} if not
	 */
	public boolean compareTo(de.akubix.keyminder.core.db.TreeNode node)
	{
		try
		{	
			if(compareType == CompareType.Before)
			{
				return Long.parseLong(node.getAttribute(attributeName)) <= milliSeconds;
			}
			else if(compareType == CompareType.After)
			{
				return Long.parseLong(node.getAttribute(attributeName)) >= milliSeconds;
			}
			else
			{
				// At same Day
				Instant nodeTime = Instant.ofEpochMilli(Long.parseLong(node.getAttribute(attributeName)));
				ZonedDateTime refzdt = referenceDate.atZone((ZoneId.systemDefault()));
				ZonedDateTime zdt = nodeTime.atZone((ZoneId.systemDefault()));
				
				return (refzdt.getYear() == zdt.getYear() && refzdt.getDayOfYear() == zdt.getDayOfYear());	
			}
		}
		catch (NumberFormatException | DateTimeException ex)
		{
			return false;
		}
	}
	
	enum CompareType {
		Before, After, AtSameDay
	}
	
	/**
	 * Returns a compare type selected by a given string
	 * @param str should be "before", "after" or "at
	 * @return Your CompareType variable
	 * @throws IllegalArgumentException if the given string is not "after", "before" or "at"
	 */
	public static CompareType getCompareTypeFromString(String str) throws IllegalArgumentException
	{
		switch(str.toLowerCase())
		{
		case "before":
		case "vor":
			return CompareType.Before;
		case "after":
		case "nach":
			return CompareType.After;
		case "at":
		case "atsameday":
		case "am":
			return CompareType.AtSameDay;
		}
		
		throw new IllegalArgumentException("Invalid compare type.");
	}
	
}
