/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	FxSidebar.java

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
package de.akubix.keyminder.ui.fx.sidebar;

import java.util.HashMap;
import java.util.Map;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.ui.fx.JavaFxUserInterfaceApi;
import de.akubix.keyminder.ui.fx.dialogs.InputDialog;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class FxSidebar {

	private final Map<String, FxSidebarElement> elements = new HashMap<>();
	private final ApplicationInstance app;
	private final JavaFxUserInterfaceApi javaFxUserInterfaceApi;
	private final VBox sidebarContainer;
	private boolean firstLabel = true;

	public FxSidebar(ApplicationInstance app, JavaFxUserInterfaceApi javaFxUserInterfaceApi) throws IllegalStateException {

		this.javaFxUserInterfaceApi = javaFxUserInterfaceApi;
		this.app = app;

		this.sidebarContainer = new VBox();
		this.sidebarContainer.setPadding(new Insets(4,4,0,10));
	}

	public Pane getContainer(){
		return this.sidebarContainer;
	}

	public boolean update(TreeNode node){
		boolean isNotEmpty = false;
		for(FxSidebarElement el: elements.values()){
			isNotEmpty = el.loadData(node) || isNotEmpty;
		}
		return isNotEmpty;
	}

	public void addElementToSidebar(FxSidebarElement sidebarElement, String name){
		elements.put(name, sidebarElement);
		sidebarContainer.getChildren().add(sidebarElement.getFxRootNode());
	}

	public void addLabel(String labelText){
		addLabel(labelText, !firstLabel);
		firstLabel = false;
	}

	public void addLabel(String labelText, boolean applyTopPadding){
		Label l = new Label(labelText);
		if(applyTopPadding){l.getStyleClass().add("sidebarLabel");}
		sidebarContainer.getChildren().add(l);
	}

	public void addSeparator(){
		Separator s = new Separator(Orientation.HORIZONTAL);
		s.setPadding(new Insets(5, -10,0,-10));
		sidebarContainer.getChildren().add(s);
	}

	public FxSidebarElement createDefaultSidebarTextbox(String hashKey){

		return new FxSidebarTextbox(app) {
			@Override
			public void storeData(TreeNode node) {
				String value = this.getUIValue();
				if(!node.getAttribute(hashKey).equals(value)){
					node.setAttribute(hashKey, value);
				}
			}

			@Override
			public boolean loadData(TreeNode node) {
				this.setUIValue(node.getAttribute(hashKey));
				return node.hasAttribute(hashKey);
			}
		};
	}

	public FxSidebarElement createDefaultSidebarPasswordbox(String hashKey){

		return new FxSidebarPasswordbox(app) {
			@Override
			public void storeData(TreeNode node) {
				String value = this.getUIValue();
				if(!node.getAttribute(hashKey).equals(value)){
					node.setAttribute(hashKey, value);
				}
			}

			@Override
			public boolean loadData(TreeNode node) {
				this.setUIValue(node.getAttribute(hashKey));
				return node.hasAttribute(hashKey);
			}
		};
	}

	public FxSidebarElement createDefaultSidebarCheckbox(String hashKey, String checkboxLabelText){

		return new FxSidebarCheckbox(app, checkboxLabelText) {
			@Override
			public void storeData(TreeNode node) {
				String value = this.getUIValue();
				if(!node.getAttribute(hashKey).equals(value)){
					node.setAttribute(hashKey, value);
				}
			}

			@Override
			public boolean loadData(TreeNode node) {
				this.setUIValue(node.getAttribute(hashKey));
				return node.hasAttribute(hashKey);
			}
		};
	}

	public FxSidebarElement createDefaultSidebarTextarea(String hashKey) {

		return new FxSidebarTextarea(app) {
			@Override
			public void storeData(TreeNode node) {
				String value = this.getUIValue();
				if(!node.getAttribute(hashKey).equals(value)){
					node.setAttribute(hashKey, value);
				}
			}

			@Override
			public boolean loadData(TreeNode node) {
				this.setUIValue(node.getAttribute(hashKey));
				return node.hasAttribute(hashKey);
			}
		};
	}

	public FxSidebarElement createDefaultSidebarHyperlink(String hashKey, boolean useMailTo) {

		return new FxSidebarHyperlink(app, useMailTo) {
			@Override
			public void storeData(TreeNode node) {

				String defaultText = getUIValue();

				InputDialog id = new InputDialog(
					javaFxUserInterfaceApi,
					javaFxUserInterfaceApi.getLocaleBundleString(useMailTo ? "mainwindow.sidebar.hyperlink.edit_email" : "mainwindow.sidebar.hyperlink.edit_link"),
					javaFxUserInterfaceApi.getLocaleBundleString(useMailTo ? "mainwindow.sidebar.hyperlink.dialog.edit_email" : "mainwindow.sidebar.hyperlink.dialog.edit_link"),
					defaultText.equals("...") ? "" : defaultText,
					false
				);

				try{
					String input = id.getInput();
					if(input != null && !input.equals("")){

						node.setAttribute(hashKey, input);
						setUIValue(input);
						javaFxUserInterfaceApi.updateStatus(
							javaFxUserInterfaceApi.getLocaleBundleString(useMailTo ? "mainwindow.sidebar.hyperlink.messages.email_edited" : "mainwindow.sidebar.hyperlink.messages.link_edited")
						);
					}
				}
				catch(UserCanceledOperationException e){}

				javaFxUserInterfaceApi.focusMainWindow();
			}

			@Override
			public boolean loadData(TreeNode node) {
				boolean hasValue = node.hasAttribute(hashKey);
				this.setUIValue(hasValue ? node.getAttribute(hashKey) : "...");
				return hasValue;
			}
		};
	}
}
