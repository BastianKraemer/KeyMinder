package de.akubix.keyminder.core.events;

/**
 * This class is used by the event handling. The compliance is given be the event name, for example "DiscardChanges":
 * This event is fired when there are unsaved changes.
 */
public enum Compliance {
	AGREE, DONT_AGREE, CANCEL
}
