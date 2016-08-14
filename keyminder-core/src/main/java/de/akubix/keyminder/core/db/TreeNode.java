/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	TreeNode.java

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
package de.akubix.keyminder.core.db;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * This interface is currently just a mask to hide some methods of class "DefaultNode" which should be used very carefully.
 * A TreeNode is one of the most important parts of the core, because it stores all values of the tree in it.
 * Every TreeNode contains a text, a color and a various number of attributes that has been stored in a HashMap.
 *
 * @see StandardNode
 *
 */
public interface TreeNode {
	/**
	 * Returns the unique identifier of this TreeNode
	 * @return the id of the node
	 */
	public int getId();

	/**
	 * Returns the current index of the TreeNode.
	 * For example: The node is the second node below its parent, then the index will be "1" .
	 * @return the current index of this TreeNode.
	 */
	public int getIndex();

	/**
	 * Returns the current text of this node
	 * @return the text of this node
	 */
	public String getText();

	/**
	 * Applys a new text to this TreeNode
	 * @param text the new text
	 * @return the TreeNode itself (maybe you want to perform more actions in a single line)
	 */
	public TreeNode setText(String text);

	/**
	 * Returns the color that has been assigned to this node
	 * @return the color of this node
	 */
	public String getColor();

	/**
	 * Assigns another color to this TreeNode
	 * @param color the new color for this node (as HTML-Value)
	 * @return the TreeNode itself (maybe you want to perform more actions in a single line)
	 */
	public TreeNode setColor(String color);

	/**
	 * Returns all child nodes of this node
	 * @return an array of all child nodes
	 */
	public TreeNode[] getChildNodes();

	/**
	 * This method allows you to use a lambda expression to iterate over all child nodes
	 * @param lambda the lambda expression
	 */
	public void forEachChildNode(Consumer<? super TreeNode> lambda);

	/**
	 * Returns the child node at a specific index
	 * @param index the index of the child node
	 * @return the child node at this index
	 * @throws IndexOutOfBoundsException if the index is lower than zero or greater than the number of child nodes
	 */
	public TreeNode getChildNodeByIndex(int index) throws IndexOutOfBoundsException;

	/**
	 * Counts the number of child nodes that has been assigned to this node
	 * This number does not include any child node below these child nodes
	 * @return the number of child nodes
	 */
	public int countChildNodes();

	/**
	 * Returns the parent bode of this tree node
	 * @return the parent node
	 */
	public TreeNode getParentNode();

	/**
	 * Provides a direct access to the original BasicNode class without using a cast (currently this interface is just a "mask")
	 * Be very careful using the unrestricted access!
	 * @return the StandardNode behind this tree node
	 * @see StandardNode
	 */
	public StandardNode getUnrestrictedAccess();

	/**
	 * Returns the tree to which the node has been assigned
	 * @return the tree of this node
	 */
	public Tree getTree();

	/**
	 * Checks if the attribute hash contains a specified key
	 * @param name the name of the attribute
	 * @return {@code true} if the attribute exists, {@code false} if not
	 */
	public boolean hasAttribute(String name);

	/**
	 * Returns the value of an attribute of the this tree node
	 * @param name the name of this attribute
	 * @return the requested value or an empty string ("") if there is no attribute with this name
	 */
	public String getAttribute(String name);

	/**
	 * Stores a value in the attribute hash of this tree node
	 * @param name the name of the attribute
	 * @param value the value for this attribute
	 */
	public void setAttribute(String name, String value);

	/**
	 * Removes an attribute from the internal hash
	 * @param name the name of the attribute
	 */
	public void removeAttribute(String name);

	/**
	 * Returns a list (respectively a {@link Set}) of all attributes of this tree node
	 * @return the {@link Set} of attributes
	 */
	public Set<String> listAttributes();

	/**
	 *Returns a set with all node attributes
	 * @return
	 */
	public Set<Map.Entry<String, String>> getAttributeSet();
}
