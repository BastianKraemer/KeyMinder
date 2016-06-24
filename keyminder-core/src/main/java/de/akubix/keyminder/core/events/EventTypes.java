package de.akubix.keyminder.core.events;

public class EventTypes {
	public enum DefaultEvent {
		// These events does not return any value
		OnExit, OnFileOpened, OnFileClosed,
		OnSettingsChanged, OnFileSettingsChanged, OnQuicklinksUpdated
	}

	/**
	 * Please use this events with care, because they affect the file handling.
	 *
	 * Event 'DiscardChanges': In this case you have got three options:
	 * <ul>
	 * <li>Agree with {@link Compliance#AGREE} to allow this (in this case all event handler have to agree)</li>
	 * <li>Don't agree with {@link Compliance#DONT_AGREE} which means that the changes should be saved (this is done if at least one event handler returns this).</li>
	 * <li>Cancel the operation with {@link Compliance#CANCEL} (this is done if at least one event handler returns this)</li>
	 * </ul>
	 */
	public enum ComplianceEvent {
		AllowFileClosing, DiscardChanges
	}
	public enum TreeNodeEvent {
		// Events, that will need a TreeNode as parameter
		OnNodeAdded, OnNodeEdited,  OnNodeVerticallyMoved, OnNodeRemoved, OnSelectedItemChanged
	}

	public enum SettingsEvent {
		// Events, which handle with settings
		OnSettingsDialogOpened, OnFileSettingsDialogOpened
	}
}
