package de.akubix.keyminder.core.interfaces;

import java.io.File;
import java.util.ResourceBundle;

import de.akubix.keyminder.core.etc.MenuEntryPosition;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.core.interfaces.events.HotKeyEvent;
import de.akubix.keyminder.core.interfaces.events.SidebarNodeChangeEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.stage.FileChooser;

public interface FxUserInterface extends UserInterface {

	// Additional FX Components
	public javafx.scene.control.Tab addSidebarPanel(String tabtitle, Node panel, SidebarNodeChangeEvent onSelectedNodeChanged, EventHandler<ActionEvent> onKeyClipButtonClicked);
	public javafx.beans.property.ReadOnlyDoubleProperty getSidbarWidthProperty();
	public void addMenuEntry(MenuItem item, MenuEntryPosition pos, boolean add2TreeDependentItems);
	public void addMenu(Menu menu, boolean add2TreeDependentItems);
	public void addNotificationItem(Node item, boolean assignToThisFile);
	public void removeNotificationItem(Node item);
	public void addTreePanel(Node item, boolean assignToThisFile);
	public void removeTreePanel(Node item);

	// Localization
	public ResourceBundle getLocaleRessourceBundle();
	public String getLocaleBundleString(String key);

	// Clipboard
	public String getClipboardText();
	public void setClipboardText(String text);

	// Text output
	public void alert(AlertType type, String title, String headline, String contentText);

	// Hotkeys
	public void addApplicationHotKey(KeyCode keyCode, boolean controlKey, boolean shiftKey, boolean altKey, HotKeyEvent onKeyDown);

	// Dialogs

	/**
	 * Show a save changes dialog
	 * @return {@code true} if the changes should be saved or {@code false} should be discarded
	 * @throws UserCanceledOperationException if the user has pressed the "Cancel" button
	 */
	public boolean showSaveChangesDialog() throws UserCanceledOperationException;

	public File showOpenFileDialog(String dialogTitle, String initalFileName, String initalDirectory, FileChooser.ExtensionFilter[] fileExtensions);
	public File showSaveFileDialog(String dialogTitle, String initalFileName, String initalDirectory, FileChooser.ExtensionFilter[] fileExtensions);

	// etc
	public void runAsFXThread(Runnable r);
	public boolean isFXThread();
	public void focusMainWindow();
}
