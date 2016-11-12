/*	KeyMinder
 * Copyright (C) 2015-2016 Bastian Kraemer
 *
 * DefaultTreeNode.java
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
package de.akubix.keyminder.core.tree;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.akubix.keyminder.util.Utilities;

public class DefaultTreeNode extends TreeNode {

	private String nodeText;
	private String color;
	private Map<String, String> attributes;

	public DefaultTreeNode() {
		this("");
	}

	public DefaultTreeNode(String nodeText) {
		super();
		this.nodeText = nodeText;
		this.color = "";
		this.attributes = new HashMap<>();
	}

	@Override
	public boolean isGenerated() {
		return false;
	}

	@Override
	public boolean canHaveChildNodes() {
		return true;
	}

	@Override
	public boolean canHaveAttributes() {
		return true;
	}

	@Override
	public String getText() {
		return this.nodeText;
	}

	@Override
	public TreeNode setText(String text) {
		preNodeUpdate();

		this.nodeText = text;
		nodeUpdated();
		return this;
	}

	@Override
	public String getColor() {
		return this.color;
	}

	@Override
	public TreeNode setColor(String color) {
		preNodeUpdate();

		this.color = color;
		nodeUpdated();
		return this;
	}

	@Override
	public boolean hasAttribute(String name) {
		return attributes.containsKey(name);
	}

	@Override
	public String getAttribute(String name) {
		return attributes.getOrDefault(name, "");
	}

	@Override
	public void setAttribute(String name, String value, boolean silent) {

		if(!silent){
			preNodeUpdate();
		}

		if(name.equals("text")){
			setText(value);
		}
		else if(name.equals("color")){
			setColor(value);
		}
		else{
			attributes.put(name, value);
		}

		if(!silent){
			nodeUpdated();
		}
	}

	@Override
	public void removeAttribute(String name) {
		preNodeUpdate();

		attributes.remove(name);
		nodeUpdated();
	}

	@Override
	public Set<String> listAttributes() {
		return Collections.unmodifiableSet(attributes.keySet());
	}

	@Override
	public Set<Entry<String, String>> getAttributes() {
		return Collections.unmodifiableSet(this.attributes.entrySet());
	}

	@Override
	public void onParentUpdate() {}

	@Override
	public TreeNode cloneNode() {
		DefaultTreeNode clone = new DefaultTreeNode();
		Utilities.hashCopy(this.attributes, clone.attributes);
		return clone;
	}
}
