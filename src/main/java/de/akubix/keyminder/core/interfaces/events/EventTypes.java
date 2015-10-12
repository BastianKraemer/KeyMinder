package de.akubix.keyminder.core.interfaces.events;


public class EventTypes {
	public enum DefaultEvent {
		// These events does not return any value
		OnExit, OnFileOpened, OnFileClosed,
		OnSettingsChanged, OnFileSettingsChanged
	}
	public enum BooleanEvent {
		//These events return a boolean
		DONTAllowFileClosing
	}
	public enum TreeNodeEvent {
		// Events, that will need a TreeNode as parameter
		OnNodeAdded, OnNodeEdited,  OnNodeVerticallyMoved, OnNodeRemoved, OnSelectedItemChanged,
	}

	public enum SettingsEvent {
		// Events, which handle with settings
		OnSettingsDialogOpened, OnFileSettingsDialogOpened
	}
}
