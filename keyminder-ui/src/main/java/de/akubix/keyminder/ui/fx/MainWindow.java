/*	KeyMinder
 * Copyright (C) 2015-2016 Bastian Kraemer
 *
 * MainWindow.java
 *
 * KeyMinder is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * KeyMinder is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with KeyMinder.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.akubix.keyminder.ui.fx;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Timer;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.KeyMinder;
import de.akubix.keyminder.core.encryption.AES;
import de.akubix.keyminder.core.events.Compliance;
import de.akubix.keyminder.core.events.EventTypes.ComplianceEvent;
import de.akubix.keyminder.core.events.EventTypes.DefaultEvent;
import de.akubix.keyminder.core.events.EventTypes.TreeNodeEvent;
import de.akubix.keyminder.core.events.TreeNodeEventHandler;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.core.io.FileExtension;
import de.akubix.keyminder.core.io.StorageManager;
import de.akubix.keyminder.core.tree.DefaultTreeNode;
import de.akubix.keyminder.core.tree.Direction;
import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.core.tree.TreeStore;
import de.akubix.keyminder.locale.LocaleLoader;
import de.akubix.keyminder.shell.CommandException;
import de.akubix.keyminder.ui.KeyMinderUserInterface;
import de.akubix.keyminder.ui.fx.components.AbstractEditableTreeCell;
import de.akubix.keyminder.ui.fx.components.TreeNodeItem;
import de.akubix.keyminder.ui.fx.components.TreeNodeReference;
import de.akubix.keyminder.ui.fx.components.VisibleTreeNodesSkin;
import de.akubix.keyminder.ui.fx.dialogs.FileSettingsDialog;
import de.akubix.keyminder.ui.fx.dialogs.FindAndReplaceDialog;
import de.akubix.keyminder.ui.fx.dialogs.InputDialog;
import de.akubix.keyminder.ui.fx.dialogs.NodeInfoDialog;
import de.akubix.keyminder.ui.fx.dialogs.SaveChangesDialog;
import de.akubix.keyminder.ui.fx.dialogs.SaveChangesDialog.Result;
import de.akubix.keyminder.ui.fx.dialogs.SettingsDialog;
import de.akubix.keyminder.ui.fx.events.FxSettingsEvent;
import de.akubix.keyminder.ui.fx.events.HotKeyEvent;
import de.akubix.keyminder.ui.fx.sidebar.FxSidebar;
import de.akubix.keyminder.ui.fx.utils.ImageMap;
import de.akubix.keyminder.ui.fx.utils.StylesheetMap;
import de.akubix.keyminder.util.Utilities;
import de.akubix.keyminder.util.search.NodeWalker;
import de.akubix.keyminder.util.search.NodeWalker.SearchResult;
import de.akubix.keyminder.util.search.NodeWalker.SearchState;
import de.akubix.keyminder.util.search.matcher.TextMatcher;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

@KeyMinderUserInterface(
	name="JavaFX user interface for KeyMinder",
	id = JavaFxUserInterface.USER_INTERFACE_ID
)
public class MainWindow extends Application implements JavaFxUserInterfaceApi {

	private HashMap<String, TreeItem<TreeNodeReference>> treeNodeTranslator = new HashMap<>();

	private ApplicationInstance app;
	private TreeStore dataTree;

	public static void init(String[] args){
		launch(args);
	}

	private boolean nextSelectedItemChangeEventWasFiredByMe = false;

	private boolean treeEditModeActive = false;

	private TreeView<TreeNodeReference> fxtree;
	private Stage me;

	private BorderPane rootPanel;
	private ContextMenu treeContextMenu;

	private SplitPane splitPane;
	private BorderPane sidebarPanel;
	private TabPane sidebarTabPanel;
	private Node sidebarToolPanel;
	private BorderPane centerPane;
	private BorderPane searchBoard;
	private final Pane notificationArea = new HBox();
	private final Pane panelStack = new VBox();
	private	TextField searchInput;
	private Label statusLabel;

	private MenuBar menuBar;
	private Menu menu_File;
	private Menu menu_Edit;
	private Menu menu_View;
	private Menu menu_Tools;
	private Menu menu_Extras;
	private Menu menu_Quicklinks = null;

	private List<FxSidebar> fxSidebarList = new ArrayList<>();
	private SimpleBooleanProperty treeDependentElementsDisableProperty = new SimpleBooleanProperty(true);
	private List<Node> assignedNotificationsItems = new ArrayList<>(2);
	private ResourceBundle localeBundle;

	final Clipboard clipboard = Clipboard.getSystemClipboard();

	private Map<String, HotKeyEvent> hotkeys = new HashMap<>();
	private List<TreeNode> internalClipboard = null;

	/*
	 * ======================================================================================================================================================
	 * Startup and Build UI
	 * ======================================================================================================================================================
	 */
	@Override
	public void start(Stage primaryStage) {

		me = primaryStage;

		rootPanel = new BorderPane();
		Scene scene = new Scene(rootPanel, 680, 420);

		app = new ApplicationInstance(this);
		dataTree = app.getTree();

		/* ================================================================================================================
		 * Language pack
		 * ================================================================================================================
		 */

		localeBundle = LocaleLoader.loadLanguagePack("ui", "fxUI", app.getLocale());
		LocaleLoader.provideBundle(JavaFxUserInterface.LANGUAGE_BUNDLE_KEY, localeBundle);

		/* ================================================================================================================
		 * Build user interface
		 * ================================================================================================================
		 */

		// Generate the whole graphical user interface
		buildUI(rootPanel);

		StylesheetMap.assignStylesheet(scene, StylesheetMap.WindowSelector.MainWindow);
		me.setScene(scene);
		me.setMinWidth(640);
		me.setMinHeight(400);

		updateWindowTitle();

		/* ================================================================================================================
		 * Event Registration
		 * ================================================================================================================
		 */

		app.addEventHandler(TreeNodeEvent.OnSelectedItemChanged, (node) -> selectedNodeChanged(node));

		app.addEventHandler(TreeNodeEvent.OnNodeAdded, (node) -> displayNewTreePart(node));
		app.addEventHandler(TreeNodeEvent.OnNodeEdited,(node) -> fxtree.refresh());
		app.addEventHandler(TreeNodeEvent.OnNodeVerticallyMoved, (node) -> rebuildTreePart(node.getParentNode(), node.getId()));
		app.addEventHandler(TreeNodeEvent.OnNodeReset, (node) -> resetNode(node));
		app.addEventHandler(TreeNodeEvent.OnNodeRemoved, (node) -> {
			TreeItem<TreeNodeReference> treeitem = getTreeItemOfTreeNode(node);
			treeitem.getParent().getChildren().remove(treeitem);
			deleteTranslatorHashItems(treeitem, true);
		});

		app.addEventHandler(DefaultEvent.OnFileOpened, () -> {
			onFileOpenedHandler();
			updateWindowTitle();
		});

		app.addEventHandler(DefaultEvent.OnSettingsChanged, () -> updateWindowTitle());

		app.addEventHandler(DefaultEvent.OnFileClosed, () -> {
			nextSelectedItemChangeEventWasFiredByMe = true;

			//The node will belong to notificationArea or panelStack.
			if(assignedNotificationsItems.size() > 0){
				assignedNotificationsItems.forEach((node) -> {notificationArea.getChildren().remove(node); panelStack.getChildren().remove(node);});
				assignedNotificationsItems.clear();
			}

			fxtree.getRoot().getChildren().clear();
			treeNodeTranslator.clear();
			treeDependentElementsDisableProperty.set(true);
			clearQuicklinkList(true);

			updateWindowTitle();
		});

		app.addEventHandler(ComplianceEvent.DiscardChanges, () -> {
			try {
				return showSaveChangesDialog() ? Compliance.DONT_AGREE : Compliance.AGREE;
			} catch (UserCanceledOperationException e) {
				return Compliance.CANCEL;
			}
		});

		app.addEventHandler(DefaultEvent.OnExit, () -> me.close());
		app.addEventHandler(DefaultEvent.OnQuicklinksUpdated, () -> updateQuicklinkList());

		startupFxUI();

		if(!AES.isAES256Supported()){
			// Important security note
			Button notification = new Button("", ImageMap.getFxImageView(("icon_warning")));
			notification.setMinWidth(24);
			notification.setMaxWidth(24);
			Tooltip tooltip = new Tooltip(localeBundle.getString("security.aes_warning_title"));
			notification.setTooltip(tooltip);
			notification.getStyleClass().add("noBorder");

			notification.setOnAction((event) -> {
				app.alert(localeBundle.getString("security.aes_warning_message"));
			});
			addNotificationItem(notification, false);
		}

		me.setOnCloseRequest((WindowEvent event) -> {
			// These following four lines are needed to make sure that the user can select "Cancel"
			// at the "SaveChangesDialog". The method "ApplicationInstance.closeFile()" provides only true and false as return value,
			// but for this functionality a third state for "cancel" is required.
			// Therefore the value of "Tree.treeHasBeenUpdated()" will be reset before calling "closeFile()".
			if(dataTree.hasUnsavedChanges()){
				SaveChangesDialog.Result r = SaveChangesDialog.show(this);
				if(r == Result.Cancel){event.consume(); return;} // Cancel
				else if(r == Result.SaveChanges){if(!app.saveFile()){event.consume(); me.requestFocus(); return;}}
				else if(r == Result.DiscardChanges){dataTree.setTreeChangedStatus(false);}
			}
			if(!app.closeFile()){
				event.consume();
				me.requestFocus();
			}
			else{
				app.terminate();
			}
		});

		// Show the main window
		ImageMap.addDefaultIconsToStage(me);

		me.show();

		// Startup the application core (load the optional plugins, ...)
		app.startup(true);

		runAsFXThread(new Runnable() {
			@Override
			public void run() {
				// Load the default password file
				app.loadDefaultFile();

				if(fxtree.getRoot().getChildren().size() == 1){
					if(!fxtree.getRoot().getChildren().get(0).isLeaf()){fxtree.getRoot().getChildren().get(0).setExpanded(true);}
				}
			}
		});

		if(sidebarTabPanel.getTabs().size() > 0){
			sidebarTabPanel.getSelectionModel().select(0);
		}
	}

	private void selectedNodeChanged(TreeNode selectedNode){

		if(!nextSelectedItemChangeEventWasFiredByMe){
			if(getTreeItemOfTreeNode(selectedNode) != fxtree.getRoot()) {
				if(getTreeItemOfTreeNode(selectedNode) != getSelectedTreeItem()){nextSelectedItemChangeEventWasFiredByMe = true;} // This value will be reset by the fxtree change listener
				fxtree.getSelectionModel().select(getTreeItemOfTreeNode(selectedNode));

				// Take a look at the class "FolderTreeViewSkin" above
				if (!((VisibleTreeNodesSkin) fxtree.getSkin()).isIndexVisible(fxtree.getSelectionModel().getSelectedIndex())){
					fxtree.scrollTo(fxtree.getSelectionModel().getSelectedIndex() - 3);
				}
			}
		}
		else {
			nextSelectedItemChangeEventWasFiredByMe = false;
		}
	}

	private void updateWindowTitle() {
		me.setTitle(
			ApplicationInstance.APP_NAME
			+ ((app.getSettingsValueAsBoolean("windowtitle.showfilename", true) && app.isAnyFileOpened()) ? " - " + app.getCurrentFile().getFilepath().getName() : "")
			+ (app.getSettingsValueAsBoolean("windowtitle.showversion", false) ? " (Version " + KeyMinder.getApplicationVersion() + ")" : ""));
	}

	private void onFileOpenedHandler(){
		buildTree();
		if(fxtree.getRoot().getChildren().size() > 0){
			fxtree.getSelectionModel().select((fxtree.getRoot().getChildren().get(0)));
		}

		nextSelectedItemChangeEventWasFiredByMe = false;
		lastFileDialogDirectory = app.getCurrentFile().getFilepath().getParent();
		treeDependentElementsDisableProperty.set(false);

		if(!dataTree.getSelectedNode().isRootNode()){
			selectedNodeChanged(dataTree.getSelectedNode());

			// The core will throw a "onSelectedItemChanged" event immediately after the return from this method
			nextSelectedItemChangeEventWasFiredByMe = true;
		}
	}

	@Override
	public ResourceBundle getLocaleRessourceBundle(){
		return localeBundle;
	}

	@Override
	public String getLocaleBundleString(String key){
		return localeBundle.getString(key);
	}

	@Override
	public void runAsFXThread(Runnable r){
		Platform.runLater(r);
	}

	@Override
	public boolean isUserInterfaceThread(){
		return Platform.isFxApplicationThread();
	}

	@Override
	public void addEventListener(FxSettingsEvent eventName, BiConsumer<TabPane, Map<String, String>> eventListener){
		app.addEventHandler(eventName.toString(), eventListener);
	}

	@Override
	public void focusMainWindow(){
		me.requestFocus();
	}

	private void buildUI(BorderPane root){

		/* ===================================================================================
		 * 	Menubar
		 * ===================================================================================
		 */

		menuBar = new MenuBar();
		menuBar.setId("MenuBar");

		// --- Menu File
		menu_File = new Menu(localeBundle.getString("mainwindow.menu.file"));

		Menu file_new = new Menu(localeBundle.getString("mainwindow.menu.file.new"), ImageMap.getFxImageView("icon_newfile"));
		file_new.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.file.new.encrypted_file"), ImageMap.getIcon("icon_new_encrypted_file"),
											  (event) -> showCreateNewFileDialog(true), false));

		file_new.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.file.new.regular_file"), ImageMap.getIcon("icon_newfile"),
				  (event) -> showCreateNewFileDialog(false), false));

		menu_File.getItems().addAll(file_new);


		menu_File.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.file.open"),
												ImageMap.getIcon("icon_openfile"),
												(event) -> initalizeOpenFile(), false));

		menu_File.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.file.save"),
												ImageMap.getIcon("icon_save"),
												(event) -> app.saveFile(), true));

		menu_File.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.file.saveas"), "",
												(event) -> initalizeSaveFileAs(), true));

		menu_File.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.file.close"), "",
												(event) -> {
													if(app.isAnyFileOpened()){
														if(app.closeFile()){
															updateStatus(localeBundle.getString("mainwindow.messages.file_successfully_closed"));
														}
													}
												}, true));

		if(app.settingsContainsKey("ui.filelist")){
			Menu openFileMenu = new Menu(localeBundle.getString("mainwindow.menu.file.recently_used"));
			String[] myFiles = app.getSettingsValue("ui.filelist").split(";");
			for(int i = 0; i < myFiles.length; i++){
				File f = new File(myFiles[i]);
				MenuItem item = createMenuItem(f.getName(), "", (event) -> app.openFile((File) ((MenuItem) event.getSource()).getUserData()), false);
				item.setUserData(f);
				openFileMenu.getItems().add( item);
			}
			menu_File.getItems().add(openFileMenu);
		}

		menu_File.getItems().addAll(
			new SeparatorMenuItem(),
			createMenuItem(localeBundle.getString("mainwindow.menu.file.filesettings"),	"", (event) -> {new FileSettingsDialog(me, app).show(); me.requestFocus();}, true));

		// --- Menu Edit
		menu_Edit = new Menu(localeBundle.getString("mainwindow.menu.edit"));

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.settings"), ImageMap.getIcon("icon_settings"),
												(event) -> {new SettingsDialog(me, app).show();	me.requestFocus();}, false));

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.duplicate_node"), "",
												(event) -> duplicateNode(getSelectedTreeNode(), true), true));

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.copy_text"),
												ImageMap.getIcon("icon_copy"),
												(event) -> setClipboardText(getSelectedTreeNode()), true));

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.insert_text"),	"",
												(event) -> InsertNodeFromClipboard(), true));

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.find"),
												ImageMap.getIcon("icon_find"), (event) -> showSearchBar(!searchBoardIsVisible), true));

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.find_replace"), "",
												(ActionEvent e) -> FindAndReplaceDialog.showInstance(me, dataTree, this), true));

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.add_root_node"),
												ImageMap.getIcon("icon_add"), (event) -> showAddTreeNodeDialog(true), true));

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.undo"),
												ImageMap.getIcon("icon_undo"), (event) -> undo(), true));

		// Menu entry to format nodes (bold, italic, strikeout)
		Menu nodeFontSettings = new Menu(localeBundle.getString("mainwindow.menu.edit.style"));

		final CheckMenuItem boldNode = new CheckMenuItem(localeBundle.getString("mainwindow.menu.edit.style.bold"));
		boldNode.setOnAction((event) -> node_toogleStyle(getSelectedTreeItem(), "bold"));

		final CheckMenuItem italicNode = new CheckMenuItem(localeBundle.getString("mainwindow.menu.edit.style.italic"));
		italicNode.setOnAction((event) -> node_toogleStyle(getSelectedTreeItem(), "italic"));

		final CheckMenuItem strikeoutNode = new CheckMenuItem(localeBundle.getString("mainwindow.menu.edit.style.strikeout"));
		strikeoutNode.setOnAction((event) -> node_toogleStyle(getSelectedTreeItem(), "strikeout"));

		nodeFontSettings.setOnShowing((event) -> {
			TreeItem<TreeNodeReference> n = getSelectedTreeItem();
			boldNode.setSelected(node_hasStyle(n, "bold"));
			italicNode.setSelected(node_hasStyle(n, "italic"));
			strikeoutNode.setSelected(node_hasStyle(n, "strikeout"));
		});

		nodeFontSettings.getItems().addAll(boldNode, italicNode, strikeoutNode);
		nodeFontSettings.disableProperty().bind(treeDependentElementsDisableProperty);

		menu_Edit.getItems().add(nodeFontSettings);

		// Sort nodes

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.sort"),
												ImageMap.getIcon("icon_sort"),
												(event) -> {
													TreeNode node = getSelectedTreeNode();
													if (node != null) {
														node.sortChildNodes(false);
													}
												}, true));

		// Menu entry for vertical node moving

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.move_node_up"),
												ImageMap.getIcon("icon_up"),
												(event) -> dataTree.getSelectedNode().move(Direction.UP), true));

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.move_node_down"),
												ImageMap.getIcon("icon_down"),
												(event) -> dataTree.getSelectedNode().move(Direction.DOWN), true));

		// --- Menu View
		menu_View = new Menu(localeBundle.getString("mainwindow.menu.view"));

		menu_View.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.view.expand"),
												ImageMap.getIcon("icon_add"),
												(event) -> getSelectedTreeItem().setExpanded(true), true));

		menu_View.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.view.collapse"),
												ImageMap.getIcon("icon_remove"),
												(event) -> getSelectedTreeItem().setExpanded(false), true));

		// --- Menu Tools
		menu_Tools = new Menu(localeBundle.getString("mainwindow.menu.tools"));
		menu_Tools.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.tools.terminal"),
												 ImageMap.getIcon("icon_bash"),
												 (event) -> new Terminal(app).show(), false));

		// --- Menu Extras
		menu_Extras = new Menu(localeBundle.getString("mainwindow.menu.extras"));
		menu_Extras.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.extras.nodeinfo"),
												  ImageMap.getIcon("icon_info"),
												  (event) -> {
													  new NodeInfoDialog(dataTree.getSelectedNode(), app).show(me);
													  me.requestFocus();
												  }, true));

		menu_Extras.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.extras.appinfo"),
				  ImageMap.getIcon("icon_star_filled"),
				  (event) -> {new de.akubix.keyminder.ui.fx.About(this).show();}, false));

		menuBar.getMenus().addAll(menu_File, menu_Edit, menu_View, menu_Extras, menu_Tools);
		root.setTop(menuBar);


		/* ===================================================================================
		 * 	Treeview
		 * ===================================================================================
		 */

		TreeNodeItem rootItem = new TreeNodeItem(new TreeNodeReference(dataTree.getRootNode()));
		rootItem.setExpanded(true);
		fxtree = new TreeView<> (rootItem);
		fxtree.setId("Tree");
		fxtree.setShowRoot(false);
		fxtree.setEditable(true);
		fxtree.setMinWidth(200);

		// Add a change listener to the tree
		fxtree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<TreeNodeReference>>() {
			@Override
			public void changed(ObservableValue<? extends TreeItem<TreeNodeReference>> observable, TreeItem<TreeNodeReference> oldValue, TreeItem<TreeNodeReference> newValue) {
				if(newValue != null){
					if(!nextSelectedItemChangeEventWasFiredByMe){
						TreeNode n = newValue.getValue().getTreeNode();
						nextSelectedItemChangeEventWasFiredByMe = true;
						dataTree.setSelectedNode(n);
					}
					else{
						nextSelectedItemChangeEventWasFiredByMe = false;
					}
				}
			}});

		centerPane = new BorderPane(fxtree);
		centerPane.setTop(panelStack);
		splitPane = new SplitPane(centerPane);
		splitPane.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
		splitPane.setId("SplitPane");
		splitPane.setUserData(0.6);

		root.setCenter(splitPane);

		fxtree.setCellFactory(new Callback<TreeView<TreeNodeReference>, TreeCell<TreeNodeReference>>(){
			@Override
			public TreeCell<TreeNodeReference> call(TreeView<TreeNodeReference> p) {
				return new AbstractEditableTreeCell(){
					@Override
					public boolean isTreeEdited() {
						return treeEditModeActive;
					}

					@Override
					public void setTreeEditStatus(boolean value) {
						treeEditModeActive = value;
					}
				};
			}
		});

		fxtree.setOnMouseClicked((MouseEvent event) -> {
			if(event.getButton() == MouseButton.MIDDLE){
				editTreeItem(getSelectedTreeItem());
			}
		});

		fxtree.setSkin(new VisibleTreeNodesSkin(fxtree));

		/* The following code is based on answer written by StackOverflow (stackoverflow.com) user José Pereda and is licensed under CC BY-SA 3.0
		 * "Creative Commons Attribution-ShareAlike 3.0 Unported", http://creativecommons.org/licenses/by-sa/3.0/)
		 *
		 * Source: http://stackoverflow.com/questions/27828982/javafx-treeview-remove-expand-collapse-button-disclosure-node-functionall/27831085#27831085
		 * The code has been modified, the idea behind is the same.
		 */

		// This is a workaround, because the JavafX Treeview Element acts a bit strange when moving the selected item using the arrow keys after a node has been removed.
		final EventDispatcher originalEventDispatcher = fxtree.getEventDispatcher();
		fxtree.setEventDispatcher(new EventDispatcher() {
			@Override
			public Event dispatchEvent(Event event, EventDispatchChain tail) {
				if(event instanceof KeyEvent && event.getEventType().equals(KeyEvent.KEY_PRESSED)){
					KeyCode kc = ((KeyEvent) event).getCode();

					if(kc.equals(KeyCode.SPACE)){
						event.consume();
					}
					else if(kc.equals(KeyCode.F2)){
						treeEditModeActive = true;
						fxtree.edit(getSelectedTreeItem());
					}
					else if(kc.equals(KeyCode.DOWN)){
						event.consume();
						if(getSelectedTreeItem().isExpanded() && !getSelectedTreeItem().isLeaf()){
							dataTree.setSelectedNode(dataTree.getSelectedNode().getChildNodeByIndex(0));
						}
						else{
							dataTree.setSelectedNode(dataTree.getNextNode(dataTree.getSelectedNode()));
						}
					}
					else if (kc.equals(KeyCode.UP)){
						event.consume();
						if(dataTree.getSelectedNode().getIndex() == 0){
							dataTree.setSelectedNode(dataTree.getPreviousNode(dataTree.getSelectedNode()));
						}
						else{
							TreeNode node = dataTree.getPreviousNode(dataTree.getSelectedNode());
							TreeItem<TreeNodeReference> item = getTreeItemOfTreeNode(node);

							if(item.isExpanded() && !item.isLeaf()){
								dataTree.setSelectedNode(node.getChildNodeByIndex(node.countChildNodes() - 1));
							}
							else{
								dataTree.setSelectedNode(node);
							}
						}
					}
				}
				return originalEventDispatcher.dispatchEvent(event, tail);
			}
		});

		// HotKey Events
		fxtree.setOnKeyPressed((KeyEvent event) -> {
			if(!treeEditModeActive){validateKeyPress(event);}
		});

		// Tree - Contextmenu
		treeContextMenu = new ContextMenu();

		treeContextMenu.getItems().add(createMenuItem(localeBundle.getString("mainwindow.contextmenu.add"), ImageMap.getIcon("icon_add"), (event) -> showAddTreeNodeDialog(false), true));
		treeContextMenu.getItems().add(createMenuItem(localeBundle.getString("mainwindow.contextmenu.edit"), ImageMap.getIcon("icon_edit"), (event) -> showEditCurrentNodeDialog(), true));
		treeContextMenu.getItems().add(createMenuItem(localeBundle.getString("mainwindow.contextmenu.remove"), ImageMap.getIcon("icon_delete"),(event) -> removeNode(getSelectedTreeItem()), true));
		Menu colorNodeItems = new Menu(localeBundle.getString("mainwindow.contextmenu.color"), ImageMap.getFxImageView("icon_color"));
		colorNodeItems.disableProperty().bind(treeDependentElementsDisableProperty);

		colorNodeItems.getItems().addAll( createColorContextMenu(localeBundle.getString("mainwindow.contextmenu.color.blue"), "icon_color_blue", "#00a1e7"),
										  createColorContextMenu(localeBundle.getString("mainwindow.contextmenu.color.darkblue"), "icon_color_darkblue", "#21579a"),
										  createColorContextMenu(localeBundle.getString("mainwindow.contextmenu.color.purple"), "icon_color_purple", "#a248a3"),
										  createColorContextMenu(localeBundle.getString("mainwindow.contextmenu.color.red"), "icon_color_red", "#eb1a22"),
										  createColorContextMenu(localeBundle.getString("mainwindow.contextmenu.color.orange"), "icon_color_orange", "#fd7d25"),
										  createColorContextMenu(localeBundle.getString("mainwindow.contextmenu.color.yellow"), "icon_color_yellow", "#fdf000"),
										  createColorContextMenu(localeBundle.getString("mainwindow.contextmenu.color.lime"), "icon_color_lime", "#b3e41b"),
										  createColorContextMenu(localeBundle.getString("mainwindow.contextmenu.color.green"), "icon_color_green", "#20af4a"),
										  createColorContextMenu(localeBundle.getString("mainwindow.contextmenu.color.brown"), "icon_color_brown", "#823700"),
										  createColorContextMenu(localeBundle.getString("mainwindow.contextmenu.color.black"), "icon_color_black", "#000000"),
										  createColorContextMenu(localeBundle.getString("mainwindow.contextmenu.color.none"),"", ""));

		treeContextMenu.getItems().add(colorNodeItems);

		treeContextMenu.getItems().add(createMenuItem(localeBundle.getString("mainwindow.contextmenu.copy"),
													  ImageMap.getIcon("icon_copy"),
													  (event) -> setClipboardText(getSelectedTreeNode()), true));

		treeContextMenu.getItems().add(
			createMenuItem(
				localeBundle.getString("mainwindow.contextmenu.copy_node"),	"",
				(event) -> {
					TreeNode node = getSelectedTreeNode();
					if (node != null) {
						internalClipboard = dataTree.exportNodeStructure(node);
					}
				}, true));

		treeContextMenu.getItems().add(
			createMenuItem(
					localeBundle.getString("mainwindow.contextmenu.cut_node"), "",
					(event) -> {
						TreeNode node = getSelectedTreeNode();
						if (node != null) {
							internalClipboard = dataTree.exportNodeStructure(node);
							node.remove();
						}
					}, true));

		treeContextMenu.getItems().add(
			createMenuItem(
				localeBundle.getString("mainwindow.contextmenu.insert_node"), "",
				(event) -> {
					if(internalClipboard != null){
						dataTree.importNodeStructure(getSelectedTreeNode(), internalClipboard);
						getSelectedTreeItem().setExpanded(true);
					}
				}, true));


		fxtree.setContextMenu(treeContextMenu);

		// Searching
		searchBoard = new BorderPane();
		searchBoard.setId("SearchPanel");
		searchInput = new TextField("");
		searchInput.setPromptText(localeBundle.getString("mainwindow.find.prompt_text"));
		Button startSearch = new Button(localeBundle.getString("mainwindow.find.button_text"));
		startSearch.setOnAction((event) -> {
			if(!searchInput.getText().equals("")){
				SearchResult result = NodeWalker.find(dataTree, new TextMatcher(searchInput.getText(), true, true));

				if(result.getState() == SearchState.END_REACHED){
					updateStatus(localeBundle.getString("mainwindow.find.end_of_document_reached"));
				}
				else if(result.getState() == SearchState.NOT_FOUND){
					updateStatus(localeBundle.getString("mainwindow.find.text_not_found"));
				}
			}
		});

		searchInput.addEventFilter(KeyEvent.KEY_RELEASED, (event) -> {
			if(event.getCode() == KeyCode.ENTER){
				startSearch.fire();
			}
		});

		searchBoard.setCenter(searchInput);
		searchBoard.setRight(startSearch);

		/* ===================================================================================
		 * 	Status (bottom panel)
		 * ===================================================================================
		 */

		statusLabel = new Label("");
		statusLabel.setPadding(new Insets(2, 4, 2, 4));

		BorderPane bottomPanel = new BorderPane(statusLabel);
		BorderPane.setAlignment(statusLabel, Pos.CENTER_LEFT);
		bottomPanel.setRight(notificationArea);
		bottomPanel.setId("StatusBar");
		root.setBottom(bottomPanel);

		/* ===================================================================================
		 * 	Sidebar
		 * ===================================================================================
		 */

		sidebarPanel = new BorderPane();
		sidebarPanel.setMinWidth(200);
		sidebarPanel.setId("Sidebar");
		sidebarTabPanel = new TabPane();
		sidebarTabPanel.setSide(Side.BOTTOM);
		sidebarTabPanel.setId(IdentifiableElement.SIDEBAR_TAB_PANEL.getId());
		sidebarTabPanel.setStyle("-fx-border-width: 0");
		sidebarPanel.setCenter(sidebarTabPanel);

		Label siidebarTopLabel = new Label();
		BorderPane bp = new BorderPane(siidebarTopLabel);

		siidebarTopLabel.setPadding(new Insets(2,3,3,3));

		GridPane sidebarGrid = new GridPane();
		sidebarGrid.setId("SidebarToolPanel");
		sidebarGrid.setAlignment(Pos.CENTER);
		sidebarGrid.setVgap(4);

		sidebarGrid.add(
			createSmallButton(
				localeBundle.getString("mainwindow.sidebar.collapsed.show"), "icon_add", 24, (event) -> {
				if(sidebarTabPanel.getTabs().size() > 0 && !dataTree.getSelectedNode().isRootNode()){
					showSidebar(true);
				}
				else {
					showAddTreeNodeDialog(false);
				}
			}), 0, 0);
		sidebarGrid.add(createSmallButton(localeBundle.getString("mainwindow.sidebar.collapsed.edit_node"), "icon_edit", 24, (event) -> showEditCurrentNodeDialog()), 0, 1);
		sidebarGrid.add(createSmallButton(localeBundle.getString("mainwindow.sidebar.collapsed.move_node_up"), "icon_up", 24, (event) -> dataTree.getSelectedNode().move(Direction.UP)), 0, 2);
		sidebarGrid.add(createSmallButton(localeBundle.getString("mainwindow.sidebar.collapsed.move_node_down"), "icon_down", 24, (event) -> dataTree.getSelectedNode().move(Direction.DOWN)), 0, 3);
		sidebarGrid.add(createSmallButton(localeBundle.getString("mainwindow.sidebar.collapsed.copy_text"), "icon_copy", 24, (event) -> setClipboardText(getSelectedTreeNode())), 0, 4);

		this.sidebarToolPanel = sidebarGrid;

		app.addEventHandler(TreeNodeEvent.OnSelectedItemChanged, (node) -> {
			siidebarTopLabel.setText(node.getText());
			boolean sidebarIsNotEmpty = false;
			for(FxSidebar sidebar: fxSidebarList){
				sidebarIsNotEmpty = sidebar.update(node) || sidebarIsNotEmpty;
			}

			showSidebar(sidebarIsNotEmpty);
		});

		bp.setMaxHeight(28);
		bp.getStyleClass().add("lightHeader");

		HBox hbox = new HBox(0);
		hbox.setId(IdentifiableElement.SIDEBAR_HEADER.getId());

		app.addEventHandler(DefaultEvent.OnFileClosed, () -> siidebarTopLabel.setText(""));

		if(!app.getSettingsValueAsBoolean("nodes.disable_quicklinks", false)){
			hbox.getChildren().add(createSidebarNodeLinkButton());
			hbox.getChildren().add(createQuicklinkButton());
		}

		bp.setRight(hbox);

		sidebarPanel.setTop(bp);
		showSidebar(true);
	}

	private void showCreateNewFileDialog(boolean encryptFile){
		if(!app.closeFile()){return;}

		File f = showSaveFileDialog(localeBundle.getString("mainwindow.dialogs.new_file.title"), "", "", getFileChooserExtensionFilter());
		if(f != null){
			app.createNewFile(f, encryptFile);
		}
	}

	/**
	 * This function will return an array of all supported file extensions, so that they can be used for a {@link FileChooser}
	 * @return an array of all supported file extensions
	 * @see FileChooser
	 * @see FileChooser.ExtensionFilter
	 */
	private FileChooser.ExtensionFilter[] getFileChooserExtensionFilter(){
		List<FileExtension> knownFileExtensions = app.getStorageManager().getKnownExtensions();
		FileChooser.ExtensionFilter[] arr = new FileChooser.ExtensionFilter[knownFileExtensions.size() + 1];

		for(int i = 0; i < knownFileExtensions.size(); i++){
			arr[i] = new FileChooser.ExtensionFilter(knownFileExtensions.get(i).getDescription(), knownFileExtensions.get(i).getExtension());
		}

		arr[knownFileExtensions.size()] = new FileChooser.ExtensionFilter(localeBundle.getString("filebrowser.allfiles_selector"), "*.*");
		return arr;
	}

	private MenuItem createColorContextMenu(String colorName, String iconKeyWord, String colorHTMLValue) {
		return createMenuItem(colorName, ImageMap.getIcon(iconKeyWord), (event) -> {
			TreeNode node = getSelectedTreeNode();
			if (node != null) {
				node.setColor(colorHTMLValue);
				updateStatus(localeBundle.getString("mainwindow.messages.node_color_changed"));
			}
		}, false);
	}

	/*
	 * ======================================================================================================================================================
	 * End of part "build ui"
	 * ======================================================================================================================================================
	 */

	private void startupFxUI(){

		// Define all "hot keys"

		final JavaFxUserInterfaceApi fxUI = this;

		BooleanSupplier condition_FileOpened = () -> app.isAnyFileOpened();
		BooleanSupplier condition_nodesAvailable = () -> (dataTree.getRootNode().countChildNodes() > 0);

		// Possible Key-Combinations
		addApplicationHotKey(KeyCode.A, true, false, false, new HotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				showAddTreeNodeDialog(false);
			}
		});

		addApplicationHotKey(KeyCode.E,true, false, false, new HotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				showAddTreeNodeDialog(true);
			}
		});

		addApplicationHotKey(KeyCode.C, true, false, false, new HotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				setClipboardText(getSelectedTreeNode());
			}
		});

		addApplicationHotKey(KeyCode.C, true, true, false, new HotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				internalClipboard = dataTree.exportNodeStructure(getSelectedTreeNode());
			}
		});

		addApplicationHotKey(KeyCode.U, true, false, false, new HotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				 showEditCurrentNodeDialog();
			}
		});

		addApplicationHotKey(KeyCode.DELETE, false, false, false, new HotKeyEvent(condition_nodesAvailable) {
  			@Override
  			public void onKeyDown() {
  				removeNode(getSelectedTreeItem());
  			}
  		});

		addApplicationHotKey(KeyCode.F, true, false, false, new HotKeyEvent(condition_nodesAvailable) {
				@Override
				public void onKeyDown() {
					showSearchBar(!searchBoardIsVisible);
				}
			});

		addApplicationHotKey(KeyCode.D, true, false, false, new HotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				duplicateNode(getSelectedTreeNode(), true);
			}
		});

		addApplicationHotKey(KeyCode.H, true, false, false, new HotKeyEvent(condition_nodesAvailable) {
				@Override
				public void onKeyDown() {
					FindAndReplaceDialog.showInstance(me, dataTree, fxUI);
				}
			});

		addApplicationHotKey(KeyCode.V, true, false, false, new HotKeyEvent(condition_FileOpened) {
				@Override
				public void onKeyDown() {
					InsertNodeFromClipboard();
				}
			});

		addApplicationHotKey(KeyCode.V, true, true, false, new HotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				dataTree.importNodeStructure(getSelectedTreeNode(), internalClipboard);
				getSelectedTreeItem().setExpanded(true);
			}
		});

		addApplicationHotKey(KeyCode.X, true, true, false, new HotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				internalClipboard = dataTree.exportNodeStructure(getSelectedTreeNode());
				removeNode(getSelectedTreeItem());
			}
		});

		addApplicationHotKey(KeyCode.Z, true, false, false, new HotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				undo();
			}
		});

		addApplicationHotKey(KeyCode.PAGE_UP, true, false, false, new HotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				dataTree.getSelectedNode().move(Direction.UP);
			}
		});

		addApplicationHotKey(KeyCode.LEFT, true, false, false, new HotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				dataTree.getSelectedNode().move(Direction.UP);
			}
		});

		addApplicationHotKey(KeyCode.PAGE_DOWN, true, false, false, new HotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				dataTree.getSelectedNode().move(Direction.DOWN);
			}
		});

		addApplicationHotKey(KeyCode.RIGHT, true, false, false, new HotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				dataTree.getSelectedNode().move(Direction.DOWN);
			}
		});

		addApplicationHotKey(KeyCode.O, true, false, false, new HotKeyEvent() {
			@Override
			public void onKeyDown() {
				initalizeOpenFile();
			}
		});

		addApplicationHotKey(KeyCode.S, true, false, false, new HotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				app.saveFile();
			}
		});

		addApplicationHotKey(KeyCode.S, true, true, false, new HotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				initalizeSaveFileAs();
			}
		});

		addApplicationHotKey(KeyCode.DEAD_CIRCUMFLEX, false, false, false, new HotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				node_toogleStyle(getSelectedTreeItem(), "strikeout");
			}
		});

		addApplicationHotKey(KeyCode.PLUS, true, false, false, new HotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				node_toogleStyle(getSelectedTreeItem(), "bold");
			}
		});

		addQuicklinkNodeHotKey(KeyCode.DIGIT1, 1, condition_FileOpened);
		addQuicklinkNodeHotKey(KeyCode.DIGIT2, 2, condition_FileOpened);
		addQuicklinkNodeHotKey(KeyCode.DIGIT3, 3, condition_FileOpened);
		addQuicklinkNodeHotKey(KeyCode.DIGIT4, 4, condition_FileOpened);
		addQuicklinkNodeHotKey(KeyCode.DIGIT5, 5, condition_FileOpened);
		addQuicklinkNodeHotKey(KeyCode.DIGIT6, 6, condition_FileOpened);
		addQuicklinkNodeHotKey(KeyCode.DIGIT7, 7, condition_FileOpened);
		addQuicklinkNodeHotKey(KeyCode.DIGIT8, 8, condition_FileOpened);
		addQuicklinkNodeHotKey(KeyCode.DIGIT9, 9, condition_FileOpened);
		addQuicklinkNodeHotKey(KeyCode.DIGIT0, 0, condition_FileOpened);
	}

	private void addQuicklinkNodeHotKey(KeyCode keycode, int index, BooleanSupplier condition_FileOpened){
		addApplicationHotKey(keycode, true, false, false, new HotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				followQuicklink(Integer.toString(index), false, false);
			}
		});
		addApplicationHotKey(keycode, true, true, false, new HotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				followQuicklink(Integer.toString(index), true, false);
			}
		});
		addApplicationHotKey(keycode, true, true, true, new HotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				followQuicklink(Integer.toString(index), true, true);
			}
		});
	}

	private void followQuicklink(String quicklinkName, boolean startKeyClip, boolean altKeyOption){
		if(!app.getSettingsValueAsBoolean("nodes.disable_quicklinks", false)){
			TreeNode n = app.getQuicklinkNode(quicklinkName);
			if(n != null){
				dataTree.setSelectedNode(n);
				if(startKeyClip && app.getShell().commandExists("keyclip")){
					try {
						app.getShell().runShellCommand("keyclip");
					} catch (CommandException | UserCanceledOperationException e) {
						app.alert(e.getMessage());
					}
				}
			}
		}
	}

	// To provide the HotKey-Events feature for all plugins
	@Override
	public void addApplicationHotKey(KeyCode keyCode, boolean controlKey, boolean shiftKey, boolean altKey, HotKeyEvent onKeyDown){
		String key = generatreKeyCodeString(keyCode, controlKey, shiftKey, altKey);
		if(!hotkeys.containsKey(key)){
			hotkeys.put(key, onKeyDown);
		}
	}

	private static String generatreKeyCodeString(KeyCode keyCode, boolean controlKey, boolean shiftKey, boolean altKey){
		return (controlKey ? "CTRL+" : "") + (shiftKey ? "SHIFT+" : "") + (altKey ? "ALT+" : "") + keyCode.toString();
	}

	/*
	 * ======================================================================================================================================================
	 * Tree Handling
	 * ======================================================================================================================================================
	 */

	private void buildTree(){
		treeNodeTranslator.clear();
		fxtree.getRoot().getChildren().clear();
		fxtree.getRoot().setValue(new TreeNodeReference(dataTree.getRootNode()));
		addChildNodes2FxTree(fxtree.getRoot());
	}

	private void addChildNodes2FxTree(TreeItem<TreeNodeReference> parentNode){

		parentNode.getValue().getTreeNode().forEachChildNode((childNode) -> {
			if(!treeNodeTranslator.containsKey(childNode.getId())){
				TreeNodeItem node = new TreeNodeItem(new TreeNodeReference(childNode));
				treeNodeTranslator.put(childNode.getId(), node);
				parentNode.getChildren().add(childNode.getIndex(), node);
				addChildNodes2FxTree(node);
			}
			else{
				addChildNodes2FxTree(treeNodeTranslator.get(childNode.getId()));
			}
		});
	}

	private void resetNode(TreeNode node){

		if(treeNodeTranslator.containsKey(node.getId())){
			treeNodeTranslator.get(node.getId()).setExpanded(node.isExpanded());
			fxtree.refresh();
		}
		else {

			TreeNode parentNode = node.getParentNode();
			TreeItem<TreeNodeReference> parentTreeItem = null;
			// this method will be called for every reset node.
			// If a node is more than one level "away" it will be added when its parent node is reset.

			if(treeNodeTranslator.containsKey(parentNode.getId())){
				parentTreeItem = treeNodeTranslator.get(parentNode.getId());
			}
			else if(parentNode.isRootNode()){
				parentTreeItem = fxtree.getRoot();
			}

			if(parentTreeItem != null){
				TreeNodeItem treeNodeItem = new TreeNodeItem(new TreeNodeReference(node));
				treeNodeTranslator.put(node.getId(), treeNodeItem);
				parentTreeItem.getChildren().add(node.getIndex(), treeNodeItem);
				addChildNodes2FxTree(treeNodeItem);
			}
		}
	}

	private void rebuildTreePart(TreeNode parentNode, String childNodeId) {

		if(!parentNode.isRootNode()){

			TreeItem<TreeNodeReference> fxTreeNode = treeNodeTranslator.get(parentNode.getId());

			deleteTranslatorHashItems(fxTreeNode, false);
			fxTreeNode.getChildren().clear();
			addChildNodes2FxTree(fxTreeNode);

			if(childNodeId != null){

				if(treeNodeTranslator.containsKey(childNodeId)){

					TreeNode changedNode = dataTree.getNodeById(childNodeId);
					if(changedNode != null){
						dataTree.setSelectedNode(changedNode);
					}

					TreeItem<TreeNodeReference> ti = treeNodeTranslator.get(childNodeId);
					ti.setExpanded(true);
				}
			}
		}
		else{
			int selectedIndex = fxtree.getSelectionModel().getSelectedIndex();
			buildTree();
			fxtree.getSelectionModel().select(selectedIndex);
		}
	}

	private void deleteTranslatorHashItems(TreeItem<TreeNodeReference> parentNode, boolean includeParentNode){
		if(includeParentNode){
			treeNodeTranslator.remove(parentNode.getValue().getNodeId());
		}

		parentNode.getChildren().forEach((node) -> {
			treeNodeTranslator.remove(node.getValue().getNodeId());
			deleteTranslatorHashItems(node, false);
		});
	}

	private void displayNewTreePart(TreeNode newNode) {
		TreeNodeItem node = new TreeNodeItem(new TreeNodeReference(newNode));
		treeNodeTranslator.put(newNode.getId(), node);

		TreeNode parentNode = newNode.getParentNode();
		if(parentNode.isRootNode()){
			fxtree.getRoot().getChildren().add(newNode.getIndex(), node);
		}
		else{
			treeNodeTranslator.get(parentNode.getId()).getChildren().add(newNode.getIndex(), node);
		}

		if(newNode.countChildNodes() != 0){addChildNodes2FxTree(node);}
	}

	public TreeItem<TreeNodeReference> getSelectedTreeItem(){
		return fxtree.getSelectionModel().getSelectedItem();
	}

	private TreeNode getSelectedTreeNode(){
		return fxtree.getSelectionModel().getSelectedItem().getValue().getTreeNode();
	}

	private void removeNode(TreeItem<TreeNodeReference> node){
		if(node != null){
			TreeNode treeNode = node.getValue().getTreeNode();
			if(treeNode != null){
				treeNode.remove();
			}
		}
	}

	private TreeItem<TreeNodeReference> getTreeItemOfTreeNode(TreeNode node){

		if(node.isRootNode()){
			return fxtree.getRoot();
		}

		if(treeNodeTranslator.containsKey(node.getId())){
			return treeNodeTranslator.get(node.getId());
		}

		alert(String.format("Internal application error: View is out of sync with data model.\nUnable to lookup node '%s' (id: '%s')",  node.getText(), node.getId()));

		if(fxtree.getRoot().getChildren().size() > 0){
			return fxtree.getRoot().getChildren().get(0);
		}
		else{
			return fxtree.getRoot();
		}
	}

	private void duplicateNode(TreeNode node, boolean selectNewNodeAfterDuplicate){
		if(node != null){
			List<TreeNode> clonedNodes = dataTree.exportNodeStructure(node);
			dataTree.importNodeStructure(node.getParentNode(), clonedNodes, node.getIndex() + 1);
			if(selectNewNodeAfterDuplicate){
				dataTree.setSelectedNode(dataTree.getNextNode(node));
			}
		}
	}

	private void editTreeItem(TreeItem<TreeNodeReference> treeitem){
		treeEditModeActive = true;
		fxtree.edit(getSelectedTreeItem());
	}

	/*
	 * ======================================================================================================================================================
	 * Node Styles
	 * ======================================================================================================================================================
	 */

	private boolean node_hasStyle(TreeItem<TreeNodeReference> treeItem, String styleClass){
		TreeNode node = treeItem.getValue().getTreeNode();
		if(node != null){
			return node.getAttribute("style").contains(styleClass + ";");
		}
		return false;
	}

	private void node_addStyle(TreeItem<TreeNodeReference> nodeRef, String styleClass){
		TreeNode node = nodeRef.getValue().getTreeNode();
		if(node != null){
			node.setAttribute("style", node.getAttribute("style") + styleClass + ";");
			fxtree.refresh();
		}
	}

	private void node_removeStyle(TreeItem<TreeNodeReference> nodeRef, String styleClass){
		TreeNode node = nodeRef.getValue().getTreeNode();
		if(node != null){
			node.setAttribute("style", node.getAttribute("style").replace(styleClass + ";", ""));
			fxtree.refresh();
		}
	}

	private void node_toogleStyle(TreeItem<TreeNodeReference> node, String styleClass){
		if(!node_hasStyle(node, styleClass)){
			node_addStyle(node, styleClass);
		}
		else{
			node_removeStyle(node, styleClass);
		}
	}

	/*
	 * ======================================================================================================================================================
	 * File Handling
	 * ======================================================================================================================================================
	 */

	private void initalizeOpenFile(){
		if(!app.closeFile()){return;}

		File f = showOpenFileDialog(localeBundle.getString("mainwindow.dialogs.open_file.title"), "", "", getFileChooserExtensionFilter());
		if(f != null){
			app.openFile(f);
		}
	}

	private void initalizeSaveFileAs(){
		File f = showSaveFileDialog(localeBundle.getString("mainwindow.dialogs.save_file.title"), app.getCurrentFile().getFilepath().getAbsolutePath(), "", getFileChooserExtensionFilter());
		if(f != null){
			String fileTypeIdentifier = app.getStorageManager().getIdentifierByExtension(Utilities.getFileExtension(f.getName()), "");
			if(fileTypeIdentifier.equals("")){
				app.alert(String.format(localeBundle.getString("mainwindow.dialogs.save_file.unsupported_filetype_message"), Utilities.getFileExtension(f.getAbsolutePath()), StorageManager.DEFAULT_FILE_TYPE));
				fileTypeIdentifier = StorageManager.DEFAULT_FILE_TYPE;
			}

			app.getCurrentFile().changeFilepath(f);
			app.getCurrentFile().changeFileTypeIdentifier(app, fileTypeIdentifier);
			app.saveFile();
		}
	}

	/*
	 * ======================================================================================================================================================
	 * Dialogs
	 * ======================================================================================================================================================
	 */

	private String lastFileDialogDirectory = System.getProperty("user.home");
	@Override
	public File showOpenFileDialog(String dialogTitle, String initalFileName, String initalDirectory, FileChooser.ExtensionFilter[] fileExtensions){
		return showFileChooser(true, dialogTitle, initalFileName, initalDirectory, fileExtensions);
	}

	@Override
	public File showSaveFileDialog(String dialogTitle, String initalFileName, String initalDirectory, FileChooser.ExtensionFilter[] fileExtensions){
		return showFileChooser(false, dialogTitle, initalFileName, initalDirectory, fileExtensions);
	}

	private File showFileChooser(Boolean showAsOpenFileDialog, String dialogTitle, String initalFileName, String initalDirectory, FileChooser.ExtensionFilter[] fileExtensions){
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(dialogTitle);

		//Set extension filter
		if(fileExtensions != null){
			  fileChooser.getExtensionFilters().addAll(fileExtensions);
		}
		else{
			  fileChooser.getExtensionFilters().addAll(getFileChooserExtensionFilter());
		}

		if(initalFileName != null && !initalFileName.equals("")){
			fileChooser.setInitialFileName(initalFileName);
		}
		else{
			if(initalDirectory != null && !initalDirectory.equals("")){
				fileChooser.setInitialDirectory(new File(initalDirectory));
				lastFileDialogDirectory = initalDirectory;
			}
			else{
				fileChooser.setInitialDirectory(new File(lastFileDialogDirectory));
			}
		}

		//Show open file dialog
		File f;
		if(showAsOpenFileDialog){f = fileChooser.showOpenDialog(me);}else{f = fileChooser.showSaveDialog(me);}

		if(f != null){
			lastFileDialogDirectory = f.getParent();
		}

		return f;
	}

	/*
	 * ======================================================================================================================================================
	 * UI
	 * ======================================================================================================================================================
	 */

	private MenuItem createMenuItem(String text, String iconname, EventHandler<ActionEvent> event, boolean add2TreeDependentItems){
		MenuItem menuItem = new MenuItem(text);
		menuItem.setOnAction(event);
		if(!iconname.equals("")){
			menuItem.setGraphic(new ImageView(new Image(iconname)));
		}

		if(add2TreeDependentItems){menuItem.disableProperty().bind(treeDependentElementsDisableProperty);}
		return menuItem;
	}

	private void showLinkNodePanel(TreeNode nodeLinkSource){
		Label l = new Label(String.format(localeBundle.getString("mainwindow.dialogs.link_nodes"), nodeLinkSource.getText()));
		l.setStyle("-fx-padding: 0 0 0 8");
		BorderPane panel = new BorderPane(l);
		BorderPane.setAlignment(l, Pos.CENTER_LEFT);

		Button ok = createSmallButton(localeBundle.getString("mainwindow.dialogs.accept_nodelink"), "icon_tick", 24, (event) -> {
			app.addLinkedNode(nodeLinkSource, dataTree.getSelectedNode().getId());
			removeTreePanel(panel);
		});
		Button cancel = createSmallButton(localeBundle.getString("cancel"), "icon_delete", 24, (event) -> removeTreePanel(panel));

		panel.setRight(new HBox(ok, cancel));
		panel.getStyleClass().add("highlighted");
		addTreePanel(panel, true);
	}

	@Override
	public void addCustomElement(Node node, IdentifiableElement parentElement) throws IllegalArgumentException {

		if(!parentElement.allowAddCustomElementOperation()){
			throw new IllegalArgumentException("You cannot add new nodes to this element.");
		}

		Pane pane = (Pane) me.getScene().lookup(parentElement.getLookupId());
		pane.getChildren().add(node);
	}

	@Override
	public Node lookupElement(IdentifiableElement element){
		return me.getScene().lookup(element.getLookupId());
	}

	@Override
	public ReadOnlyDoubleProperty getSidebarWidthProperty(){
		return sidebarPanel.widthProperty();
	}

	@Override
	public FxSidebar getCurrentSidebar(){
		int result = sidebarTabPanel.getSelectionModel().getSelectedIndex();
		return result >= 0 ? this.fxSidebarList.get(result) : null;
	}

	@Override
	public Tab addSidebarPanel(String sidebarTabTitle, FxSidebar sidebar, int index, boolean disableSidebarWhileNoFileIsOpened) {

		Tab myTabPage = new Tab(sidebarTabTitle);

		ScrollPane scrollPane = new ScrollPane(sidebar.getContainer());
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

		sidebar.getContainer().prefWidthProperty().bind(sidebarPanel.widthProperty().subtract(12));
		scrollPane.prefWidthProperty().bind(sidebarPanel.widthProperty());

		myTabPage.setContent(scrollPane);
		myTabPage.setClosable(false);
		myTabPage.setDisable(true);

		if(disableSidebarWhileNoFileIsOpened){
			app.addEventHandler(DefaultEvent.OnFileOpened, () -> myTabPage.setDisable(false));
			app.addEventHandler(DefaultEvent.OnFileClosed, () -> myTabPage.setDisable(true));
		}

		if(sidebarTabPanel.getTabs().size() < index){
			sidebarTabPanel.getTabs().add(myTabPage);
			fxSidebarList.add(sidebar);
		}
		else {
			sidebarTabPanel.getTabs().add(index, myTabPage);
			fxSidebarList.add(index, sidebar);
		}

		return myTabPage;
	}

	/**
	 * Creates a small button with an image and a tool tip, but without a text.
	 * @param tooltip the tool tip
	 * @param icon the icon
	 * @param size the size (width and height)
	 * @param onClick the event handler
	 * @return a button with your preferences
	 */
	public static Button createSmallButton(String tooltip, String icon, double size, EventHandler<ActionEvent> onClick){
		Button b = new Button("", ImageMap.getFxImageView((icon)));
		b.setMinWidth(size);
		b.setMaxWidth(size);
		b.setTooltip(new Tooltip(tooltip));
		b.getStyleClass().add("noBorder");
		if(onClick != null){b.setOnAction(onClick);}
		return b;
	}

	private Button createSidebarNodeLinkButton(){
		ContextMenu cm = new ContextMenu();
		MenuItem menuItems[] = createNodeLinkMenuItems(3);
		cm.getItems().addAll(menuItems);

		Button linkedNodes = createSmallButton(localeBundle.getString("mainwindow.sidebar.linked_nodes_button"), "icon_down", 24, (event) -> {
			updateSidebarNodeLinkContextMenu(menuItems, getSelectedTreeNode());
			runAsFXThread(() -> cm.show((Node) event.getSource(), javafx.geometry.Side.BOTTOM, 0, 0));
		});

		linkedNodes.disableProperty().bind(treeDependentElementsDisableProperty);

		return linkedNodes;
	}

	private MenuItem[] createNodeLinkMenuItems(int count){
		MenuItem menuItems[] = new MenuItem[count];
		for(int i = 0; i < count; i++){
			menuItems[i] = new MenuItem();
			menuItems[i].setUserData("");
			menuItems[i].setOnAction((event) -> {
				MenuItem me = (MenuItem) event.getSource();
				String id = (String) me.getUserData();
				if(id != null && !id.equals("")){
					TreeNode linkedNode = dataTree.getNodeById(id);
					if(linkedNode != null && !linkedNode.isRootNode()){
						dataTree.setSelectedNode(linkedNode);
					}
				}
				else{
					showLinkNodePanel(getSelectedTreeNode());
				}
			});
		}
		return menuItems;
	}

	private void updateSidebarNodeLinkContextMenu(MenuItem[] menuItems, TreeNode node){
		int i[] = new int[]{0};

		app.forEachLinkedNode(node, (linkedNode) -> {
			if(i[0] < menuItems.length){
				menuItems[i[0]].setText(linkedNode.getText());
				menuItems[i[0]].setUserData(linkedNode.getId());
				menuItems[i[0]].setVisible(true);
				i[0]++;
			}
		});

		boolean isFirst = true;
		while(i[0] < menuItems.length){
			menuItems[i[0]].setVisible(isFirst);
			if(isFirst){
				menuItems[i[0]].setText(localeBundle.getString("mainwindow.sidebar.button_add_node_link"));
				menuItems[i[0]].setUserData("");
				isFirst= false;
			}
			i[0]++;
		}
	}

	private Button createQuicklinkButton(){
		ImageView imgview_notStarred = new ImageView(ImageMap.getIcon("icon_star"));
		ImageView imgview_starred = new ImageView(ImageMap.getIcon("icon_star_filled"));
		Button b = new Button("", imgview_notStarred);
		b.setMinWidth(24);
		b.setMaxWidth(24);
		b.setMinHeight(25);
		b.setMaxHeight(25);
		b.setTooltip(new Tooltip(localeBundle.getString("mainwindow.sidebar.star_node")));
		b.getStyleClass().add("noBorder");

		TreeNodeEventHandler handler = (node) -> b.setGraphic(node.hasAttribute(ApplicationInstance.NODE_ATTRIBUTE_QUICKLINK) ? imgview_starred : imgview_notStarred);
		imgview_notStarred.setFitHeight(16);
		imgview_notStarred.setFitWidth(16);
		imgview_starred.setFitHeight(16);
		imgview_starred.setFitWidth(16);

		app.addEventHandler(TreeNodeEvent.OnSelectedItemChanged, handler);

		b.setOnAction((event) -> {
			TreeNode node = getSelectedTreeNode();
			if(node.hasAttribute(ApplicationInstance.NODE_ATTRIBUTE_QUICKLINK)){
				app.removeQuicklink(node.getAttribute(ApplicationInstance.NODE_ATTRIBUTE_QUICKLINK));
				handler.eventFired(node);
			}
			else{
				Set<String> quciklinks = app.getQuicklinks();
				for(int i = 1; i <= 10; i++){
					String s = (i != 10 ? Integer.toString(i) : "0");
					if(!quciklinks.contains(s)){
						app.addQuicklink(s, node);
						handler.eventFired(node);
						return;
					}
				}

				app.alert(localeBundle.getString("mainwindow.messages.max_quicklinks"));
			}
		});

		b.disableProperty().bind(treeDependentElementsDisableProperty);

		return b;
	}

	private boolean lastSidebarVisiblityValue = false;
	private void showSidebar(boolean value){
		if(value != lastSidebarVisiblityValue){
			if(value){
				rootPanel.setRight(null);
				splitPane.getItems().add(sidebarPanel);
				splitPane.setDividerPositions((double) splitPane.getUserData());
				lastSidebarVisiblityValue = value;
			}
			else if(app.getSettingsValueAsBoolean("sidebar.autohide", true)){
				if(splitPane.getDividerPositions().length > 0){
					splitPane.setUserData(splitPane.getDividerPositions()[0]);
				}

				rootPanel.setRight(sidebarToolPanel);
				splitPane.getItems().remove(sidebarPanel);
				lastSidebarVisiblityValue = value;
			}
		}
	}

	@Override
	public void addMenuEntry(MenuItem item, MenuEntryPosition pos, boolean add2TreeDependentItems) {
		switch(pos){
			case FILE:
				menu_File.getItems().add(item);
				break;

			case EDIT:
				menu_Edit.getItems().add(item);
				break;

			case VIEW:
				menu_View.getItems().add(item);
				break;

			case CONTEXTMENU:
				treeContextMenu.getItems().add(item);
				break;

			case TOOLS:
				menu_Tools.getItems().add(item);
				break;
		}

		if(add2TreeDependentItems){item.disableProperty().bind(treeDependentElementsDisableProperty);}
	}

	@Override
	public void addMenu(Menu menu, boolean add2TreeDependentItems){
		menuBar.getMenus().add(menu);
		if(add2TreeDependentItems){menu.disableProperty().bind(treeDependentElementsDisableProperty);}
	}

	@Override
	public synchronized String getClipboardText() {
		if(clipboard.hasString() || clipboard.hasHtml() || clipboard.hasUrl()){
			return clipboard.getString();
		}
		return "";
	}

	private void setClipboardText(TreeNode node) {
		if(node != null){
			setClipboardText(node.getText());
		}
	}

	@Override
	public synchronized void setClipboardText(String text) {
		if(Platform.isFxApplicationThread()){
			ClipboardContent clipboard_content = new ClipboardContent();
			clipboard_content.putString(text);
			clipboard.setContent(clipboard_content);
			updateStatus(localeBundle.getString("mainwindow.messages.text_copied_to_clipboard"));
		}
		else{
			Platform.runLater(() -> setClipboardText(text));
		}
	}

	@Override
	public void updateStatus(String text){
		if(Platform.isFxApplicationThread()){
			changeStatusText(text);
		}
		else{
			Platform.runLater(() -> changeStatusText(text));
		}

		java.util.Timer t = new Timer();
		t.schedule(new java.util.TimerTask() {
			  @Override
			  public void run() {
				  runAsFXThread(new Runnable() {
					@Override
					public void run() {
						statusLabel.setStyle("-fx-text-fill: black");
					}
				});
			  }
			}, 500);
	}

	private void changeStatusText(String text){
		statusLabel.setText(text);
		statusLabel.setStyle("-fx-text-fill: -fx-accent");
	}

	@Override
	public void log(String text) {
		app.println(text);
	}

	@Override
	public void alert(String text) {
		if(isUserInterfaceThread()){
			alert(AlertType.INFORMATION, "", null, text);
		}
		else{
			runAsFXThread(() -> {alert(AlertType.INFORMATION, "", null, text);});
		}
	}

	@Override
	public void alert(AlertType type, String title, String headline, String contentText){
		final String alertTile = title.equals("") ? ApplicationInstance.APP_NAME : title;

		if(isUserInterfaceThread()){
			showAlert(type, alertTile, headline, contentText);
		}
		else{
			runAsFXThread(() -> {showAlert(type, alertTile, headline, contentText);});
		}
	}

	private void showAlert(AlertType type, String title, String headline, String text){
		Alert msg = new Alert(type);
		msg.setTitle(title);
		msg.setHeaderText(headline);
		msg.setContentText(text);
		Stage s = (Stage) msg.getDialogPane().getScene().getWindow();
		ImageMap.addDefaultIconsToStage(s);
		s.initOwner(me);
		StylesheetMap.assignDefaultStylesheet(s.getScene());
		msg.showAndWait();
	}

	@Override
	public String getStringInput(String windowTitle, String labelText, String defaultValue) throws UserCanceledOperationException{
		InputDialog id = new InputDialog(me, this, windowTitle, labelText, defaultValue, false);
		return id.getInput();
	}

	@Override
	public char[] getPasswordInput(String title, String text, String passwordHint) throws UserCanceledOperationException {
		InputDialog id = new InputDialog(me, this, title, text, passwordHint, true);
		return id.getInput().toCharArray();
	}

	/*
	 * Shows a dialog to add a new tree node to the currently selected node
	 */
	private void showAddTreeNodeDialog(boolean add2Root){
		InputDialog id = new InputDialog(me, this, localeBundle.getString("mainwindow.dialogs.add_node.title"), localeBundle.getString("mainwindow.dialogs.add_node.text"), "", false);
		try {
			String input = id.getInput();

			if(input != null && !input.equals("")){
				TreeNode newNode = new DefaultTreeNode(input);
				if(add2Root){
					dataTree.getRootNode().addChildNode(newNode);
					dataTree.setSelectedNode(newNode);
				}
				else{
					TreeNode selectedNode = dataTree.getSelectedNode();
					selectedNode.addChildNode(newNode);

					if(!selectedNode.isRootNode()){
						fxtree.getSelectionModel().getSelectedItem().setExpanded(true);
					}
				}
			}
		} catch (UserCanceledOperationException e) {}

		me.requestFocus();
	}

	private void showEditCurrentNodeDialog(){
		TreeNode selectedNode = getSelectedTreeNode();

		if(selectedNode == null){
			return;
		}

		InputDialog id = new InputDialog(this, localeBundle.getString("mainwindow.dialogs.edit_node.text"), localeBundle.getString("mainwindow.dialogs.edit_node.text"), selectedNode.getText(), false);
		try {
			String value = id.getInput();
			if(!value.equals("")){
				selectedNode.setText(value);
			}
		} catch (UserCanceledOperationException e) {}
		me.requestFocus();
	}

	private void validateKeyPress(KeyEvent event){
		String keyCodeStr = generatreKeyCodeString(event.getCode(), event.isControlDown(), event.isShiftDown(), event.isAltDown());
		if(hotkeys.containsKey(keyCodeStr)){hotkeys.get(keyCodeStr).fireEvent();}
	}

	private void InsertNodeFromClipboard(){
		String clipboardText = getClipboardText();
		if(!clipboardText.trim().equals("")){
			TreeNode newNode = new DefaultTreeNode(clipboardText);
			TreeNode selectedNode = getSelectedTreeNode();
			if (selectedNode == null) {
				dataTree.getRootNode().addChildNode(newNode);;
			}
			else {
				selectedNode.addChildNode(newNode);
				fxtree.getSelectionModel().getSelectedItem().setExpanded(true);
			}

			updateStatus(localeBundle.getString("mainwindow.messages.node_insert_from_clipboard"));
		}
	}

	private boolean searchBoardIsVisible = false;
	private void showSearchBar(boolean show){
		if(show){
			centerPane.setBottom(searchBoard);
			searchInput.requestFocus();
			searchBoardIsVisible = true;
		}
		else{
			centerPane.setBottom(null);
			searchBoardIsVisible = false;
		}
	}

	/**
	 * Show a save changes dialog
	 * @return {@code true} if the changes should be saved or {@code false} should be discarded
	 * @throws UserCanceledOperationException if the user has pressed the "Cancel" button
	 */
	private boolean showSaveChangesDialog() throws UserCanceledOperationException {
		SaveChangesDialog.Result r = SaveChangesDialog.show(this);
		if(r == Result.Cancel){throw new UserCanceledOperationException("Cancel button was pressed.");}
		return (r == Result.SaveChanges);
	}

	@Override
	/**
	 * Display a simple yes/no dialog to the user
	 * @return TRUE if the user has clicked "YES", false if she/he chooses "No"
	 **/
	public boolean getYesNoChoice(String windowTitle, String headline, String contentText){
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(windowTitle);
		alert.setHeaderText(headline);
		alert.setContentText(contentText);
		Stage s = (Stage) alert.getDialogPane().getScene().getWindow();
		ImageMap.addDefaultIconsToStage(s);
		StylesheetMap.assignDefaultStylesheet(alert.getDialogPane().getScene());
		s.initOwner(me);

		ButtonType buttonYes = new ButtonType(localeBundle.getString("yes"), ButtonData.YES);
		ButtonType buttonNo = new ButtonType(localeBundle.getString("no"), ButtonData.NO);

		alert.getButtonTypes().setAll(buttonYes, buttonNo);

		java.util.Optional<ButtonType> result = alert.showAndWait();
		return result.get()  == buttonYes ? true : false;
	}

	@Override
	public void addNotificationItem(Node item, boolean assignToThisFile){
		if(Platform.isFxApplicationThread()){
			notificationArea.getChildren().add(item);
			if(assignToThisFile){assignedNotificationsItems.add(item);}
		}
		else{
			Platform.runLater(() -> {notificationArea.getChildren().add(item); if(assignToThisFile){assignedNotificationsItems.add(item);}});
		}
	}

	@Override
	public void removeNotificationItem(Node item){
		notificationArea.getChildren().remove(item);
		if(assignedNotificationsItems.contains(item)){
			assignedNotificationsItems.remove(item);
		}
	}

	@Override
	public void addTreePanel(Node item, boolean assignToThisFile){
		if(Platform.isFxApplicationThread()){
			panelStack.getChildren().add(item);
			if(assignToThisFile){assignedNotificationsItems.add(item);}
		}
		else{
			Platform.runLater(() -> {panelStack.getChildren().add(item); if(assignToThisFile){assignedNotificationsItems.add(item);}});
		}
	}

	@Override
	public void removeTreePanel(Node item){
		panelStack.getChildren().remove(item);
		if(assignedNotificationsItems.contains(item)){
			assignedNotificationsItems.remove(item);
		}
	}

	public void updateQuicklinkList(){
		if(menu_Quicklinks == null){
			menu_Quicklinks = new Menu(localeBundle.getString("mainwindow.menu.quicklinks"));
		}
		else{
			clearQuicklinkList(false);
		}

		app.getQuicklinks().stream().filter((s) -> s.matches("[0-9]")).forEach((name) -> {
			TreeNode node = app.getQuicklinkNode(name);
			if(node == null){return;}

			MenuItem menuItem = new MenuItem(String.format("%s: %s", name, node.getText()));
			menuItem.setUserData(name);
			menuItem.setOnAction((event) -> {
				TreeNode quicklinkNode = app.getQuicklinkNode(name);
				if (quicklinkNode != null) {
					dataTree.setSelectedNode(quicklinkNode);
				}
				else {
					app.log(String.format("Unable to find node with id '%s'.", name));
				}
			});

			menu_Quicklinks.getItems().add(menuItem);
		});

		if(menu_Quicklinks.getItems().size() > 0){
			menuBar.getMenus().add(menu_Quicklinks);
		}
		else{
			menu_Quicklinks = null;
		}
	}

	private void clearQuicklinkList(boolean setNull){
		if(menu_Quicklinks != null){
			menu_Quicklinks.getItems().clear();
			menuBar.getMenus().remove(menu_Quicklinks);
			if(setNull){menu_Quicklinks = null;}
		}
	}

	private void undo(){
		if(!dataTree.undo(true)){
			updateStatus(localeBundle.getString("mainwindow.messages.undo_limit_reached"));
		}
	}
}
