package de.akubix.keyminder.ui.fx;

import java.io.File;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

import de.akubix.keyminder.ui.UserInterface;
import de.akubix.keyminder.ui.fx.events.FxSettingsEvent;
import de.akubix.keyminder.ui.fx.events.HotKeyEvent;
import de.akubix.keyminder.ui.fx.sidebar.FxSidebar;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.stage.FileChooser;

public interface JavaFxUserInterfaceApi extends UserInterface {

	// Menu entries
	public void addMenuEntry(MenuItem item, MenuEntryPosition pos, boolean add2TreeDependentItems);
	public void addMenu(Menu menu, boolean add2TreeDependentItems);

	public void addCustomElement(Node node, IdentifiableElement parentElement) throws IllegalArgumentException;
	public Node lookupElement(IdentifiableElement element);

	// Sidebar
	public Tab addSidebarPanel(String sidebarTabTitle, FxSidebar sidebar, int index, boolean disableSidebarWhileNoFileIsOpened);
	public FxSidebar getCurrentSidebar();
	public ReadOnlyDoubleProperty getSidebarWidthProperty();

	// Notifications and panels
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
	public File showOpenFileDialog(String dialogTitle, String initalFileName, String initalDirectory, FileChooser.ExtensionFilter[] fileExtensions);
	public File showSaveFileDialog(String dialogTitle, String initalFileName, String initalDirectory, FileChooser.ExtensionFilter[] fileExtensions);

	// etc
	public void runAsFXThread(Runnable r);
	public void focusMainWindow();

	// Events
	public void addEventListener(FxSettingsEvent eventName, BiConsumer<TabPane, Map<String, String>> eventData);
}
