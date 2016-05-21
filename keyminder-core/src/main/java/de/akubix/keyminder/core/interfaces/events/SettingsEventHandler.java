package de.akubix.keyminder.core.interfaces.events;

import java.util.Map;
import javafx.scene.control.TabPane;

public interface SettingsEventHandler {
	public void eventFired(TabPane tabControl, Map<String, String> settings);
}
