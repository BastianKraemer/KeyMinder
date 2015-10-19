package de.akubix.keyminder.core.interfaces;

import de.akubix.keyminder.core.ApplicationInstance;

/**
 * This interface has to be implemented by the main window of the JavaFX user interface.
 * Compared to the interface {@link FxUserInterface} this interface is only available in the {@link ApplicationInstance} class.
 */
public interface FxAdministrationInterface extends FxUserInterface{
	/**
	 * Sets the title of the main window
	 * @param title the window title
	 */
	public void setTitle(String title);
	
	/**
	 * This method is called when a file is opened and very similar to the {@link de.akubix.keyminder.core.interfaces.events.EventTypes.DefaultEvent#OnFileOpened} event.
	 * The only difference is, that this method is called before all event handlers.
	 */
	public void onFileOpenedHandler();

	/**
	 * This method is called when the list of favorite nodes has been updated.
	 * @param favoriteNodes An array of tree node IDs. Each node has been selected as "favorite node"
	 */
	public void buildFavoriteNodeList(int[] favoriteNodes);
}
