package de.akubix.keyminder.core.interfaces.events;

import java.util.Map;

import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.exceptions.IllegalCallException;

public interface EventHost {
	public void addEventHandler(EventTypes.DefaultEvent eventName, DefaultEventHandler eventHandler);
	public void addEventHandler(EventTypes.ComplianceEvent eventName, ComplianceEventHandler eventHandler);
	public void addEventHandler(EventTypes.TreeNodeEvent eventName, TreeNodeEventHandler eventHandler);
	public void addEventHandler(EventTypes.SettingsEvent eventName, SettingsEventHandler eventHandler);

	public void fireEvent(EventTypes.DefaultEvent event) throws IllegalCallException;
	public Compliance fireEvent(EventTypes.ComplianceEvent event) throws IllegalCallException;
	public void fireEvent(EventTypes.TreeNodeEvent event, TreeNode node) throws IllegalCallException;
	public void fireEvent(EventTypes.SettingsEvent event, javafx.scene.control.TabPane tabControl, Map<String, String> settings) throws IllegalCallException;
}