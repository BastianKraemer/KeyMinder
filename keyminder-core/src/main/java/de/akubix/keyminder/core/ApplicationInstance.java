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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
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
import de.akubix.keyminder.core.interfaces.FxUserInterface;
import de.akubix.keyminder.core.interfaces.UserInterface;
import de.akubix.keyminder.core.interfaces.events.BooleanEventHandler;
import de.akubix.keyminder.core.interfaces.events.DefaultEventHandler;
import de.akubix.keyminder.core.interfaces.events.EventHost;
import de.akubix.keyminder.core.interfaces.events.EventTypes;
import de.akubix.keyminder.core.interfaces.events.EventTypes.BooleanEvent;
import de.akubix.keyminder.core.interfaces.events.EventTypes.DefaultEvent;
import de.akubix.keyminder.core.interfaces.events.EventTypes.SettingsEvent;
import de.akubix.keyminder.core.interfaces.events.EventTypes.TreeNodeEvent;
import de.akubix.keyminder.core.interfaces.events.SettingsEventHandler;
import de.akubix.keyminder.core.interfaces.events.TreeNodeEventHandler;
import de.akubix.keyminder.core.io.StorageManager;
import de.akubix.keyminder.core.modules.ModuleLoader;
import de.akubix.keyminder.lib.Tools;
import de.akubix.keyminder.lib.XMLCore;
import de.akubix.keyminder.locale.LocaleLoader;
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
	public static final String APP_NAME = "KeyMinder";

	public static final String NODE_ATTRIBUTE_CREATION_DATE = "created";
	public static final String NODE_ATTRIBUTE_MODIFICATION_DATE = "modified";
	public static final String NODE_ATTRIBUTE_QUICKLINK = "quicklink";
	public static final String NODE_ATTRIBUTE_LINKED_NODES = "linked_nodes";

	public static final String SETTINGS_KEY_ENABLED_MODULES = "enabled_modules";
	public static final String SETTINGS_KEY_DEFAULT_FILE = "startup.defaultfile";
	public static final String SETTINGS_KEY_USE_OTHER_WEB_BROWSER = "etc.useotherbrowser";
	public static final String SETTINGS_KEY_BROWSER_PATH = "etc.browserpath";

	/* Other variables */

	private File settingsFile;
	private UserInterface inputSourceProvider;
	private FxUserInterface fxInterface = null;

	public Map<String, String> settings = new HashMap<String, String>();
	public FileConfiguration currentFile = null;

	private Map<String, List<Object>> eventCollection = new HashMap<>();

	private StandardTree tree;
	private final Shell shell;
	private final ModuleLoader moduleLoader;
	public final StorageManager storageManager;
	private final ResourceBundle locale;

	private final Set<ShellOutputWriter> outputWriter;

	public ApplicationInstance(UserInterface inputSourceProvider){
		this(inputSourceProvider, false);
	}

	public ApplicationInstance(UserInterface inputSource, boolean forceEnglishLocale){
		this.inputSourceProvider = inputSource;

		outputWriter = new HashSet<>();
		outputWriter.add(new ConsoleOutput());

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

		Locale applicationLocale = Locale.ENGLISH;

		if(!forceEnglishLocale){
			if(settings.containsKey("language")){
				String language = settings.get("language").toLowerCase();
				if(!language.equals("en")){
					if(language.equals("de")){
						applicationLocale = Locale.GERMAN;
					}
					else{
						if(KeyMinder.verbose_mode){printf("Unsupported locale: '%s'. Using 'en/EN' instead...\n", language);}
						applicationLocale = Locale.ENGLISH;
					}
				}
			}
			else{
				Locale systemDefaultLocale = Locale.getDefault();
				if(systemDefaultLocale.getLanguage().matches("en|de")){
					applicationLocale = systemDefaultLocale;
				}
			}
		}

		this.locale = LocaleLoader.loadLanguagePack("core", "core", applicationLocale);

		this.shell = new Shell(this);
		shell.loadCommandsFromIniFile("/de/akubix/keyminder/shell/commands.ini");

		this.moduleLoader = new ModuleLoader(this);

		TreeNodeEventHandler fx = (node) -> {
			if(node.hasAttribute(NODE_ATTRIBUTE_QUICKLINK)){buildQuicklinkList();}
		};

		addEventHandler(TreeNodeEvent.OnNodeEdited, fx);
		addEventHandler(TreeNodeEvent.OnNodeRemoved, fx);
	}

	public Tree getTree(){
		return tree;
	}

	public Shell getShell(){
		return this.shell;
	}

	public Locale getLocale(){
		return locale.getLocale();
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

	public boolean getSettingsValueAsBoolean(String name, Boolean onNotExist){
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

			updateStatus(String.format(locale.getString("application.file_opened"), file.getName()));

			return true;
		} catch (StorageException e) {
			if(e.getReason() != de.akubix.keyminder.core.exceptions.StorageExceptionType.UserCancelation){
				alert(String.format(locale.getString("application.unable_to_open_file"), file.getName(), e.getMessage()));
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

		fireEvent(DefaultEvent.OnFileOpened);

		buildQuicklinkList();

		if(tree.getSelectedNode().getId() != 0){fireEvent(TreeNodeEvent.OnSelectedItemChanged, tree.getSelectedNode());}
	}

	public synchronized boolean saveFile(){
		try {
			if(currentFile != null) {
				storageManager.getStorageHandler(currentFile.getFileTypeIdentifier()).save(currentFile, tree, this);
				tree.setTreeChangedStatus(false);
				updateStatus(String.format(locale.getString("application.file_saved"), currentFile.getFilepath().getName()));
			}
			return true;
		} catch (IllegalArgumentException e) {
			if(requestYesNoDialog(APP_NAME,	String.format(locale.getString("application.invalid_file_type"),
														  currentFile.getFileTypeIdentifier()))){
				currentFile.changeFileTypeIdentifier(this, StorageManager.defaultFileType);
				return saveFile();
			}
			else{
				return false;
			}

		} catch (StorageException e) {
			alert(String.format(locale.getString("application.unable_to_save_file"), e.getMessage()));
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
					if(!inputSourceProvider.getYesNoChoice(null, null, "Do you want do discard your changes?")){
						return false;
					}
				}
			}

			tree.enableEventFireing(false);
			tree.reset();

			if(currentFile.getEncryptionManager() != null){currentFile.getEncryptionManager().destroy();}
			currentFile = null;
			quicklinks.clear();
			tree.setTreeChangedStatus(false);
			tree.enableEventFireing(true);
			fireEvent(DefaultEvent.OnFileClosed);

			updateStatus(locale.getString("application.file_closed"));
			return true;
		}
		else{
			return true;
		}
	}

	public boolean createNewFile(File file, boolean encryptFileWithDefaultCipher){
		return createNewFile(file, StorageManager.defaultFileType, encryptFileWithDefaultCipher);
	}

	public synchronized boolean createNewFile(File file, String fileTypeIdentifier, boolean encryptFileWithDefaultCipher) {
		if(currentFile != null){
			if(!closeFile()){return false;}
		}

		currentFile = new FileConfiguration(file, "(not set)", encryptFileWithDefaultCipher, fileTypeIdentifier, encryptFileWithDefaultCipher ? new EncryptionManager(true) : null, new HashMap<>(), new HashMap<>());

		if(encryptFileWithDefaultCipher){
			try {
				if(!currentFile.getEncryptionManager().requestPasswordInputWithConfirm(this,
						locale.getString("application.createfile.setpassword.headline"),
						locale.getString("application.createfile.setpassword.text"),
						locale.getString("application.createfile.setpassword.confirmtext"))){
					alert(locale.getString("application.createfile.setpassword.passwords_not_equal"));
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

		updateStatus(String.format(locale.getString("application.file_created"), file.getName()));
		return true;
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
	 * Quicklinks
	 * ========================================================================================================================================================
	 */

	private Map<String, Integer> quicklinks = new HashMap<>();

	private synchronized void buildQuicklinkList(){
		quicklinks.clear();
		if(!getSettingsValueAsBoolean("nodes.disable_quicklinks", false)){
			tree.allNodes((node) -> {
				if(node.hasAttribute(NODE_ATTRIBUTE_QUICKLINK)){
					quicklinks.put(node.getAttribute(NODE_ATTRIBUTE_QUICKLINK), node.getId());
				}
			});
		}
		fireEvent(DefaultEvent.OnQuicklinksUpdated);
	}

	public synchronized Set<String> getQuicklinks(){
		return quicklinks.keySet();
	}

	public synchronized TreeNode getQuicklinkNode(String quicklinkName){
		if(quicklinks.containsKey(quicklinkName)){
			return tree.getNodeById(quicklinks.get(quicklinkName));
		}
		return null;
	}

	public synchronized void addQuicklink(String quicklinkName, TreeNode node){
		// Currently "quicklinks" cannot be undone - therefore the undo manager is temporary disabled
		boolean undoWasEnabled = tree.undoManager.isEnabled();
		if(quicklinks.containsKey(quicklinkName)){
			TreeNode oldNode = tree.getNodeById(quicklinks.get(quicklinkName));
			if(oldNode != null){
				oldNode.removeAttribute(NODE_ATTRIBUTE_QUICKLINK);
			}
		}

		node.setAttribute(NODE_ATTRIBUTE_QUICKLINK, quicklinkName);
		quicklinks.put(quicklinkName, node.getId());
		tree.undoManager.setEnable(undoWasEnabled);
		fireEvent(DefaultEvent.OnQuicklinksUpdated);
	}

	public void removeQuicklink(String quicklinkName){
		// Currently "quicklinks" cannot be undone - therefore the undo manager is temporary disabled
		boolean undoWasEnabled = tree.undoManager.isEnabled();
		if(quicklinks.containsKey(quicklinkName)){
			TreeNode node = tree.getNodeById(quicklinks.get(quicklinkName));
			if(node != null){
				node.removeAttribute(NODE_ATTRIBUTE_QUICKLINK);
			}
		}

		quicklinks.remove(quicklinkName);
		tree.undoManager.setEnable(undoWasEnabled);
		fireEvent(DefaultEvent.OnQuicklinksUpdated);
	}

	/*
	 * ========================================================================================================================================================
	 * Modules and Interface "EventHost"
	 * ========================================================================================================================================================
	 */

	public void registerFXUserInterface(FxUserInterface fxUI){
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
	 * @param title the window title (this value is ignored in ConsoleMode)
	 * @param text the label text
	 * @param defaultValue the default value
	 * @return the string entered by the user
	 * @throws UserCanceledOperationException if the user canceled the operation
	 */
	public String requestStringInput(String title, String text, String defaultValue) throws UserCanceledOperationException{
		return inputSourceProvider.getStringInput(title, text, defaultValue);
	}

	/**
	 * Requests password input (if the JavaFX user interface is loaded it will be shown as graphical dialog)
	 * @param title the window title (this value is ignored in ConsoleMode)
	 * @param text the label text
	 * @param passwordHint the password hint (if any)
	 * @return The password as char array
	 * @throws UserCanceledOperationException
	 */
	public char[] requestPasswordInput(String title, String text, String passwordHint) throws UserCanceledOperationException {
		return inputSourceProvider.getPasswordInput(title, text, passwordHint);
	}

	/**
	 * Request a yes no input dialog (if the JavaFX user interface is loaded it will be shown as graphical dialog)
	 * @param windowTitle the window title
	 * @param labelText the label text
	 * @return {@code true} if the user clicked "yes", {@code false} if it was "no"
	 */
	public boolean requestYesNoDialog(String title, String text) {
		return inputSourceProvider.getYesNoChoice(title, null, text);
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
		inputSourceProvider.updateStatus(text);
	}

	/**
	 * Write something to the application log (will be written to "standard out" or maybe to an open KeyMinder Terminal)
	 * @param text the text you want to log
	 */
	public void log(String text){
		inputSourceProvider.log(text);
	}

	/**
	 * Show a message window (or print to "standard out" if the JavaFX user interface is not available)
	 * @param text the text you want to display
	 */
	public void alert(String text){
		inputSourceProvider.alert(text);
	}

	/*
	 * ========================================================================================================================================================
	 * Interface ShellOutputWriter
	 * ========================================================================================================================================================
	 */
	@Override
	public void print(String text) {
		outputWriter.forEach((out) -> out.print(text));
	}

	@Override
	public void println(String text) {
		outputWriter.forEach((out) -> out.println(text));
	}

	@Override
	public void printf(String text, Object... args){
		outputWriter.forEach((out) -> out.printf(text, args));
	}

	@Override
	public void setColor(AnsiColor color){
		outputWriter.forEach((out) -> out.setColor(color));
	}

	public void startOutputRedirect(ShellOutputWriter target){
		outputWriter.add(target);
	}

	public void terminateOutputRedirect(ShellOutputWriter writer){
		outputWriter.remove(writer);
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
