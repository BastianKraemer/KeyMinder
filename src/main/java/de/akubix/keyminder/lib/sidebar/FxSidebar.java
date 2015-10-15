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
package de.akubix.keyminder.lib.sidebar;

import java.util.HashMap;
import java.util.Map;

import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.core.interfaces.FxUserInterface;
import de.akubix.keyminder.core.interfaces.events.DefaultEventHandler;
import de.akubix.keyminder.core.interfaces.events.EventTypes.DefaultEvent;
import de.akubix.keyminder.ui.fx.dialogs.InputDialog;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;

public class FxSidebar {
	
	private Map<String, FxSidebarElement> elements = new HashMap<>();
	private de.akubix.keyminder.core.ApplicationInstance instance;
	private final VBox sidebarContainer;
	private final javafx.scene.control.Tab sidebarTab;
	private boolean firstLabel = true;
	
	public FxSidebar(de.akubix.keyminder.core.ApplicationInstance instance, String sidebarTitle, boolean disableSidebarWhileNoFileIsOpened, EventHandler<ActionEvent> keySendHandler) throws IllegalStateException
	{
		if(!instance.isFxUserInterfaceAvailable()){throw new IllegalStateException("JavaFX User Interface is not available.");}
		this.instance = instance;
		this.sidebarContainer = new VBox();
		
		this.sidebarContainer.setPadding(new Insets(4,4,0,10));
		ScrollPane scrollPane = new ScrollPane(sidebarContainer);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

		this.sidebarContainer.prefWidthProperty().bind(instance.getFxUserInterface().getSidbarWidthProperty().subtract(12));
		scrollPane.prefWidthProperty().bind(instance.getFxUserInterface().getSidbarWidthProperty());

		this.sidebarTab = instance.getFxUserInterface().addSidebarPanel(sidebarTitle, scrollPane, (node) -> {
			boolean b[] = new boolean[]{false};
			elements.values().forEach((fxSidebarEl) -> {b[0] = fxSidebarEl.loadData(node) || b[0];});
			return b[0];
		}, keySendHandler);
		
		if(disableSidebarWhileNoFileIsOpened)
		{
			sidebarTab.setDisable(true);
			
			instance.addEventHandler(DefaultEvent.OnFileOpened, new DefaultEventHandler() {
				@Override
				public void eventFired() {
					sidebarTab.setDisable(false);
				}
			});
			
			instance.addEventHandler(DefaultEvent.OnFileClosed, new DefaultEventHandler() {
				@Override
				public void eventFired() {
					sidebarTab.setDisable(true);
					for(String key: elements.keySet())
					{
						elements.get(key).setUIValue("");
					}
				}
			});
		}
	}
	
	public void addElementToSidebar(FxSidebarElement sidebarElement, String name)
	{
		elements.put(name, sidebarElement);
		sidebarContainer.getChildren().add(sidebarElement.getFxRootNode());
	}
	
	public void addLabel(String labelText)
	{
		addLabel(labelText, !firstLabel);
		firstLabel = false;
	}
	
	public void addLabel(String labelText, boolean applyTopPadding)
	{
		Label l = new Label(labelText);
		if(applyTopPadding){l.getStyleClass().add("sidebarLabel");}
		sidebarContainer.getChildren().add(l);
	}
	
	public void addSeperator()
	{
		Separator s = new Separator(Orientation.HORIZONTAL);
		s.setPadding(new Insets(5, -10,0,-10));
		sidebarContainer.getChildren().add(s);
	}
	
	public FxSidebarElement createDefaultSidebarTextbox(String hashKey)
	{
		return new FxSidebarTextbox(instance) {
			@Override
			public void storeData(TreeNode node) {
				String value = this.getUIValue();
				if(!node.getAttribute(hashKey).equals(value))
				{
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
	
	public FxSidebarElement createDefaultSidebarPasswordbox(String hashKey)
	{
		return new FxSidebarPasswordbox(instance) {
			@Override
			public void storeData(TreeNode node) {
				String value = this.getUIValue();
				if(!node.getAttribute(hashKey).equals(value))
				{
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
	
	public FxSidebarElement createDefaultSidebarCheckbox(String hashKey, String checkboxLabelText)
	{
		return new FxSidebarCheckbox(instance, checkboxLabelText) {
			@Override
			public void storeData(TreeNode node) {
				String value = this.getUIValue();
				if(!node.getAttribute(hashKey).equals(value))
				{
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
	
	public FxSidebarElement createDefaultSidebarTextarea(String hashKey)
	{
		return new FxSidebarTextarea(instance) {
			@Override
			public void storeData(TreeNode node) {
				String value = this.getUIValue();
				if(!node.getAttribute(hashKey).equals(value))
				{
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
	
	public FxSidebarElement createDefaultSidebarHyperlink(String hashKey, boolean useMailTo)
	{
		return new FxSidebarHyperlink(instance, useMailTo) {
			@Override
			public void storeData(TreeNode node) {
				FxUserInterface fxUI = instance.getFxUserInterface();
				String defaultText = getUIValue();
				InputDialog id = new InputDialog(instance.getFxUserInterface(), fxUI.getLocaleBundleString(useMailTo ? "mainwindow.sidebar.hyperlink.edit_email" : "mainwindow.sidebar.hyperlink.edit_link"),
																			 fxUI.getLocaleBundleString(useMailTo ? "mainwindow.sidebar.hyperlink.dialog.edit_email" : "mainwindow.sidebar.hyperlink.dialog.edit_link"),
																			 defaultText.equals("...") ? "" : defaultText, false);

				try{
					String input = id.getInput();
					if(input != null && !input.equals(""))
					{
						node.setAttribute(hashKey, input);
						setUIValue(input);
						instance.getFxUserInterface().updateStatus(fxUI.getLocaleBundleString(useMailTo ? "mainwindow.sidebar.hyperlink.messages.email_edited" : "mainwindow.sidebar.hyperlink.messages.link_edited"));
					}
				}
				catch(UserCanceledOperationException e){}

				fxUI.focusMainWindow();
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
