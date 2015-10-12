package de.akubix.keyminder.core.interfaces.events;

import java.util.Map;

import de.akubix.keyminder.core.exceptions.IllegalCallException;

public interface EventHost {
	public void addEventHandler(EventTypes.DefaultEvent eventName, DefaultEventHandler eventHandler);
	public void addEventHandler(EventTypes.BooleanEvent eventName, BooleanEventHandler eventHandler);
	public void addEventHandler(EventTypes.TreeNodeEvent eventName, TreeNodeEventHandler eventHandler);
	public void addEventHandler(EventTypes.SettingsEvent eventName, SettingsEventHandler eventHandler);
	
	public void fireEvent(de.akubix.keyminder.core.interfaces.events.EventTypes.DefaultEvent event) throws IllegalCallException;
	public boolean fireEvent(de.akubix.keyminder.core.interfaces.events.EventTypes.BooleanEvent event, boolean cancelOn, boolean cancelValue) throws IllegalCallException;
	public void fireEvent(de.akubix.keyminder.core.interfaces.events.EventTypes.TreeNodeEvent event, de.akubix.keyminder.core.db.TreeNode node) throws IllegalCallException;
	public void fireEvent(de.akubix.keyminder.core.interfaces.events.EventTypes.SettingsEvent event, javafx.scene.control.TabPane tabControl, Map<String, String> settings) throws IllegalCallException;
}