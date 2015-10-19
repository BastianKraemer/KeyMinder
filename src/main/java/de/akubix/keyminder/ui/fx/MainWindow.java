/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	MainWindow.java

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.akubix.keyminder.ui.fx;
	
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Timer;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.core.interfaces.events.DefaultEventHandler;
import de.akubix.keyminder.core.interfaces.events.EventTypes.DefaultEvent;
import de.akubix.keyminder.core.interfaces.events.EventTypes.TreeNodeEvent;
import de.akubix.keyminder.core.interfaces.events.TreeNodeEventHandler;
import de.akubix.keyminder.lib.gui.ImageSelector;
import de.akubix.keyminder.lib.gui.StyleSelector;
import de.akubix.keyminder.lib.gui.StyleSelector.WindowSelector;
import de.akubix.keyminder.ui.fx.dialogs.FindAndReplaceDialog;
import de.akubix.keyminder.ui.fx.dialogs.InputDialog;
import de.akubix.keyminder.ui.fx.dialogs.SaveChangesDialog.Result;
import de.akubix.keyminder.ui.fx.shadow.FxHotKeyEvent;
import de.akubix.keyminder.ui.fx.shadow.Precondition;
import javafx.application.Application;
import javafx.application.Platform;
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
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;

public class MainWindow extends Application implements de.akubix.keyminder.core.interfaces.FxAdministrationInterface {

	private HashMap<TreeNode, TreeItem<TreeNode>> treeNodeTranslator = new HashMap<>();
	
	private static ApplicationInstance app;
	private de.akubix.keyminder.core.db.Tree dataTree;
	
	public static void init(String[] args, ApplicationInstance instance) {
		app = instance;
		launch(args);
	}
	
	private boolean nextSelectedItemChangeEventWasFiredByMe = false;
	
	private boolean treeEditModeActive = false;
	
	private TreeView<TreeNode> fxtree;
	private Stage me;

	private BorderPane rootPanel;
	private ContextMenu treeContextMenu;

	private SplitPane splitPane;
	private BorderPane sidebarPanel;
	private TabPane sidebarTabPanel;
	private Node sidebarToolPanel;
	private Button keyClipSidebarButton;
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
	private Menu menu_FavoriteNodes = null;
	
	private SimpleBooleanProperty treeDependentElementsDisableProperty = new SimpleBooleanProperty(true);
	private List<Node> assignedNotificationsItems = new ArrayList<>(2);
	private ResourceBundle localeBundle;

	final Clipboard clipboard = Clipboard.getSystemClipboard();
	
	private Map<String, FxHotKeyEvent> hotkeys = new HashMap<>();
	
	/*
	 * ======================================================================================================================================================
	 * TreeCell Factory Class
	 *
	 * This class is based on an example mentioned in the official JavaFX documentation
	 *
	 * JavaFX/Using JavaFX UI Controls, Release 2.2, Primary Author: Alla Redko
	 * https://docs.oracle.com/javafx/2/ui_controls/tree-view.htm
	 * ======================================================================================================================================================
	 */
	
	final class TextFieldTreeCellImpl extends TreeCell<TreeNode> {
		 
		private TextField textField;
		public TextFieldTreeCellImpl() {
			super();
		}

		@Override
		public void startEdit() {
			if(treeEditModeActive)
			{
				super.startEdit();

				if (textField == null) {
					createTextField();
				}
				else
				{
					textField.setText(getItem().getText());
				}
				setText(null);
				setGraphic(textField);
				textField.requestFocus();
				textField.selectAll();
			}
		}

		@Override
		public void commitEdit(TreeNode newValue){
			super.commitEdit(newValue);
			treeEditModeActive = false;
		};

		@Override
		public void cancelEdit() {
			super.cancelEdit();

			setText((String) getItem().getText());
			setGraphic(getTreeItem().getGraphic());
			treeEditModeActive = false;
		}

		@Override
		public void updateItem(TreeNode item, boolean empty) {
			super.updateItem(item, empty);

			if (empty) {
				setText(null);
				setGraphic(null);
			} else {
				if (isEditing()) {
					if (textField != null) {
						textField.setText(getString());
					}
					setText(null);
					setGraphic(textField);
					treeEditModeActive = false;
				} else {
					setText(getString());
					
					this.getStyleClass().removeAll("bold", "italic", "strikeout");
					if(item.hasAttribute("style"))
					{
						this.getStyleClass().addAll(item.getAttribute("style").split(";"));
					}

					if(item.getColor().equals(""))
					{
						setTextFill(Color.BLACK);
					}
					else
					{
						setTextFill(Color.web(item.getColor()));
					}
					
					setGraphic(getTreeItem().getGraphic());
				}
			}
		}

		private void createTextField() {
			textField = new TextField(getString());
			textField.setOnKeyReleased(new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent t) {
					if (t.getCode() == KeyCode.ENTER) {
						commitEdit(getSelectedTreeNode().setText(textField.getText()));
					} else if (t.getCode() == KeyCode.ESCAPE) {
						cancelEdit();
					}
				}
			});
		}

		private String getString() {
			TreeNode item = getItem();
			return item == null ? "" : item.getText();
		}
	}

	/*
	 * ======================================================================================================================================================
	 * A TreeView-Skin to check if a node is visible on screen
	 * ======================================================================================================================================================
	 */

	/* The following code was written by StackOverflow (stackoverflow.com) user Ahmed and is licensed under CC BY-SA 3.0 
	 * "Creative Commons Attribution-ShareAlike 3.0 Unported", http://creativecommons.org/licenses/by-sa/3.0/)
	 *
	 * Source: http://stackoverflow.com/questions/27059701/javafx-in-treeview-need-only-scroll-to-index-number-when-treeitem-is-out-of-vie
	 * The code has not been modified.
	 */

	/**
	 * Only done as a workaround. If the selected node changes (maybe because of a "cd" command) the tree view might not scroll to it.
	 *
	 * WARNING: This method relies on classes, which does not contain to the Java API
	 */
	@SuppressWarnings("restriction")
	final class FolderTreeViewSkin extends com.sun.javafx.scene.control.skin.TreeViewSkin<TreeNode>
	{
		public FolderTreeViewSkin(TreeView<TreeNode> treeView)
		{
			super(treeView);
		}

		public boolean isIndexVisible(int index)
		{
			if (flow.getFirstVisibleCell() != null &&
				flow.getLastVisibleCell() != null &&
				flow.getFirstVisibleCell().getIndex() <= index &&
				flow.getLastVisibleCell().getIndex() >= index)
				return true;
			return false;
		}
	}

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

			dataTree = app.getTree();

			/* ================================================================================================================
			 * Language pack
			 * ================================================================================================================
			 */

			localeBundle = ResourceBundle.getBundle("de.akubix.keyminder.bundles.LangPackBundle", app.applicationLocale);
			//localeBundle = ResourceBundle.getBundle("bundles.LangBundle", new Locale("en", "EN"));

			/* ================================================================================================================
			 * Build user interface
			 * ================================================================================================================
			 */

			// Generate the whole graphical user interface
			buildUI(rootPanel);
			
			StyleSelector.assignStylesheets(scene, WindowSelector.MainWindow);
			me.setScene(scene);
			me.setMinWidth(640);
			me.setMinHeight(400);
			
			app.registerFXUserInterface(this);
			
			/* ================================================================================================================
			 * Event Registration
			 * ================================================================================================================
			 */
			
			app.addEventHandler(TreeNodeEvent.OnSelectedItemChanged, (node) -> selectedNodeChanged(node));
			
			app.addEventHandler(TreeNodeEvent.OnNodeAdded, new TreeNodeEventHandler() {
				
				@Override
				public void eventFired(TreeNode node) {
					displayNewTreePart(node);
				}
			});
			
			app.addEventHandler(TreeNodeEvent.OnNodeEdited, new TreeNodeEventHandler() {
				
				@Override
				public void eventFired(TreeNode node) {
						updateTree(getTreeItemOfTreeNode(node));
				}
			});
			
			app.addEventHandler(TreeNodeEvent.OnNodeVerticallyMoved, new TreeNodeEventHandler() {
				
				@Override
				public void eventFired(TreeNode node) {
					rebuildTreePart(node.getParentNode());
				}
			});
			
			app.addEventHandler(TreeNodeEvent.OnNodeRemoved, new TreeNodeEventHandler() {
				
				@Override
				public void eventFired(TreeNode node) {
					TreeItem<TreeNode> treeitem = getTreeItemOfTreeNode(node);
					treeitem.getParent().getChildren().remove(treeitem);
					deleteTranslatorHashItems(treeitem, true);
				}
			});
			
			app.addEventHandler(DefaultEvent.OnFileClosed, new DefaultEventHandler() {
				@Override
				public void eventFired() {
					nextSelectedItemChangeEventWasFiredByMe = true;

					//The node will belong to notificationArea or panelStack.
					if(assignedNotificationsItems.size() > 0){
						assignedNotificationsItems.forEach((node) -> {notificationArea.getChildren().remove(node); panelStack.getChildren().remove(node);});
						assignedNotificationsItems.clear();
					}

					fxtree.getRoot().getChildren().clear();
					treeNodeTranslator.clear();
					treeDependentElementsDisableProperty.set(true);
					clearFavoriteNodeList(true);
				}
			});
			
			app.addEventHandler(DefaultEvent.OnExit, new DefaultEventHandler() {
				@Override
				public void eventFired() {
					me.close();
				}
			});
			
			startupFxUI();
			
			if(!de.akubix.keyminder.lib.AESCore.isAES256Supported()){
					// Important security note
					Button notification = new Button("", ImageSelector.getFxImageView(("icon_warning")));
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
					if(dataTree.treeHasBeenUpdated()){
						de.akubix.keyminder.ui.fx.dialogs.SaveChangesDialog.Result r = de.akubix.keyminder.ui.fx.dialogs.SaveChangesDialog.show(this);
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
			
			// Startup the application core (load the optional modules, ...)
			app.startup();
			
			// Show the main window
			me.getIcons().add(new Image(ApplicationInstance.APP_ICON));
			me.show();
			
			runAsFXThread(new Runnable() {
				
				@Override
				public void run() {
					// Load the default password file
					app.loadDefaultFile();
					
					if(fxtree.getRoot().getChildren().size() == 1)
					{
						if(!fxtree.getRoot().getChildren().get(0).isLeaf()){fxtree.getRoot().getChildren().get(0).setExpanded(true);}
					}
				}
			});
	}
	
	private void selectedNodeChanged(TreeNode selectedNode){
		if(!nextSelectedItemChangeEventWasFiredByMe)
		{
			if(getTreeItemOfTreeNode(selectedNode) != fxtree.getRoot())
			{
				if(getTreeItemOfTreeNode(selectedNode) != getSelectedTreeItem()){nextSelectedItemChangeEventWasFiredByMe = true;} // This value will be reset by the fxtree change listener
				fxtree.getSelectionModel().select(getTreeItemOfTreeNode(selectedNode));

				// Take a look at the class "FolderTreeViewSkin" above
				if (!((FolderTreeViewSkin) fxtree.getSkin()).isIndexVisible(fxtree.getSelectionModel().getSelectedIndex()))
				{
					fxtree.scrollTo(fxtree.getSelectionModel().getSelectedIndex() - 3);
				}
			}
		}
		else
		{
			nextSelectedItemChangeEventWasFiredByMe = false;
		}
	}

	@Override
	public void onFileOpenedHandler(){
		buildTree();
		if(fxtree.getRoot().getChildren().size() > 0)
		{
			fxtree.getSelectionModel().select((fxtree.getRoot().getChildren().get(0)));
		}

		nextSelectedItemChangeEventWasFiredByMe = false;
		lastFileDialogDirectory = app.currentFile.getFilepath().getParent();
		treeDependentElementsDisableProperty.set(false);

		if(dataTree.getSelectedNode().getId() != 0){
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
	public void setTitle(String title){
		me.setTitle(title);
	}
	
	@Override
	public void runAsFXThread(Runnable r){
		Platform.runLater(r);
	}

	@Override
	public boolean isFXThread(){
		return Platform.isFxApplicationThread();
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
		
		Menu file_new = new Menu(localeBundle.getString("mainwindow.menu.file.new"), ImageSelector.getFxImageView("icon_newfile"));
		file_new.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.file.new.encrypted_file"), ImageSelector.getIcon("icon_new_encrypted_file"),
											  (event) -> showCreateNewFileDialog(true), false));
		
		file_new.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.file.new.regular_file"), ImageSelector.getIcon("icon_newfile"),
				  (event) -> showCreateNewFileDialog(false), false));
		
		menu_File.getItems().add(file_new);


		menu_File.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.file.open"),
												ImageSelector.getIcon("icon_openfile"),
												(event) -> initalizeOpenFile(), false));

		menu_File.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.file.save"),
												ImageSelector.getIcon("icon_save"),
												(event) -> app.saveFile(), true));
	
		menu_File.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.file.saveas"), "",
												(event) -> initalizeSaveFileAs(), true));

		menu_File.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.file.close"), "", new EventHandler<ActionEvent>() {
							@Override public void handle(ActionEvent e)
							{
								if(app.currentFile != null)
								{
									if(app.closeFile())
									{
										updateStatus(localeBundle.getString("mainwindow.messages.file_successfully_closed"));
									}
								}
							}}, true));

		if(app.settings.containsKey("ui.filelist")){
			Menu openFileMenu = new Menu(localeBundle.getString("mainwindow.menu.file.recently_used"));
			String[] myFiles = app.settings.get("ui.filelist").split(";");
			for(int i = 0; i < myFiles.length; i++){
				File f = new File(myFiles[i]);
				MenuItem item = createMenuItem(f.getName(), "", (event) -> app.openFile((File) ((MenuItem) event.getSource()).getUserData()), false);
				item.setUserData(f);
				openFileMenu.getItems().add( item);
			}
			menu_File.getItems().add(openFileMenu);
		}
		
		menu_File.getItems().addAll(new SeparatorMenuItem(), createMenuItem(localeBundle.getString("mainwindow.menu.file.filesettings"), "", new EventHandler<ActionEvent>() {
			@Override public void handle(ActionEvent e){
				de.akubix.keyminder.ui.fx.dialogs.FileSettingsDialog fsd = new de.akubix.keyminder.ui.fx.dialogs.FileSettingsDialog(me, app);
				fsd.show();
				me.requestFocus();
			}}, true));

		// --- Menu Edit
		menu_Edit = new Menu(localeBundle.getString("mainwindow.menu.edit"));
 
		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.settings"), ImageSelector.getIcon("icon_settings"), new EventHandler<ActionEvent>() {
			@Override public void handle(ActionEvent e)
			{
				de.akubix.keyminder.ui.fx.dialogs.SettingsDialog sd = new de.akubix.keyminder.ui.fx.dialogs.SettingsDialog(me, app);
				sd.show();
				me.requestFocus();
			}}, false));
		
		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.duplicate_node"), "",
												(event) -> duplicateNode(getSelectedTreeNode(), true), true));
		
		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.copy_text"),
												ImageSelector.getIcon("icon_copy"),
												(event) -> setClipboardText(getSelectedTreeNode().getText()), true));

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.insert_text"),	"",
												(event) -> InsertNodeFromClipboard(), true));

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.find"),
												ImageSelector.getIcon("icon_find"), (event) -> showSearchBar(!searchBoardIsVisible), true));

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.find_replace"), "",
												(ActionEvent e) -> FindAndReplaceDialog.showInstance(me, dataTree, this), true));

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.add_root_node"),
												ImageSelector.getIcon("icon_add"), (event) -> showAddTreeNodeDialog(true), true));

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.undo"),
												ImageSelector.getIcon("icon_undo"), (event) -> undo(), true));
		
		// Menu entry to format nodes (bold, italic, strikeout)
		Menu nodeFontSettings = new Menu(localeBundle.getString("mainwindow.menu.edit.style"));

		final CheckMenuItem boldNode = new CheckMenuItem(localeBundle.getString("mainwindow.menu.edit.style.bold"));
		boldNode.setOnAction((event) -> node_toogleStyle(getSelectedTreeItem(), "bold"));

		final CheckMenuItem italicNode = new CheckMenuItem(localeBundle.getString("mainwindow.menu.edit.style.italic"));
		italicNode.setOnAction((event) -> node_toogleStyle(getSelectedTreeItem(), "italic"));

		final CheckMenuItem strikeoutNode = new CheckMenuItem(localeBundle.getString("mainwindow.menu.edit.style.strikeout"));
		strikeoutNode.setOnAction((event) -> node_toogleStyle(getSelectedTreeItem(), "strikeout"));

		nodeFontSettings.setOnShowing(new EventHandler<Event>() {
			@Override
			public void handle(Event event) {
				TreeItem<TreeNode> n = getSelectedTreeItem();
				boldNode.setSelected(node_hasStyle(n, "bold"));
				italicNode.setSelected(node_hasStyle(n, "italic"));
				strikeoutNode.setSelected(node_hasStyle(n, "strikeout"));
		}});

		nodeFontSettings.getItems().addAll(boldNode, italicNode, strikeoutNode);
		nodeFontSettings.disableProperty().bind(treeDependentElementsDisableProperty);

		menu_Edit.getItems().add(nodeFontSettings);

		// Sort nodes

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.sort"),
												ImageSelector.getIcon("icon_sort"),
												(event) -> dataTree.sortChildNodes(getSelectedTreeNode(), false), true));

		// Menu entry for vertical node moving

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.move_node_up"),
												ImageSelector.getIcon("icon_up"),
												(event) -> dataTree.moveNodeVertical(dataTree.getSelectedNode(), -1), true));

		menu_Edit.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.edit.move_node_down"),
												ImageSelector.getIcon("icon_down"),
												(event) -> dataTree.moveNodeVertical(dataTree.getSelectedNode(), 1), true));

		// --- Menu View
		menu_View = new Menu(localeBundle.getString("mainwindow.menu.view"));

		menu_View.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.view.expand"),
												ImageSelector.getIcon("icon_add"),
												(event) -> getSelectedTreeItem().setExpanded(true), true));

		menu_View.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.view.collapse"),
												ImageSelector.getIcon("icon_remove"),
												(event) -> getSelectedTreeItem().setExpanded(false), true));

		// --- Menu Tools
		menu_Tools = new Menu(localeBundle.getString("mainwindow.menu.tools"));
		menu_Tools.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.tools.terminal"),
												 ImageSelector.getIcon("icon_bash"),
												 (event) -> new Terminal(app).show(), false));
		
		// --- Menu Extras
		menu_Extras = new Menu(localeBundle.getString("mainwindow.menu.extras"));
		menu_Extras.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.extras.nodeinfo"),
												  ImageSelector.getIcon("icon_info"),
												  (event) -> {
													  new de.akubix.keyminder.ui.fx.dialogs.NodeInfoDialog(dataTree.getSelectedNode(), app).show(me);
													  me.requestFocus();
												  }, true));
		
		menu_Extras.getItems().add(createMenuItem(localeBundle.getString("mainwindow.menu.extras.appinfo"),
				  ImageSelector.getIcon("icon_star_filled"),
				  (event) -> {new de.akubix.keyminder.ui.fx.About(app).show();}, false));

		menuBar.getMenus().addAll(menu_File, menu_Edit, menu_View, menu_Extras, menu_Tools);
		root.setTop(menuBar);
		
		
		/* ===================================================================================
		 * 	Treeview
		 * ===================================================================================
		 */
		// Node rootIcon = new ImageView(new Image(getClass().getResourceAsStream("file://...")));

		TreeItem<TreeNode> rootItem = new TreeItem<TreeNode>(dataTree.getRootNode());//, rootIcon);
		rootItem.setExpanded(true);
		fxtree = new TreeView<TreeNode> (rootItem);
		fxtree.setId("Tree");
		fxtree.setShowRoot(false);
		fxtree.setEditable(true);
		fxtree.setMinWidth(200);

		// Add a change listener to the tree
		fxtree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<TreeNode>>() {
			@Override
			public void changed(ObservableValue<? extends TreeItem<TreeNode>> observable, TreeItem<TreeNode> oldValue, TreeItem<TreeNode> newValue) {
				if(newValue != null)
				{
					if(!nextSelectedItemChangeEventWasFiredByMe)
					{
						TreeNode n = newValue.getValue();
						nextSelectedItemChangeEventWasFiredByMe = true;
						dataTree.setSelectedNode(n);
					}
					else
					{
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

		fxtree.setCellFactory(new Callback<TreeView<TreeNode>, TreeCell<TreeNode>>(){
			@Override
			public TreeCell<TreeNode> call(TreeView<TreeNode> p) {
				return new TextFieldTreeCellImpl();
			}
		});

		fxtree.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if(event.getButton() == MouseButton.MIDDLE)
				{
					treeEditModeActive = true;
					fxtree.edit(getSelectedTreeItem());
				}
			}
		});

		fxtree.setSkin(new FolderTreeViewSkin(fxtree));

		/* The following code is based on answer written by StackOverflow (stackoverflow.com) user Jos� Pereda and is licensed under CC BY-SA 3.0 
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
					else if(kc.equals(KeyCode.F2))
					{
						treeEditModeActive = true;
						fxtree.edit(getSelectedTreeItem());
					}
					else if(kc.equals(KeyCode.DOWN))
					{
						event.consume();
						if(getSelectedTreeItem().isExpanded() && !getSelectedTreeItem().isLeaf())
						{
							dataTree.setSelectedNode(dataTree.getSelectedNode().getChildNodeByIndex(0));
						}
						else
						{
							dataTree.setSelectedNode(dataTree.getNextNode(dataTree.getSelectedNode()));
						}
					}
					else if (kc.equals(KeyCode.UP))
					{
						event.consume();
						if(dataTree.getSelectedNode().getIndex() == 0)
						{
							dataTree.setSelectedNode(dataTree.getPreviousNode(dataTree.getSelectedNode()));
						}
						else
						{
							TreeNode node = dataTree.getPreviousNode(dataTree.getSelectedNode());
							TreeItem<TreeNode> item = getTreeItemOfTreeNode(node);

							if(item.isExpanded() && !item.isLeaf())
							{
								dataTree.setSelectedNode(node.getChildNodeByIndex(node.countChildNodes() - 1));
							}
							else
							{
								dataTree.setSelectedNode(node);
							}
						}
					}
				}
				return originalEventDispatcher.dispatchEvent(event, tail);
			}
		});

		// HotKey Events
		fxtree.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if(!treeEditModeActive){validateKeyPress(event);}
			}});
		
		// Tree - Contextmenu
		treeContextMenu = new ContextMenu();

		treeContextMenu.getItems().add(createMenuItem(localeBundle.getString("mainwindow.contextmenu.add"), ImageSelector.getIcon("icon_add"), (event) -> showAddTreeNodeDialog(false), true));
		treeContextMenu.getItems().add(createMenuItem(localeBundle.getString("mainwindow.contextmenu.edit"), ImageSelector.getIcon("icon_edit"), (event) -> showEditCurrentNodeDialog(), true));
		treeContextMenu.getItems().add(createMenuItem(localeBundle.getString("mainwindow.contextmenu.remove"), ImageSelector.getIcon("icon_delete"),(event) -> removeNode(getSelectedTreeItem()), true));
		Menu colorNodeItems = new Menu(localeBundle.getString("mainwindow.contextmenu.color"), ImageSelector.getFxImageView("icon_color"));
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
													  ImageSelector.getIcon("icon_copy"),
													  (event) -> setClipboardText(getSelectedTreeNode().getText()), true));

		treeContextMenu.getItems().add(createMenuItem(localeBundle.getString("mainwindow.contextmenu.copy_node"), "",
													  (event) -> dataTree.copyNodeToInternalClipboard(getSelectedTreeNode()), true));

		treeContextMenu.getItems().add(createMenuItem(localeBundle.getString("mainwindow.contextmenu.cut_node"), "",
													 (event) -> {dataTree.copyNodeToInternalClipboard(getSelectedTreeNode()); removeNode(getSelectedTreeItem());}, true));

		treeContextMenu.getItems().add(createMenuItem(localeBundle.getString("mainwindow.contextmenu.insert_node"), "",
													 (event) -> {dataTree.pasteNodeFromInternalClipboard(getSelectedTreeNode()); getSelectedTreeItem().setExpanded(true);}, true));
		
		fxtree.setContextMenu(treeContextMenu);
		
		sidebarPanel = new BorderPane();
		sidebarPanel.setMinWidth(200);
		sidebarPanel.setId("Sidebar");
		sidebarTabPanel = new TabPane();
		sidebarTabPanel.setSide(Side.BOTTOM);
		sidebarTabPanel.setId("SidebarTabPanel");
		sidebarTabPanel.setStyle("-fx-border-width: 0");
		sidebarPanel.setCenter(sidebarTabPanel);
		
		// Searching
		searchBoard = new BorderPane();
		searchBoard.setId("SearchPanel");
		searchInput = new TextField("");
		searchInput.setPromptText(localeBundle.getString("mainwindow.find.prompt_text"));
		Button startSearch = new Button(localeBundle.getString("mainwindow.find.button_text"));
		startSearch.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if(!searchInput.getText().equals(""))
				{
					de.akubix.keyminder.lib.TreeSearch.SearchResult result = de.akubix.keyminder.lib.TreeSearch.find(searchInput.getText(), dataTree, true);
					if(result == de.akubix.keyminder.lib.TreeSearch.SearchResult.END_REACHED){
						updateStatus(localeBundle.getString("mainwindow.find.end_of_document_reached"));
					}
					else if(result == de.akubix.keyminder.lib.TreeSearch.SearchResult.NOT_FOUND){
						updateStatus(localeBundle.getString("mainwindow.find.text_not_found"));
					}
				}
			}
		});

		searchInput.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				if(event.getCode() == KeyCode.ENTER)
				{
					startSearch.fire();
				}
			}});

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
	}
	
	private void showCreateNewFileDialog(boolean encryptFile){
		if(!app.closeFile()){return;}
		
		File f = showSaveFileDialog(localeBundle.getString("mainwindow.dialogs.new_file.title"), "", "", app.storageManager.getFileChooserExtensionFilter());
		if(f != null)
		{
			app.createNewFile(f, encryptFile);
		}
	}
	
	private MenuItem createColorContextMenu(String colorName, String iconKeyWord, String colorHTMLValue)
	{
		return createMenuItem(colorName, ImageSelector.getIcon(iconKeyWord), new EventHandler<ActionEvent>() {
			public void handle(ActionEvent e) {
				getSelectedTreeNode().setColor(colorHTMLValue);
				updateStatus(localeBundle.getString("mainwindow.messages.node_color_changed"));
			}}, false);
	}
	
	/*
	 * ======================================================================================================================================================
	 * End of part "build ui"
	 * ======================================================================================================================================================
	 */
	
	private void startupFxUI()
	{
		// TODO FXUI Startup
		
		// Define all "hot keys"
		
		final de.akubix.keyminder.core.interfaces.FxUserInterface fxUI = this;

		Precondition condition_FileOpened = new Precondition() {
			@Override
			public boolean check() {
				return app.currentFile != null;
			}
		};
		
		Precondition condition_nodesAvailable = new Precondition() {
			@Override
			public boolean check() {
				return (dataTree.getRootNode().countChildNodes() > 0);
			}
		};
		
		// Possible Key-Combinations
		addApplicationHotKey(KeyCode.A, true, false, false, new FxHotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				showAddTreeNodeDialog(false);
			}
		});
		
		addApplicationHotKey(KeyCode.E,true, false, false, new FxHotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				showAddTreeNodeDialog(true);
			}
		});
		
		addApplicationHotKey(KeyCode.C, true, false, false, new FxHotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				setClipboardText(getSelectedTreeNode().getText());
			}
		});
		
		addApplicationHotKey(KeyCode.C, true, true, false, new FxHotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				dataTree.copyNodeToInternalClipboard(getSelectedTreeNode());
			}
		});

		addApplicationHotKey(KeyCode.U, true, false, false, new FxHotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				 showEditCurrentNodeDialog();
			}
		});
		
		addApplicationHotKey(KeyCode.DELETE, false, false, false, new FxHotKeyEvent(condition_nodesAvailable) {
  			@Override
  			public void onKeyDown() {
  				removeNode(getSelectedTreeItem());
  			}
  		});
		
		addApplicationHotKey(KeyCode.F, true, false, false, new FxHotKeyEvent(condition_nodesAvailable) {
				@Override
				public void onKeyDown() {
					showSearchBar(!searchBoardIsVisible);
				}
			});
		
		addApplicationHotKey(KeyCode.D, true, false, false, new FxHotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				duplicateNode(getSelectedTreeNode(), true);
			}
		});
		
		addApplicationHotKey(KeyCode.H, true, false, false, new FxHotKeyEvent(condition_nodesAvailable) {
				@Override
				public void onKeyDown() {
					FindAndReplaceDialog.showInstance(me, dataTree, fxUI);
				}
			});
		
		addApplicationHotKey(KeyCode.V, true, false, false, new FxHotKeyEvent(condition_FileOpened) {
				@Override
				public void onKeyDown() {
					InsertNodeFromClipboard();
				}
			});
		
		addApplicationHotKey(KeyCode.V, true, true, false, new FxHotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				dataTree.pasteNodeFromInternalClipboard(getSelectedTreeNode());
				getSelectedTreeItem().setExpanded(true);
			}
		});

		addApplicationHotKey(KeyCode.X, true, true, false, new FxHotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				dataTree.copyNodeToInternalClipboard(getSelectedTreeNode());
				removeNode(getSelectedTreeItem());
			}
		});

		addApplicationHotKey(KeyCode.Z, true, false, false, new FxHotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				undo();
			}
		});

		addApplicationHotKey(KeyCode.PAGE_UP, true, false, false, new FxHotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				dataTree.moveNodeVertical(dataTree.getSelectedNode(), -1);
			}
		});

		addApplicationHotKey(KeyCode.LEFT, true, false, false, new FxHotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				dataTree.moveNodeVertical(dataTree.getSelectedNode(), -1);
			}
		});

		addApplicationHotKey(KeyCode.PAGE_DOWN, true, false, false, new FxHotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				dataTree.moveNodeVertical(dataTree.getSelectedNode(), 1);
			}
		});

		addApplicationHotKey(KeyCode.RIGHT, true, false, false, new FxHotKeyEvent(condition_nodesAvailable) {
			@Override
			public void onKeyDown() {
				dataTree.moveNodeVertical(dataTree.getSelectedNode(), 1);
			}
		});
		
		addApplicationHotKey(KeyCode.O, true, false, false, new FxHotKeyEvent() {
			@Override
			public void onKeyDown() {
				initalizeOpenFile();
			}
		});
		
		addApplicationHotKey(KeyCode.S, true, false, false, new FxHotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				app.saveFile();
			}
		});
		
		addApplicationHotKey(KeyCode.S, true, true, false, new FxHotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				initalizeSaveFileAs();
			}
		});

		addApplicationHotKey(KeyCode.DEAD_CIRCUMFLEX, false, false, false, new FxHotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				node_toogleStyle(getSelectedTreeItem(), "strikeout");
			}
		});

		addApplicationHotKey(KeyCode.PLUS, true, false, false, new FxHotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				node_toogleStyle(getSelectedTreeItem(), "bold");
			}
		});

		addFavoriteNodeHotKey(KeyCode.DIGIT1, (byte) 0, condition_FileOpened);
		addFavoriteNodeHotKey(KeyCode.DIGIT2, (byte) 1, condition_FileOpened);
		addFavoriteNodeHotKey(KeyCode.DIGIT3, (byte) 2, condition_FileOpened);
		addFavoriteNodeHotKey(KeyCode.DIGIT4, (byte) 3, condition_FileOpened);
		addFavoriteNodeHotKey(KeyCode.DIGIT5, (byte) 4, condition_FileOpened);
		addFavoriteNodeHotKey(KeyCode.DIGIT6, (byte) 5, condition_FileOpened);
		addFavoriteNodeHotKey(KeyCode.DIGIT7, (byte) 6, condition_FileOpened);
		addFavoriteNodeHotKey(KeyCode.DIGIT8, (byte) 7, condition_FileOpened);
		addFavoriteNodeHotKey(KeyCode.DIGIT9, (byte) 8, condition_FileOpened);
		addFavoriteNodeHotKey(KeyCode.DIGIT0, (byte) 9, condition_FileOpened);
	}


	private void addFavoriteNodeHotKey(KeyCode keycode, byte index, Precondition condition_FileOpened)
	{
		addApplicationHotKey(keycode, true, false, false, new FxHotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				goToFavoriteNode(index, false, false);
			}
		});
		addApplicationHotKey(keycode, true, true, false, new FxHotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				goToFavoriteNode(index, true, false);
			}
		});
		addApplicationHotKey(keycode, true, true, true, new FxHotKeyEvent(condition_FileOpened) {
			@Override
			public void onKeyDown() {
				goToFavoriteNode(index, true, true);
			}
		});
	}

	private void goToFavoriteNode(byte index, boolean startKeyClip, boolean pressEnter)
	{
		if(!app.getSettingsValueAsBoolean("nodes.disable_favorites", false)){
			TreeNode n = app.getFavoriteNode(index);
			if(n != null){
				dataTree.setSelectedNode(n);
				if(startKeyClip && app.commandAvailable("keyclip")){
					if(pressEnter){
						app.execute("keyclip", app.getTree().getSelectedNode().getAttribute("username"),
															 app.getTree().getSelectedNode().getAttribute("password"),
															 "yes");
					}
					else{
						app.execute("keyclip", app.getTree().getSelectedNode().getAttribute("username"),
															 app.getTree().getSelectedNode().getAttribute("password"));
					}
				}
			}
		}
	}

	// To provide the HotKey-Events feature for all modules
	@Override
	public void addApplicationHotKey(KeyCode keyCode, boolean controlKey, boolean shiftKey, boolean altKey, FxHotKeyEvent onKeyDown){
		String key = generatreKeyCodeString(keyCode, controlKey, shiftKey, altKey);
		if(!hotkeys.containsKey(key))
		{
			hotkeys.put(key, onKeyDown);
		}
	}
	
	private static String generatreKeyCodeString(KeyCode keyCode, boolean controlKey, boolean shiftKey, boolean altKey)
	{
		return (controlKey ? "CTRL+" : "") + (shiftKey ? "SHIFT+" : "") + (altKey ? "ALT+" : "") + keyCode.toString();
	}

	/*
	 * ======================================================================================================================================================
	 * Tree Handling
	 * ======================================================================================================================================================
	 */
	
	private void buildTree()
	{
		treeNodeTranslator.clear();
		fxtree.getRoot().getChildren().clear();
		fxtree.getRoot().setValue(dataTree.getRootNode());
		addChildNodes2FxTree(fxtree.getRoot());
	}
	
	private void addChildNodes2FxTree(TreeItem<TreeNode> parentNode)
	{
		parentNode.getValue().forEachChildNode((childNode) -> {
			TreeItem<TreeNode> node = new TreeItem<TreeNode>(childNode);
			treeNodeTranslator.put(childNode, node);
			parentNode.getChildren().add(node);
			addChildNodes2FxTree(node);
		});
	}
	
	private void updateTree(TreeItem<TreeNode> node) {
		if(node.isLeaf())
		{
			nextSelectedItemChangeEventWasFiredByMe = true;
			boolean wasExpanded = node.getParent().isExpanded();
			node.getParent().setExpanded(!wasExpanded);
			nextSelectedItemChangeEventWasFiredByMe = true;
			node.getParent().setExpanded(wasExpanded);
			fxtree.getSelectionModel().select(node);
		}
		else
		{
			boolean wasExpanded = node.isExpanded();
			node.setExpanded(!wasExpanded);
			node.setExpanded(wasExpanded);
		}
	}
	
	private void rebuildTreePart(TreeNode parentNode) {
		if(parentNode.getId() > 0)
		{
			TreeItem<TreeNode> fxTreeNode = treeNodeTranslator.get(parentNode);
			
			deleteTranslatorHashItems(fxTreeNode, false);
			fxTreeNode.getChildren().clear();
			addChildNodes2FxTree(fxTreeNode);
		}
		else
		{
			int selectedIndex = fxtree.getSelectionModel().getSelectedIndex();
			buildTree();
			fxtree.getSelectionModel().select(selectedIndex);
		}
	}
	
	private void deleteTranslatorHashItems(TreeItem<TreeNode> parentNode, boolean includeParentNode)
	{
		if(includeParentNode){treeNodeTranslator.remove(parentNode.getValue());}
		
		parentNode.getChildren().forEach((node) -> {
			treeNodeTranslator.remove(node.getValue());
			deleteTranslatorHashItems(node, false);
		});
	}
	
	private void displayNewTreePart(TreeNode newNode) {
		
		TreeItem<TreeNode> node = new TreeItem<TreeNode>(newNode);
		treeNodeTranslator.put(newNode, node);
		
		TreeNode parentNode = newNode.getParentNode();
		if(parentNode.getId() == 0) // -> RootNode
		{
			fxtree.getRoot().getChildren().add(node);
		}
		else
		{
			treeNodeTranslator.get(parentNode).getChildren().add(node);
		}

		if(newNode.countChildNodes() != 0){addChildNodes2FxTree(node);}
	}
	
	public TreeItem<TreeNode> getSelectedTreeItem(){
		return fxtree.getSelectionModel().getSelectedItem();
	}
	
	private TreeNode getSelectedTreeNode(){
		return fxtree.getSelectionModel().getSelectedItem().getValue();
	}
	
	private void removeNode(TreeItem<TreeNode> node)
	{
		node.getValue().getTree().removeNode(node.getValue());
	}

	private TreeItem<TreeNode> getTreeItemOfTreeNode(TreeNode node)
	{
		if(node.getId() == 0){return fxtree.getRoot();}
		if(treeNodeTranslator.containsKey(node)){return treeNodeTranslator.get(node);}
		alert("ERROR - Node '" + node.getText() + "' is not assigned to tree!");
		
		if(fxtree.getRoot().getChildren().size() > 0)
		{
			return fxtree.getRoot().getChildren().get(0);
		}
		else
		{
			return fxtree.getRoot();
		}
	}
	
	private void duplicateNode(TreeNode node, boolean selectNewNodeAfterDuplicate)
	{
		TreeNode clone = dataTree.cloneTreeNode(node, true);
		dataTree.insertNode(clone, node.getParentNode(), node.getIndex() + 1);
		if(selectNewNodeAfterDuplicate){dataTree.setSelectedNode(clone);}
	}

	/*
	 * ======================================================================================================================================================
	 * Node Styles
	 * ======================================================================================================================================================
	 */

	private boolean node_hasStyle(TreeItem<TreeNode> node, String styleClass)
	{
		return node.getValue().getAttribute("style").contains(styleClass + ";");
	}

	private void node_addStyle(TreeItem<TreeNode> node, String styleClass)
	{
		node.getValue().setAttribute("style", node.getValue().getAttribute("style") + styleClass + ";");
		updateTree(node);
	}

	private void node_removeStyle(TreeItem<TreeNode> node, String styleClass)
	{
		node.getValue().setAttribute("style", node.getValue().getAttribute("style").replace(styleClass + ";", ""));
		updateTree(node);
	}

	private void node_toogleStyle(TreeItem<TreeNode> node, String styleClass)
	{
		if(!node_hasStyle(node, styleClass))
		{
			node_addStyle(node, styleClass);
		}
		else
		{
			node_removeStyle(node, styleClass);
		}
	}

	/*
	 * ======================================================================================================================================================
	 * File Handling
	 * ======================================================================================================================================================
	 */
	
	private void initalizeOpenFile()
	{		 	
		if(!app.closeFile()){return;}

		File f = showOpenFileDialog(localeBundle.getString("mainwindow.dialogs.open_file.title"), "", "", app.storageManager.getFileChooserExtensionFilter());
		if(f != null)
		{
			app.openFile(f);
		}
	}

	private void initalizeSaveFileAs()
	{		
		File f = showSaveFileDialog(localeBundle.getString("mainwindow.dialogs.save_file.title"), app.currentFile.getFilepath().getAbsolutePath(), "", app.storageManager.getFileChooserExtensionFilter());
		if(f != null)
		{
			String fileTypeIdentifier = app.storageManager.getIdentifierByExtension(de.akubix.keyminder.lib.Tools.getFileExtension(f.getName()), "");
			if(fileTypeIdentifier.equals(""))
			{
				app.alert(String.format(localeBundle.getString("mainwindow.dialogs.save_file.unsupported_filetype_message"), de.akubix.keyminder.lib.Tools.getFileExtension(f.getAbsolutePath()), de.akubix.keyminder.core.io.StorageManager.defaultFileType));
				fileTypeIdentifier = de.akubix.keyminder.core.io.StorageManager.defaultFileType;
			}

			app.currentFile.changeFilepath(f);
			app.currentFile.changeFileTypeIdentifier(app, fileTypeIdentifier);
			app.saveFile();
		}
	}

	/*
	 * ======================================================================================================================================================
	 * Dialogs
	 * ======================================================================================================================================================
	 */

	private String lastFileDialogDirectory = System.getProperty("user.home");
	public File showOpenFileDialog(String dialogTitle, String initalFileName, String initalDirectory, FileChooser.ExtensionFilter[] fileExtensions)
	{
		return showFileChooser(true, dialogTitle, initalFileName, initalDirectory, fileExtensions);
	}
	
	public File showSaveFileDialog(String dialogTitle, String initalFileName, String initalDirectory, FileChooser.ExtensionFilter[] fileExtensions)
	{
		return showFileChooser(false, dialogTitle, initalFileName, initalDirectory, fileExtensions);
	}

	private File showFileChooser(Boolean showAsOpenFileDialog, String dialogTitle, String initalFileName, String initalDirectory, FileChooser.ExtensionFilter[] fileExtensions)
	{
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(dialogTitle);
		  
		//Set extension filter
		if(fileExtensions != null)
		{
			  fileChooser.getExtensionFilters().addAll(fileExtensions);
		}
		else
		{
			  fileChooser.getExtensionFilters().addAll(app.storageManager.getFileChooserExtensionFilter());
		}
		
		if(initalFileName != null && !initalFileName.equals(""))
		{
			fileChooser.setInitialFileName(initalFileName);
		}
		else
		{
			if(initalDirectory != null && !initalDirectory.equals(""))
			{
				fileChooser.setInitialDirectory(new File(initalDirectory));
				lastFileDialogDirectory = initalDirectory;
			}
			else
			{
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

	private MenuItem createMenuItem(String text, String iconname, EventHandler<ActionEvent> event, boolean add2TreeDependentItems)
	{
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

	/*
	 * ======================================================================================================================================================
	 * Interface "core.interfaces.FXUserInterface"
	 * ======================================================================================================================================================
	 */
	
	private List<de.akubix.keyminder.lib.sidebar.SidebarNodeChangeEvent> sidebarPanelNodeChangeEvenHandler = new ArrayList<de.akubix.keyminder.lib.sidebar.SidebarNodeChangeEvent>();
	private boolean sidebarAvailable = false;
	
	@Override
	public Tab addSidebarPanel(String tabtitle, Node panel, de.akubix.keyminder.lib.sidebar.SidebarNodeChangeEvent onSelectedNodeChanged, EventHandler<ActionEvent> onKeyClipButtonClicked) {
	
		if(sidebarTabPanel.getTabs().size() == 0){createSidebar();}
		sidebarAvailable = true;

		Tab myTabPage = new Tab(tabtitle);
		myTabPage.setContent(panel);
		myTabPage.setClosable(false);
		
		if(keyClipSidebarButton != null){
		sidebarTabPanel.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
					@Override
					public void changed(ObservableValue<? extends Tab> ov, Tab oldTab, Tab newTab) {
						if(myTabPage.equals(newTab)){
							if(onKeyClipButtonClicked == null)
							{
								keyClipSidebarButton.setVisible(false);
							}
							else
							{
								keyClipSidebarButton.setOnAction(onKeyClipButtonClicked);
								keyClipSidebarButton.setVisible(true);
							}
						}
					}});
		}
		
		sidebarPanelNodeChangeEvenHandler.add(onSelectedNodeChanged);
		sidebarTabPanel.getTabs().add(myTabPage);
		return myTabPage;
	}
	
	private void createSidebar()
	{
		Label l = new Label();
		BorderPane bp = new BorderPane(l);
		
		l.setPadding(new Insets(2,3,3,3));

		GridPane grid = new GridPane();
		grid.setId("SidebarToolPanel");
		grid.setAlignment(Pos.CENTER);
		grid.setVgap(4);
		grid.add(createSmallButton(localeBundle.getString("mainwindow.sidebar.collapsed.show"), "icon_add", 24, (event) -> {if(sidebarAvailable){showSidebar(true);}}), 0, 0);
		grid.add(createSmallButton(localeBundle.getString("mainwindow.sidebar.collapsed.edit_node"), "icon_edit", 24, (event) -> showEditCurrentNodeDialog()), 0, 1);
		grid.add(createSmallButton(localeBundle.getString("mainwindow.sidebar.collapsed.move_node_up"), "icon_up", 24, (event) -> dataTree.moveNodeVertical(dataTree.getSelectedNode(), -1)), 0, 2);
		grid.add(createSmallButton(localeBundle.getString("mainwindow.sidebar.collapsed.move_node_down"), "icon_down", 24, (event) -> dataTree.moveNodeVertical(dataTree.getSelectedNode(), 1)), 0, 3);
		grid.add(createSmallButton(localeBundle.getString("mainwindow.sidebar.collapsed.copy_text"), "icon_copy", 24, (event) -> setClipboardText(getSelectedTreeNode().getText())), 0, 4);

		sidebarToolPanel = grid;

		app.addEventHandler(TreeNodeEvent.OnSelectedItemChanged, (node) -> {
				l.setText(node.getText());
				boolean sidebarIsEmpty[] = new boolean[]{false};
				sidebarPanelNodeChangeEvenHandler.forEach((event) -> sidebarIsEmpty[0] = event.selectedNodeChanged(node) || sidebarIsEmpty[0]);
				showSidebar(sidebarIsEmpty[0]);
			});
		
		bp.setMaxHeight(28);
		bp.getStyleClass().add("lightHeader");

		HBox hbox = new HBox(0);
		hbox.getChildren().add(createSidebarNodeLinkButton());

		app.addEventHandler(DefaultEvent.OnFileClosed, () -> l.setText(""));

		if(!app.getSettingsValueAsBoolean("nodes.disable_favorites", false)){
			hbox.getChildren().add(createFavNodeButton());
		}

		if(app.commandAvailable("keyclip")){
			keyClipSidebarButton = createSmallButton("KeyClip", "icon_arrow-rotate-box", 24, null);
			keyClipSidebarButton.disableProperty().bind(treeDependentElementsDisableProperty);
			hbox.getChildren().add(keyClipSidebarButton);
		}

		if(hbox.getChildren().size() > 0){bp.setRight(hbox);}

		sidebarPanel.setTop(bp);
		showSidebar(true);
	}

	/**
	 * Creates a small button with an image and a tool tip, but without a text.
	 * @param tooltip the tool tip
	 * @param icon the icon
	 * @param size the size (width and height)
	 * @param onClick the event handler
	 * @return a button with your preferences
	 */
	public static Button createSmallButton(String tooltip, String icon, double size, EventHandler<ActionEvent> onClick)
	{
		Button b = new Button("", ImageSelector.getFxImageView((icon)));
		b.setMinWidth(size);
		b.setMaxWidth(size);
		b.setTooltip(new Tooltip(tooltip));
		b.getStyleClass().add("noBorder");
		if(onClick != null){b.setOnAction(onClick);}
		return b;
	}

	private Button createSidebarNodeLinkButton()
	{
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
		for(int i = 0; i < count; i++)
		{
			menuItems[i] = new MenuItem();
			menuItems[i].setUserData(-1);
			menuItems[i].setOnAction((event) -> {
				MenuItem me = (MenuItem) event.getSource();
				int id = (int) me.getUserData();
				if(id > 0){
					TreeNode linkedNode = dataTree.getNodeById(id);
					if(linkedNode != null){dataTree.setSelectedNode(linkedNode);}
				}
				else{
					showLinkNodePanel(getSelectedTreeNode());
				}
			});
		}
		return menuItems;
	}

	private void updateSidebarNodeLinkContextMenu(MenuItem[] menuItems, TreeNode node)
	{
		int i[] = new int[]{0};

		app.forEachLinkedNode(node, (linkedNode) -> {
			if(i[0] < menuItems.length)
			{
				menuItems[i[0]].setText(linkedNode.getText());
				menuItems[i[0]].setUserData(linkedNode.getId());
				menuItems[i[0]].setVisible(true);
				i[0]++;
			}
		});

		boolean isFirst = true;
		while(i[0] < menuItems.length)
		{
			menuItems[i[0]].setVisible(isFirst);
			if(isFirst){
				menuItems[i[0]].setText(localeBundle.getString("mainwindow.sidebar.button_add_node_link"));
				menuItems[i[0]].setUserData(-1);
				isFirst= false;
			}
			i[0]++;
		}
	}

	private Button createFavNodeButton()
	{
		ImageView imgview_notStarred = new ImageView(ImageSelector.getIcon("icon_star"));
		ImageView imgview_starred = new ImageView(ImageSelector.getIcon("icon_star_filled"));
		Button b = new Button("", imgview_notStarred);
		b.setMinWidth(24);
		b.setMaxWidth(24);
		b.setMinHeight(25);
		b.setMaxHeight(25);
		b.setTooltip(new Tooltip(localeBundle.getString("mainwindow.sidebar.star_node")));
		b.getStyleClass().add("noBorder");

		TreeNodeEventHandler handler = (node) -> b.setGraphic(node.hasAttribute("favorite") ? imgview_starred : imgview_notStarred);
		imgview_notStarred.setFitHeight(16);
		imgview_notStarred.setFitWidth(16);
		imgview_starred.setFitHeight(16);
		imgview_starred.setFitWidth(16);

		app.addEventHandler(TreeNodeEvent.OnSelectedItemChanged, handler);

		b.setOnAction((event) -> {
			TreeNode node = getSelectedTreeNode();
			if(node.hasAttribute(ApplicationInstance.NODE_ATTRIBUTE_FAVORITE_NODE))
			{
				app.removeFavoriteNode(node);
			}
			else
			{
				if(!app.setFavoriteNode(node)){
					//Cannot set node as favorite node - most likely the maximal number of favorite nodes (10) is reached
					app.alert(localeBundle.getString("mainwindow.messages.max_number_of_favorite_nodes_reached"));
				}
			}
			handler.eventFired(node);
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
	public javafx.beans.property.ReadOnlyDoubleProperty getSidbarWidthProperty()
	{
		return sidebarPanel.widthProperty();
	}
	
	@Override
	public void addMenuEntry(MenuItem item, de.akubix.keyminder.core.etc.MenuEntryPosition pos, boolean add2TreeDependentItems) {
		switch(pos)
		{
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
	public void addMenu(Menu menu, boolean add2TreeDependentItems)
	{
		menuBar.getMenus().add(menu);
		if(add2TreeDependentItems){menu.disableProperty().bind(treeDependentElementsDisableProperty);}
	}

	@Override
	public String getClipboardText() {
		if(clipboard.hasString() || clipboard.hasHtml() || clipboard.hasUrl())
		{
			return clipboard.getString();
		}
		return "";
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
	public void updateStatus(String text)
	{
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
		if(isFXThread()){
			alert(AlertType.INFORMATION, "", null, text);
		}
		else{
			runAsFXThread(() -> {alert(AlertType.INFORMATION, "", null, text);});
		}
	}
	
	@Override
	public void alert(AlertType type, String title, String headline, String contentText){
		final String alertTile = title.equals("") ? ApplicationInstance.APP_NAME : title;

		if(isFXThread()){
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
		s.getIcons().add(new Image(ApplicationInstance.APP_ICON));
		s.initOwner(me);
		StyleSelector.assignDefaultStylesheet(s.getScene());
		msg.showAndWait();
	}
	
	@Override
	public String showInputDialog(String windowTitle, String labelText, String defaultValueOrPasswordHint, boolean useAsPasswordDialog) throws UserCanceledOperationException
	{
		InputDialog id = new InputDialog(me, this, windowTitle, labelText, defaultValueOrPasswordHint, useAsPasswordDialog);
		return id.getInput();
	}
	
	/*
	 * Shows a dialog to add a new tree node to the currently selected node
	 */
	private void showAddTreeNodeDialog(boolean add2Root)
	{
		InputDialog id = new InputDialog(me, this, localeBundle.getString("mainwindow.dialogs.add_node.title"), localeBundle.getString("mainwindow.dialogs.add_node.text"), "", false);
		try {
			String input = id.getInput();

			if(input != null && !input.equals(""))
			{
				TreeNode newNode = dataTree.createNode(input);
				if(add2Root)
				{
					dataTree.addNode(newNode, dataTree.getRootNode());
					dataTree.setSelectedNode(newNode);
				}
				else
				{
					dataTree.addNode(newNode, getSelectedTreeNode());
					fxtree.getSelectionModel().getSelectedItem().setExpanded(true);
				}
			}
		} catch (UserCanceledOperationException e) {}

		me.requestFocus();
	}
	
	private void showEditCurrentNodeDialog()
	{
		TreeNode selectedNode = getSelectedTreeNode();
		InputDialog id = new InputDialog(this, localeBundle.getString("mainwindow.dialogs.edit_node.text"), localeBundle.getString("mainwindow.dialogs.edit_node.text"), selectedNode.getText(), false);
		try {
			String value = id.getInput();
			if(!value.equals("")){
				selectedNode.setText(value);
			}
		} catch (UserCanceledOperationException e) {}
		me.requestFocus();
	}
		
	private void validateKeyPress(KeyEvent event)
	{
		String keyCodeStr = generatreKeyCodeString(event.getCode(), event.isControlDown(), event.isShiftDown(), event.isAltDown());
		if(hotkeys.containsKey(keyCodeStr)){hotkeys.get(keyCodeStr).fireEvent();}
	}
	
	private void InsertNodeFromClipboard()
	{
		String clipboardText = getClipboardText();
		if(!clipboardText.trim().equals(""))
		{
			TreeNode newNode = dataTree.createNode(clipboardText);
			dataTree.addNode(newNode, getSelectedTreeNode());
			updateStatus(localeBundle.getString("mainwindow.messages.node_insert_from_clipboard"));
		}
	}
		
	private boolean searchBoardIsVisible = false;
	private void showSearchBar(boolean show)
	{
		if(show)
		{
			centerPane.setBottom(searchBoard);
			searchInput.requestFocus();
			searchBoardIsVisible = true;
		}
		else
		{
			centerPane.setBottom(null);
			searchBoardIsVisible = false;
		}
	}
	
	@Override
	public boolean showSaveChangesDialog() throws UserCanceledOperationException
	{
		de.akubix.keyminder.ui.fx.dialogs.SaveChangesDialog.Result r = de.akubix.keyminder.ui.fx.dialogs.SaveChangesDialog.show(this);
		if(r == Result.Cancel){throw new UserCanceledOperationException("Cancel button was pressed.");}
		return (r == Result.SaveChanges);
	}
	
	@Override
	/**
	 * Display a simple yes/no dialog to the user
	 * @return TRUE if the user has clicked "YES", false if she/he chooses "No"
	 **/
	public boolean showYesNoDialog(String windowTitle, String headline, String contentText)
	{
		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle(windowTitle);
		alert.setHeaderText(headline);
		alert.setContentText(contentText);
		Stage s = (Stage) alert.getDialogPane().getScene().getWindow();
		s.getIcons().add(new Image(ApplicationInstance.APP_ICON));
		StyleSelector.assignDefaultStylesheet(alert.getDialogPane().getScene());
		s.initOwner(me);
		
		ButtonType buttonYes = new ButtonType(localeBundle.getString("yes"), ButtonData.YES);
		ButtonType buttonNo = new ButtonType(localeBundle.getString("no"), ButtonData.NO);

		alert.getButtonTypes().setAll(buttonYes, buttonNo);

		java.util.Optional<ButtonType> result = alert.showAndWait();
		return result.get()  == buttonYes ? true : false;
	}

	@Override
	public void addNotificationItem(Node item, boolean assignToThisFile)
	{
		if(Platform.isFxApplicationThread()){
			notificationArea.getChildren().add(item);
			if(assignToThisFile){assignedNotificationsItems.add(item);}
		}
		else{
			Platform.runLater(() -> {notificationArea.getChildren().add(item); if(assignToThisFile){assignedNotificationsItems.add(item);}});
		}
	}

	@Override
	public void removeNotificationItem(Node item)
	{
		notificationArea.getChildren().remove(item);
		if(assignedNotificationsItems.contains(item)){assignedNotificationsItems.remove(assignedNotificationsItems);}
	}

	@Override
	public void addTreePanel(Node item, boolean assignToThisFile)
	{
		if(Platform.isFxApplicationThread()){
			panelStack.getChildren().add(item);
			if(assignToThisFile){assignedNotificationsItems.add(item);}
		}
		else{
			Platform.runLater(() -> {panelStack.getChildren().add(item); if(assignToThisFile){assignedNotificationsItems.add(item);}});
		}
	}

	@Override
	public void removeTreePanel(Node item)
	{
		panelStack.getChildren().remove(item);
		if(assignedNotificationsItems.contains(item)){assignedNotificationsItems.remove(assignedNotificationsItems);}
	}

	@Override
	public void buildFavoriteNodeList(int[] favoriteNodes)
	{
		if(menu_FavoriteNodes == null){
			menu_FavoriteNodes = new Menu(localeBundle.getString("mainwindow.menu.favorite_nodes"));
		}
		else{
			clearFavoriteNodeList(false);
		}

		byte count = 0;
		for(byte i = 0; i< favoriteNodes.length; i++)
		{
			if(favoriteNodes[i] > 0)
			{
				TreeNode n = dataTree.getNodeById(favoriteNodes[i]);
				if(n != null)
				{
					MenuItem menuItem = new MenuItem(i + ": " +n.getText());
					menuItem.setUserData(favoriteNodes[i]);
					menuItem.setOnAction((event) -> {
							TreeNode node = dataTree.getNodeById((int) menuItem.getUserData());
							if(node != null){dataTree.setSelectedNode(node);}
					});

					menu_FavoriteNodes.getItems().add(menuItem);
					count++;
				}
			}
		}

		if(count > 0){
			menuBar.getMenus().add(menu_FavoriteNodes);
		}
		else{
			menu_FavoriteNodes = null;
		}
	}

	private void clearFavoriteNodeList(boolean setNull)
	{
		if(menu_FavoriteNodes != null){
			menu_FavoriteNodes.getItems().clear();
			menuBar.getMenus().remove(menu_FavoriteNodes);
			if(setNull){menu_FavoriteNodes = null;}
		}
	}

	private void undo(){
		if(!dataTree.undo()){
			updateStatus(localeBundle.getString("mainwindow.messages.undo_limit_reached"));
		}
	}
}