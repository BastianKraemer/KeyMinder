/*	KeyMinder
	Copyright (C) 2015-2016 Bastian Kraemer

	ApplicationInstance.java

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
package de.akubix.keyminder.core;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import javax.management.modelmbean.XMLParseException;

import de.akubix.keyminder.core.db.StandardTree;
import de.akubix.keyminder.core.db.Tree;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.encryption.EncryptionManager;
import de.akubix.keyminder.core.exceptions.IllegalCallException;
import de.akubix.keyminder.core.exceptions.StorageException;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.core.interfaces.FxAdministrationInterface;
import de.akubix.keyminder.core.interfaces.FxUserInterface;
import de.akubix.keyminder.core.interfaces.events.DefaultEventHandler;
import de.akubix.keyminder.core.interfaces.events.EventHost;
import de.akubix.keyminder.core.interfaces.events.EventTypes.BooleanEvent;
import de.akubix.keyminder.core.interfaces.events.EventTypes.DefaultEvent;
import de.akubix.keyminder.core.interfaces.events.EventTypes.SettingsEvent;
import de.akubix.keyminder.core.interfaces.events.EventTypes.TreeNodeEvent;
import de.akubix.keyminder.core.io.StorageManager;
import de.akubix.keyminder.core.modules.ModuleLoader;
import de.akubix.keyminder.lib.Tools;
import de.akubix.keyminder.lib.XMLCore;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.Shell;
import de.akubix.keyminder.shell.io.ShellOutputWriter;
import javafx.scene.control.TabPane;

/**
 * This class is the core of this application, it provides all functions and methods for the whole event handling, manages the loading of all modules
 * and creates the database. Furthermore it is the only interface to load and store data from respectively into files.
 */
public class ApplicationInstance implements EventHost, ShellOutputWriter {

	/* Static configurations variables */

	public static String APP_VERSION = "0.2-SNAPSHOT";

	public static final String APP_ICON_16 = "/de/akubix/keyminder/images/app/AppIcon16.png";
	public static final String APP_ICON_32 = "/de/akubix/keyminder/images/app/AppIcon32.png";
	public static final String APP_ICON_256 = "/de/akubix/keyminder/images/app/AppIcon256.png";
	public static final String APP_NAME = "KeyMinder";

	public static final String NODE_ATTRIBUTE_CREATION_DATE = "created";
	public static final String NODE_ATTRIBUTE_MODIFICATION_DATE = "modified";
	public static final String NODE_ATTRIBUTE_FAVORITE_NODE = "favorite";
	public static final String NODE_ATTRIBUTE_LINKED_NODES = "linked_nodes";

	public static final String SETTINGS_KEY_ENABLED_MODULES = "enabled_modules";
	public static final String SETTINGS_KEY_DEFAULT_FILE = "startup.defaultfile";
	public static final String SETTINGS_KEY_USE_OTHER_WEB_BROWSER = "etc.useotherbrowser";
	public static final String SETTINGS_KEY_BROWSER_PATH = "etc.browserpath";

	/* Other variables */

	private File settingsFile;
	private FxAdministrationInterface fxInterface = null;

	public Map<String, String> settings = new HashMap<String, String>();
	public FileConfiguration currentFile = null;

	private Map<String, List<Object>> eventCollection = new HashMap<>();

	private StandardTree tree;
	private ShellOutputWriter outputRedirect = null;
	private final Shell shell;
	private final ModuleLoader moduleLoader;
	public final StorageManager storageManager;
	public final Locale applicationLocale;

	public ApplicationInstance(){
		Package p = getClass().getPackage();
		if(p.getImplementationVersion() != null){
			APP_VERSION = p.getImplementationVersion();
		}

		tree = new StandardTree(this);
		storageManager = new StorageManager();

		final String defaultSettingsFile = "keyminder_settings.xml";

		if(KeyMinder.environment.containsKey("cmd.settingsfile")){
			settingsFile = new File(KeyMinder.environment.get("cmd.settingsfile"));
		}
		else{
			settingsFile = new File(defaultSettingsFile);
			if(!settingsFile.exists()){
				// There is no configuration file next to the jar -> maybe there is a global configuration file, placed in the users home directory?
				File globalSettingsFile = new File(System.getProperty("user.home") + System.getProperty("file.separator") + defaultSettingsFile);
				if(globalSettingsFile.exists()){
					settingsFile = globalSettingsFile;
				}
			}
		}

		presetDefaultSettings();
		loadSettingsFromXMLFile();

		if(settings.containsKey("language")){
			String language = settings.get("language").toLowerCase();
			if(language.equals("de")){
				applicationLocale = new Locale("de", "DE");
			}
			else if(language.equals("en")){
				applicationLocale = new Locale("en", "EN");
			}
			else{
				if(KeyMinder.verbose_mode){printf("Unsupported locale: '%s'. Using 'en/EN' instead...\n", language);}
				applicationLocale = new Locale("en", "EN");
			}
		}
		else{
			Locale systemDefaultLocale = Locale.getDefault();
			if(systemDefaultLocale.getLanguage().matches("en|de")){
				applicationLocale = systemDefaultLocale;
			}
			else{
				applicationLocale = new Locale("en", "EN");
			}
		}

		this.shell = new Shell(this);
		shell.loadCommandsFromIniFile("/de/akubix/keyminder/shell/commands.ini");

		this.moduleLoader = new ModuleLoader(this);
	}

	public Tree getTree(){
		return tree;
	}

	public Shell getShell(){
		return this.shell;
	}

	public ModuleLoader getModuleLoader(){
		return this.moduleLoader;
	}

	/*
	 * ========================================================================================================================================================
	 * Startup method
	 * ========================================================================================================================================================
	 */

	// TODO: Startup

	/**
	 * The Startup-Method must be called by the UserInterface when it is ready.
	 */
	public void startup(boolean enableModuleLoading){

		if(enableModuleLoading){
			moduleLoader.loadModules();
		}

		updateMainWindowTitle();

		if(isFxUserInterfaceAvailable())
		{
			DefaultEventHandler eventHandler = new DefaultEventHandler() {
				@Override
				public void eventFired() {
					updateMainWindowTitle();
				}
			};

			addEventHandler(DefaultEvent.OnFileOpened, eventHandler);
			addEventHandler(DefaultEvent.OnFileClosed, eventHandler);
			addEventHandler(DefaultEvent.OnSettingsChanged, eventHandler);
		}
	}

	/**
	 * The loadDefaultFile-Method should be called by the UserInterface when the startup has finished.
	 * This is necessary because maybe this must be called by the UI-Main Thread.
	 */
	public void loadDefaultFile(){
		if(KeyMinder.environment.containsKey("cmd.file")){
			File f = new File(KeyMinder.environment.get("cmd.file"));

			if(f.exists()){
				if(!KeyMinder.environment.containsKey("cmd.password")){
					openFile(f);
				}
				else{
					openFile(f, KeyMinder.environment.get("cmd.password"));
					KeyMinder.environment.remove("cmd.password");
				}
			}
			else{
				println("Cannot open file \"" + f.getAbsolutePath() + "\". File does not exist.");
			}
		}
		else{
			if(settings.containsKey(SETTINGS_KEY_DEFAULT_FILE)){
				File f = new File(settings.get(SETTINGS_KEY_DEFAULT_FILE));
				if(f.exists()){
					if(!KeyMinder.environment.containsKey("cmd.password")){
						openFile(f);
					}
					else{
						openFile(f, KeyMinder.environment.get("cmd.password"));
						KeyMinder.environment.remove("cmd.password");
					}
				}
				else{
					println("Cannot open file \"" + f.getAbsolutePath() + "\". File does not exist.");
				}
			}
		}
	}

	/*
	 * ========================================================================================================================================================
	 * Settings
	 * ========================================================================================================================================================
	 */

	private void presetDefaultSettings(){
		settings.put(SETTINGS_KEY_ENABLED_MODULES, "KeyClip;Sidebar;");
	}

	private void loadSettingsFromXMLFile(){
		try {
			if(settingsFile.exists()){
				XMLCore.xml2Map(settingsFile, settings, true);
				loadDefaultEnvironmentXMLFile();
			}
		} catch (XMLParseException e) {
			println("Cannot load settings from xml file. " + e.getMessage());
		}
	}

	private void loadDefaultEnvironmentXMLFile()
	{
		if(settings.containsKey("startup.default_environment")){
			try {
				File environmrntXMLFile = new File(settings.get("startup.default_environment"));
				if(environmrntXMLFile.exists()){
					XMLCore.xml2Map(environmrntXMLFile, KeyMinder.environment, false);
					if(KeyMinder.environment.containsKey("verbose_mode") && Tools.isYes(KeyMinder.environment.get("verbose_mode"))){
						KeyMinder.verbose_mode = true;
					}
				}
				else{
					println(String.format("Warning: Cannot find environment file \"%s\"", settings.get("startup.default_environment")));
				}
			} catch (XMLParseException e) {
				println("Cannot load default environment from xml file: " + e.getMessage());
			}
		}
	}

	public void reloadSettings(){
		settings.clear();
		presetDefaultSettings();
		loadSettingsFromXMLFile();
		fireEvent(DefaultEvent.OnSettingsChanged);
	}

	public boolean settingsContainsKey(String name){
		return settings.containsKey(name);
	}

	public String getSettingsValue(String name){
		return settings.getOrDefault(name, "");
	}

	public String setSettingsValue(String name, String value) throws IllegalArgumentException, IllegalStateException {
		if(!name.matches("[a-zA-Z0-9_:\\.]+")){throw new IllegalArgumentException("The name for the settings value contains an illegal character. Allowed characters: 'A-Z', 'a-z', '0-9', '.', '_' and ':'.");}
		return settings.put(name, value);
	}

	public Boolean getSettingsValueAsBoolean(String name, Boolean onNotExist){
		if(!settings.containsKey(name)){return onNotExist;}
		String value = settings.get(name).toLowerCase();
		if(value.equals("yes") || value.equals("true") || value.equals("1")){return true;}else{return false;}
	}

	public boolean removeSettingsValue(String name){
		return (settings.remove(name) != null);
	}

	public boolean fileSettingsContainsKey(String name){
		if(currentFile != null){
			return currentFile.fileSettings.containsKey(name);
		}
		return false;
	}

	public String getFileSettingsValue(String name){
		if(currentFile != null){
			return currentFile.fileSettings.getOrDefault(name, "");
		}
		return "";
	}

	public void setFileSettingsValue(String name, String value) throws IllegalArgumentException, IllegalStateException {
		if(currentFile == null){
			throw new IllegalStateException("Unable to change the file settings if not file is opened!");
		}

		if(!name.matches("[a-zA-Z0-9_:\\.]+")){
			throw new IllegalArgumentException("The name for the file settings value contains an illegal character. Allowed characters: 'A-Z', 'a-z', '0-9', '.', '_' and ':'.");
		}
		currentFile.fileSettings.put(name, value);
	}

	public boolean removeFileSettingsValue(String name){
		if(currentFile == null){
			throw new IllegalStateException("Unable to change the file settings if not file is opened!");
		}

		return (currentFile.fileSettings.remove(name) != null);
	}

	public Set<String> getSettingsKeySet(){
		return settings.keySet();
	}

	public Set<String> getFileSettingsKeySet(){
		if(currentFile == null){
			throw new IllegalStateException("Unable to change the file settings if not file is opened!");
		}

		return currentFile.fileSettings.keySet();
	}

	/**
	 * Tell the ApplicationInstance that some settings may have changed an fire the assigned events
	 */
	public void saveSettings(){
		XMLCore.saveMapAsXMLFile(settingsFile, settings, "keyminder_settings");
		fireEvent(DefaultEvent.OnSettingsChanged);
	}

	/**
	 * Tell the ApplicationInstance that some file settings may have changed an fire the assigned events
	 */
	public void fileSettingsHasBeenUpdated(){
		saveFile();
		fireEvent(DefaultEvent.OnFileSettingsChanged);
	}

	/*
	 * ========================================================================================================================================================
	 * File handling
	 * ========================================================================================================================================================
	 */

	public boolean openFile(File file){
		return openFile(file, "");
	}

	public boolean openFile(File file, String filepassword){
		return openFile(file, filepassword, storageManager.getIdentifierByExtension(file.getName(), StorageManager.defaultFileType));
	}

	public synchronized boolean openFile(File file, String filepassword, String fileTypeIdentifier){
		if(fileTypeIdentifier == null || fileTypeIdentifier.equals("")){
			println("Warning: Unknown file type - assuming 'KeyMind XML file (" + StorageManager.defaultFileType + ")'.");
			fileTypeIdentifier = StorageManager.defaultFileType;
		}

		if(!storageManager.hasStorageHandler(fileTypeIdentifier)){
			println(String.format("Unknown file type identifier \"%s\". Canceling...", fileTypeIdentifier));
			return false;
		}

		if(currentFile != null){
			if(!closeFile()){return false;}
		}

		try {
			if(KeyMinder.verbose_mode){log(String.format("Opening file \"%s\" (as \"%s\")...", file.getAbsoluteFile(), fileTypeIdentifier));}

			tree.reset();
			currentFile = storageManager.getStorageHandler(fileTypeIdentifier).open(file, filepassword, tree, this);
			tree.verify();

			prepareAppForFileOpened();

			updateStatus(String.format((
					isFxUserInterfaceAvailable() ?
					fxInterface.getLocaleBundleString("application.file_opened") :
					"File '%s' successfully opened."
				), file.getName()));

			return true;
		} catch (StorageException e) {
			if(e.getReason() != de.akubix.keyminder.core.exceptions.StorageExceptionType.UserCancelation){
				alert(String.format((
							isFxUserInterfaceAvailable() ?
							fxInterface.getLocaleBundleString("application.unable_to_open_file") :
							"Unable to open file '%s':\n%s"
						), file.getName(), e.getMessage()));
			}

			return false;
		}
	}

	public synchronized void prepareAppForFileOpened(){
		tree.undoManager.setEnable(true);
		tree.setTreeChangedStatus(false);

		tree.resetNodePointer(); // the node pointer should have a default value, maybe a module listens to the "onFileOpened" event.

		tree.enableNodeTimestamps(true);
		tree.enableEventFireing(true);

		if(isFxUserInterfaceAvailable()){fxInterface.onFileOpenedHandler();}
		generateFavoriteNodeListFromTree();

		fireEvent(DefaultEvent.OnFileOpened);

		if(tree.getSelectedNode().getId() != 0){fireEvent(TreeNodeEvent.OnSelectedItemChanged, tree.getSelectedNode());}
	}

	public synchronized boolean saveFile(){
		try {
			if(currentFile != null) {
				storageManager.getStorageHandler(currentFile.getFileTypeIdentifier()).save(currentFile, tree, this);
				tree.setTreeChangedStatus(false);
				updateStatus(String.format((
								isFxUserInterfaceAvailable() ?
								fxInterface.getLocaleBundleString("application.file_saved") :
								"File '%s' has been saved."
							), currentFile.getFilepath().getName()));
			}
			return true;
		} catch (IllegalArgumentException e) {
			if(requestYesNoDialog(APP_NAME, String.format((
						isFxUserInterfaceAvailable() ?
							fxInterface.getLocaleBundleString("application.invalid_file_type") :
							"Invalid file type: \"%s\"\nDo you want to save your file as \"KeyMind XML File (xml/keymindfile)\"?"
						), currentFile.getFileTypeIdentifier()))){

				currentFile.changeFileTypeIdentifier(this, StorageManager.defaultFileType);
				return saveFile();
			}
			else{
				return false;
			}

		} catch (StorageException e) {
			alert(String.format((
					isFxUserInterfaceAvailable() ?
					fxInterface.getLocaleBundleString("application.unable_to_save_file") :
					"Unable to save file: %s"
				), e.getMessage()));
			return false;
		}
	}

	/**
	 * Closes the currently opened file
	 * @return TRUE if the file has been closed, FALSE if the user canceled the action
	 */
	public synchronized boolean closeFile(){
		if(currentFile != null){
			if(fireEvent(BooleanEvent.DONTAllowFileClosing, true, true)){return false;}
			if(tree.treeHasBeenUpdated()){
				if(isFxUserInterfaceAvailable()){
					try {
						if(fxInterface.showSaveChangesDialog()){
							// Save the changes
							if(!saveFile()){return false;}
						}
					} catch (UserCanceledOperationException e) {
						return false;
					}
				}
				else{
					if(!ConsoleMode.askYesNo("Do you want do discard your changes?")){return false;}
				}
			}

			tree.enableEventFireing(false);
			tree.reset();

			if(currentFile.getEncryptionManager() != null){currentFile.getEncryptionManager().destroy();}
			currentFile = null;
			clearFavoriteNodeList();
			tree.setTreeChangedStatus(false);
			tree.enableEventFireing(true);
			fireEvent(DefaultEvent.OnFileClosed);

			updateStatus(isFxUserInterfaceAvailable() ?	fxInterface.getLocaleBundleString("application.file_closed") :	"File closed.");
			return true;
		}
		else{
			return true;
		}
	}

	public boolean createNewFile(File file, boolean encryptFileWithDefaultCipher){
		return createNewFile(file, StorageManager.defaultFileType, encryptFileWithDefaultCipher);
	}

	public synchronized boolean createNewFile(File file, String fileTypeIdentifier, boolean encryptFileWithDefaultCipher){
		if(currentFile != null){
			if(!closeFile()){return false;}
		}

		currentFile = new FileConfiguration(file, "(not set)", encryptFileWithDefaultCipher, fileTypeIdentifier, encryptFileWithDefaultCipher ? new EncryptionManager(true) : null, new HashMap<>(), new HashMap<>());

		if(encryptFileWithDefaultCipher){
			try {
				if(!currentFile.getEncryptionManager().requestPasswordInputWithConfirm(this,
					isFxUserInterfaceAvailable() ? fxInterface.getLocaleBundleString("mainwindow.createfile.setpassword.headline") : "Set Password...",
					isFxUserInterfaceAvailable() ? fxInterface.getLocaleBundleString("mainwindow.createfile.setpassword.text") : "Please enter a password for your new file:",
					isFxUserInterfaceAvailable() ? fxInterface.getLocaleBundleString("mainwindow.createfile.setpassword.confirmtext") : "Please enter the password again:"))
				{
					alert(isFxUserInterfaceAvailable() ? fxInterface.getLocaleBundleString("mainwindow.createfile.setpassword.passwords_not_equal") : "The entered passwords doesn't match.");
					return createNewFile(file, fileTypeIdentifier, encryptFileWithDefaultCipher);
				}

			} catch (UserCanceledOperationException e) {
				currentFile.getEncryptionManager().destroy();
				return false;
			}
		}

		TreeNode firstNode = tree.createNode(APP_NAME);
		tree.addNode(firstNode, tree.getRootNode());

		prepareAppForFileOpened();

		updateStatus(String.format((
				isFxUserInterfaceAvailable() ?
				fxInterface.getLocaleBundleString("application.file_created") :
				"File '%s' created."
			), file.getName()));
		return true;
	}


	private void updateMainWindowTitle() {
		if (isFxUserInterfaceAvailable()){
			fxInterface.setTitle(APP_NAME +
								 ((getSettingsValueAsBoolean("windowtitle.showfilename", true) && currentFile != null) ? " - " + currentFile.getFilepath().getName() : "") +
								 (getSettingsValueAsBoolean("windowtitle.showversion", false) ? " (Version " + APP_VERSION + ")" : ""));
		}
	}

	/*
	 * ========================================================================================================================================================
	 * Node links
	 * ========================================================================================================================================================
	 */

	public void forEachLinkedNode(TreeNode node, Consumer<TreeNode> forEach){
		boolean listNeedsToBeCorrected = false;
		for(String idString: node.getAttribute(NODE_ATTRIBUTE_LINKED_NODES).split(";")){
			try{
				int id = Integer.parseInt(idString);
				TreeNode linkedNode = tree.getNodeById(id);
				if(linkedNode != null){
					forEach.accept(linkedNode);
				}
				else{
					listNeedsToBeCorrected = true;
				}
			}
			catch(NumberFormatException numEx){
				listNeedsToBeCorrected = true;
			}
		}

		if(listNeedsToBeCorrected){removeLinkedNode(node, -1);}
	}

	public synchronized void addLinkedNode(TreeNode node, int idOfLinkedNode){
		if(node.hasAttribute(NODE_ATTRIBUTE_LINKED_NODES)){
			String currentValue = node.getAttribute(NODE_ATTRIBUTE_LINKED_NODES);
			node.setAttribute(NODE_ATTRIBUTE_LINKED_NODES, currentValue + (currentValue.endsWith(";") ? "" : ";") + idOfLinkedNode);
		}
		else{
			node.setAttribute(NODE_ATTRIBUTE_LINKED_NODES, Integer.toString(idOfLinkedNode));
		}
	}

	public synchronized void removeLinkedNode(TreeNode node, int linkedId){
		int cnt = 0;
		StringBuilder newLinkedNodesString = new StringBuilder();
		for(String idString: node.getAttribute(NODE_ATTRIBUTE_LINKED_NODES).split(";")){
			try	{
				int id = Integer.parseInt(idString);
				if(linkedId != id){
					TreeNode linkedNode = tree.getNodeById(id);
					if(linkedNode != null){
						newLinkedNodesString.append(((cnt == 0) ? "" : ";") + id);
					}
					cnt++;
				}
			}
			catch(NumberFormatException numEx){}
		}
		if(cnt >= 0){
			node.setAttribute(NODE_ATTRIBUTE_LINKED_NODES, newLinkedNodesString.toString());
		}
		else{
			node.removeAttribute(NODE_ATTRIBUTE_LINKED_NODES);
		}
	}

	/*
	 * ========================================================================================================================================================
	 * Favorite nodes
	 * ========================================================================================================================================================
	 */
	private int[] favoriteNodes = new int[10];

	private synchronized void clearFavoriteNodeList(){
		for(byte i = 0; i < favoriteNodes.length; i++){
			favoriteNodes[i] = 0;
		}
	}

	private synchronized void generateFavoriteNodeListFromTree(){
		clearFavoriteNodeList();
		if(!getSettingsValueAsBoolean("nodes.disable_favorites", false)){
			boolean undoWasEnabled = tree.undoManager.isEnabled();
			tree.undoManager.setEnable(false);
			tree.allNodes((node) -> {if(node.hasAttribute(NODE_ATTRIBUTE_FAVORITE_NODE)){loadFavoriteNode(node, node.getAttribute(NODE_ATTRIBUTE_FAVORITE_NODE));}});
			if(isFxUserInterfaceAvailable()){fxInterface.buildFavoriteNodeList(favoriteNodes);}
			tree.undoManager.setEnable(undoWasEnabled);
		}
	}

	public boolean setFavoriteNode(TreeNode node){
		if(currentFile != null){
			if(!node.hasAttribute(NODE_ATTRIBUTE_FAVORITE_NODE)){
				for(byte i = 0; i < favoriteNodes.length; i++){
					// Find first unused place
					if(favoriteNodes[i] <= 0){
						return setFavoriteNode(node, i , true);
					}
				}
			}
		}
		return false;
	}

	private void loadFavoriteNode(TreeNode node, String favoriteNumber){
		try {
			setFavoriteNode(node, Byte.parseByte(favoriteNumber), false);
		}
		catch(NumberFormatException numFormatEx){
			if(KeyMinder.verbose_mode){println("Error while parsing favorite number of tree node " + node.getText());}
			node.removeAttribute(NODE_ATTRIBUTE_FAVORITE_NODE);
		}

	}

	public synchronized boolean setFavoriteNode(TreeNode node, byte favoriteNumber, boolean writeNodeAttribute){
		if(currentFile != null && favoriteNumber <= 9 && favoriteNumber >= 0){
			if(node.getId() > 0){
				favoriteNodes[favoriteNumber] = node.getId();
				if(writeNodeAttribute){
					boolean undoWasEnabled = tree.undoManager.isEnabled();
					tree.undoManager.setEnable(false);
					node.setAttribute(NODE_ATTRIBUTE_FAVORITE_NODE, Byte.toString(favoriteNumber));
					tree.undoManager.setEnable(undoWasEnabled);
					if(isFxUserInterfaceAvailable()){fxInterface.buildFavoriteNodeList(favoriteNodes);}
				}
				return true;
			}
		}
		return false;
	}

	public void removeFavoriteNode(TreeNode node){
		if(currentFile != null){
			for(byte i = 0; i < favoriteNodes.length; i++){
				if(node.getId() == favoriteNodes[i]){
					removeFavoriteNode(i);
				}
			}
		}
	}

	public synchronized void removeFavoriteNode(byte favoriteNumber){
		if(currentFile != null && favoriteNumber <= 9 && favoriteNumber >= 0){
			if(favoriteNodes[favoriteNumber] > 0){
				TreeNode n = tree.getNodeById(favoriteNodes[favoriteNumber]);
				boolean undoWasEnabled = tree.undoManager.isEnabled();
				tree.undoManager.setEnable(false);
				if(n != null){n.removeAttribute(NODE_ATTRIBUTE_FAVORITE_NODE);}
				tree.undoManager.setEnable(undoWasEnabled);
				favoriteNodes[favoriteNumber] = 0;
				if(isFxUserInterfaceAvailable()){fxInterface.buildFavoriteNodeList(favoriteNodes);}
			}
			else{
				favoriteNodes[favoriteNumber] = 0;
			}
		}
	}

	public synchronized TreeNode getFavoriteNode(byte favoriteNumber){
		if(currentFile != null && favoriteNumber <= 9 && favoriteNumber >= 0){
			if(favoriteNodes[favoriteNumber] > 0){
				TreeNode n = tree.getNodeById(favoriteNodes[favoriteNumber]);
				if(n != null){return n;}
			}
		}
		return null;
	}

	/*
	 * ========================================================================================================================================================
	 * Modules and Interface "EventHost"
	 * ========================================================================================================================================================
	 */

	public void registerFXUserInterface(FxAdministrationInterface fxUI){
		fxInterface = fxUI;
	}

	@Override
	/**
	 * Adds an event handler. If the Java FX user interface is loaded. All events handlers will be called with the JavaFX Thread
	 * @param eventName the name of the event
	 * @param eventHandler the handler which will be execution when the event is triggered
	 */
	public void addEventHandler(DefaultEvent eventName, de.akubix.keyminder.core.interfaces.events.DefaultEventHandler eventHandler) {
		if(!eventCollection.containsKey(eventName.toString())){eventCollection.put(eventName.toString(), new ArrayList<Object>());}
		eventCollection.get(eventName.toString()).add(eventHandler);
	}

	@Override
	/**
	 * Adds an event handler. If the Java FX user interface is loaded. All events handlers will be called with the JavaFX Thread
	 * @param eventName the name of the event
	 * @param eventHandler the handler which will be execution when the event is triggered
	 */
	public void addEventHandler(BooleanEvent eventName, de.akubix.keyminder.core.interfaces.events.BooleanEventHandler eventHandler) {
		if(!eventCollection.containsKey(eventName.toString())){eventCollection.put(eventName.toString(), new ArrayList<Object>());}
		eventCollection.get(eventName.toString()).add(eventHandler);
	}

	@Override
	/**
	 * Adds an event handler. If the Java FX user interface is loaded. All events handlers will be called with the JavaFX Thread
	 * @param eventName the name of the event
	 * @param eventHandler the handler which will be execution when the event is triggered
	 */
	public void addEventHandler(TreeNodeEvent eventName, de.akubix.keyminder.core.interfaces.events.TreeNodeEventHandler eventHandler) {
		if(!eventCollection.containsKey(eventName.toString())){eventCollection.put(eventName.toString(), new ArrayList<Object>());}
		eventCollection.get(eventName.toString()).add(eventHandler);
	}

	@Override
	/**
	 * Adds an event handler. If the Java FX user interface is loaded. All events handlers will be called with the JavaFX Thread
	 * @param eventName the name of the event
	 * @param eventHandler the handler which will be execution when the event is triggered
	 */
	public void addEventHandler(SettingsEvent eventName, de.akubix.keyminder.core.interfaces.events.SettingsEventHandler eventHandler) {
		if(!eventCollection.containsKey(eventName.toString())){eventCollection.put(eventName.toString(), new ArrayList<Object>());}
		eventCollection.get(eventName.toString()).add(eventHandler);
	}

	@Override
	/**
	 * This method will fire an event, according to this all registered event handlers for this event will be called.
	 * Note: If the JavaFX user interface has been loaded, its not allowed to call this method with another thread than the JavaFX Thread.
	 * @param event the event that should be triggered
	 * @throws core.exceptions.IllegalCallException If the JavaFX user interface is loaded, an this method is not called with JavaFX Thread.
	 */
	public synchronized void fireEvent(de.akubix.keyminder.core.interfaces.events.EventTypes.DefaultEvent event) throws IllegalCallException {
		if(isFxUserInterfaceAvailable()){
			if(!fxInterface.isFXThread()){throw new de.akubix.keyminder.core.exceptions.IllegalCallException("If there is an JavaFX UserInterface, all events must be fired with the FXThread!");}
		}

		if(eventCollection.containsKey(event.toString())){
			for(int i = 0; i < eventCollection.get(event.toString()).size(); i++){
				((de.akubix.keyminder.core.interfaces.events.DefaultEventHandler) eventCollection.get(event.toString()).get(i)).eventFired();
			}
		}
	}

	@Override
	/**
	 * This method will fire an event, according to this all registered event handlers for this event will be called.
	 * Currently this event is only fired to ask all modules if the file has unsaved changes an can be closed.
	 * Note: If the JavaFX user interface has been loaded, its not allowed to call this method with another thread than the JavaFX Thread.
	 * @param event the event that should be fired
	 * @param cancelOn this method will return the parameter 'cancelValue' if one event handler returns this boolean value
	 * @param cancelValue the value that will be returned if one event handler returns the value of 'cancelOn'
	 * @return if everything is okay this method will return '!cancelValue', if not it will be 'cancelValue'
	 * @throws core.exceptions.IllegalCallException If the JavaFX user interface is loaded, an this method is not called with JavaFX Thread.
	 */
	public synchronized boolean fireEvent(de.akubix.keyminder.core.interfaces.events.EventTypes.BooleanEvent event, boolean cancelOn, boolean cancelValue) throws IllegalCallException{
		if(isFxUserInterfaceAvailable()){
			if(!fxInterface.isFXThread()){
				throw new de.akubix.keyminder.core.exceptions.IllegalCallException("If there is an JavaFX UserInterface, all events must be fired with the FXThread!");
			}
		}

		if(eventCollection.containsKey(event.toString())) {
			for(int i = 0; i < eventCollection.get(event.toString()).size(); i++){
				if(((de.akubix.keyminder.core.interfaces.events.BooleanEventHandler) eventCollection.get(event.toString()).get(i)).eventFired() == cancelOn){
					return cancelValue;
				}
			}
		}

		return !cancelValue;
	}

	@Override
	/**
	 * This method will fire an event, according to this all registered event handlers for this event will be called.
	 * Note: If the JavaFX user interface has been loaded, its not allowed to call this method with another thread than the JavaFX Thread.
	 * @param event the event that should be triggered
	 * @param node the node that belongs to this event
	 * @throws core.exceptions.IllegalCallException If the JavaFX user interface is loaded, an this method is not called with JavaFX Thread.
	 */
	public synchronized void fireEvent(de.akubix.keyminder.core.interfaces.events.EventTypes.TreeNodeEvent event, TreeNode node) throws IllegalCallException {
		if(isFxUserInterfaceAvailable()){
			if(!fxInterface.isFXThread()){
				throw new de.akubix.keyminder.core.exceptions.IllegalCallException("If there is an JavaFX UserInterface, all events must be fired with the FXThread!");
			}
		}

		if(eventCollection.containsKey(event.toString())){
			for(int i = 0; i < eventCollection.get(event.toString()).size(); i++){
				((de.akubix.keyminder.core.interfaces.events.TreeNodeEventHandler) eventCollection.get(event.toString()).get(i)).eventFired(node);
			}
		}
	}

	@Override
	/**
	 * This method will fire an event, according to this all registered event handlers for this event will be called.
	 * Note: If the JavaFX user interface has been loaded, its not allowed to call this method with another thread than the JavaFX Thread.
	 * @param event the event that should be triggered
	 * @param tabControl the TabControl of the settings dialog. Some modules maybe want to add their own tab pages
	 * @param settings a reference to the new settings map - all settings the user want to change has to be updated HERE
	 * @throws core.exceptions.IllegalCallException If the JavaFX user interface is loaded, an this method is not called with JavaFX Thread.
	 */
	public synchronized void fireEvent(de.akubix.keyminder.core.interfaces.events.EventTypes.SettingsEvent event, TabPane tabControl, Map<String, String> settings) throws IllegalCallException	{
		if(isFxUserInterfaceAvailable()){
			if(!fxInterface.isFXThread()){
				throw new de.akubix.keyminder.core.exceptions.IllegalCallException("If there is an JavaFX UserInterface, all events must be fired with the FXThread!");
			}
		}

		if(eventCollection.containsKey(event.toString())){
			for(int i = 0; i < eventCollection.get(event.toString()).size(); i++){
				((de.akubix.keyminder.core.interfaces.events.SettingsEventHandler) eventCollection.get(event.toString()).get(i)).eventFired(tabControl, settings);
			}
		}
	}

	/*
	 * ========================================================================================================================================================
	 * Plugins for JavaFX Interface
	 * ========================================================================================================================================================
	 */

	/**
	 * This application can be started without a user interface. This method can be used to check if the graphical user interface is available.
	 * @return true of the JavaFX user interface has been loaded, false if not
	 */
	public boolean isFxUserInterfaceAvailable(){
		return (fxInterface != null);
	}

	/**
	 * This method allows you to access the JavaFX user interface
	 * @return Reference to the JavaFX user interface OR null if it is currently not available.
	 */
	public FxUserInterface getFxUserInterface(){
		return fxInterface;
	}

	// The following methods can be by used all modules to request input dialogs in console mode and graphical mode

	/**
	 * Request a (single line) text input dialog (if the JavaFX user interface is loaded it will be shown as graphical dialog)
	 * @param windowTitle the window title
	 * @param labelText the label text
	 * @param defaultValue the default value
	 * @param useAsPasswordDialog show as password dialog?
	 * @return the string entered by the user
	 * @throws UserCanceledOperationException if the user canceled the operation
	 */
	public String requestStringInput(String windowTitle, String labelText, String defaultValue, boolean useAsPasswordDialog) throws UserCanceledOperationException{
		if(isFxUserInterfaceAvailable()){
			return fxInterface.showInputDialog(windowTitle, labelText, defaultValue, useAsPasswordDialog);
		}
		else{
			println("\n" + labelText + (defaultValue.equals("") ? "" : " [" + defaultValue + "]"));
			if(!useAsPasswordDialog){
				return ConsoleMode.readLineFromSystemIn();
			}
			else{
				return ConsoleMode.readPasswordFromSystemIn();
			}
		}
	}
	/**
	 * Request a yes no input dialog (if the JavaFX user interface is loaded it will be shown as graphical dialog)
	 * @param windowTitle the window title
	 * @param labelText the label text
	 * @return {@code true} if the user clicked "yes", {@code false} if it was "no"
	 */
	public boolean requestYesNoDialog(String windowTitle, String labelText){
		if(isFxUserInterfaceAvailable()){
			return fxInterface.showYesNoDialog(windowTitle, null, labelText);
		}
		else{
			return ConsoleMode.askYesNo(labelText);
		}
	}

	/*
	 * ========================================================================================================================================================
	 * All modules can use this methods to display their information
	 * ========================================================================================================================================================
	 */

	/**
	 * Update the application status (does not take any effect if the JavaFX user interface is not available)
	 * @param text the text that will be displayed in the status bar
	 */
	public void updateStatus(String text){
		if(isFxUserInterfaceAvailable()){
			fxInterface.updateStatus(text);
		}
	}

	/**
	 * Write something to the application log (will be written to "standard out" or maybe to an open KeyMinder Terminal)
	 * @param text the text you want to log
	 */
	public void log(String text){
		if(isFxUserInterfaceAvailable()){
			fxInterface.log(text);
		}
		else{
			println(text);
		}
	}

	/**
	 * Show a message window (or print to "standard out" if the JavaFX user interface is not available)
	 * @param text the text you want to display
	 */
	public void alert(String text){
		if(isFxUserInterfaceAvailable()){
			fxInterface.alert(text);
		}
		else{
			println(" > " + text);
		}
	}

	/*
	 * ========================================================================================================================================================
	 * Interface ShellOutputWriter
	 * ========================================================================================================================================================
	 */
	@Override
	public void print(String text) {
		if(outputRedirect == null){
			System.out.print(text);
		}
		else{
			outputRedirect.print(text);
		}
	}

	@Override
	public void println(String text) {
		if(outputRedirect == null){
			System.out.println(text);
		}
		else{
			outputRedirect.println(text);
		}
	}

	@Override
	public void printf(String text, Object... args){
		this.print(String.format(text, args));
	}

	@Override
	public void setColor(AnsiColor color){
		if(outputRedirect == null){
			if(KeyMinder.enableColoredOutput){
				this.print(color.getAnsiCode());
			}
		}
		else{
			outputRedirect.setColor(color);
		}
	}

	public void tryToEstablishOutputRedirect(ShellOutputWriter redirectTo){
		if(!KeyMinder.environment.containsKey("disable_output_redirect")){
			if(redirectTo != this && outputRedirect == null){outputRedirect = redirectTo;}
		}
	}

	public void terminateOutputRedirect(ShellOutputWriter currentOwner){
		if(outputRedirect == currentOwner){outputRedirect = null;}
	}

	/*
	 * ========================================================================================================================================================
	 * Terminate
	 * ========================================================================================================================================================
	 */
	/**
	 * Terminates the whole application.
	 */
	public void terminate(){
		if(closeFile()){
			fireEvent(DefaultEvent.OnExit);
			System.exit(0);
		}
	}
}
