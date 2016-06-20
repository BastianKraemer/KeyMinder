package de.akubix.keyminder.core.interfaces.events;

import java.util.Map;

import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.exceptions.IllegalCallException;

public interface EventHost {
	public void addEventHandler(EventTypes.DefaultEvent eventName, DefaultEventHandler eventHandler);
	public void addEventHandler(EventTypes.BooleanEvent eventName, BooleanEventHandler eventHandler);
	public void addEventHandler(EventTypes.TreeNodeEvent eventName, TreeNodeEventHandler eventHandler);
	public void addEventHandler(EventTypes.SettingsEvent eventName, SettingsEventHandler eventHandler);

	public void fireEvent(EventTypes.DefaultEvent event) throws IllegalCallException;
	public boolean fireEvent(EventTypes.BooleanEvent event, boolean cancelOn, boolean cancelValue) throws IllegalCallException;
	public void fireEvent(EventTypes.TreeNodeEvent event, TreeNode node) throws IllegalCallException;
	public void fireEvent(EventTypes.SettingsEvent event, javafx.scene.control.TabPane tabControl, Map<String, String> settings) throws IllegalCallException;
}