package de.akubix.keyminder.core.interfaces;

import java.io.File;
import java.util.ResourceBundle;

import de.akubix.keyminder.core.etc.MenuEntryPosition;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.ui.fx.shadow.FxHotKeyEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.stage.FileChooser;

public interface FxUserInterface {
	
	// Additional FX Components
	public javafx.scene.control.Tab addSidebarPanel(String tabtitle, Node panel, de.akubix.keyminder.lib.sidebar.SidebarNodeChangeEvent onSelectedNodeChanged, EventHandler<ActionEvent> onKeyClipButtonClicked);
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
	public void updateStatus(String text);
	public void log(String text);
	public void alert(String text);
	public void alert(AlertType type, String title, String headline, String contentText);
	
	// Hotkeys
	public void addApplicationHotKey(KeyCode keyCode, boolean controlKey, boolean shiftKey, boolean altKey, FxHotKeyEvent onKeyDown);
	
	// Dialogs

	/**
	 * Show a save changes dialog
	 * @return "-1" if the user canceled the action, "0" if the changes should be discarded, "1" if the file should be saved
	 */
	public int showSaveChangesDialog();
	public String showInputDialog(String windowTitle, String labelText, String defaultValue, boolean useAsPasswordDialog) throws UserCanceledOperationException;
	public boolean showYesNoDialog(String windowTitle, String headline, String contentText);
	
	public File showOpenFileDialog(String dialogTitle, String initalFileName, String initalDirectory, FileChooser.ExtensionFilter[] fileExtensions);
	public File showSaveFileDialog(String dialogTitle, String initalFileName, String initalDirectory, FileChooser.ExtensionFilter[] fileExtensions);
	
	// etc
	public void runAsFXThread(Runnable r);
	public boolean isFXThread();
}
