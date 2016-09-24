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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;

import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import de.akubix.keyminder.core.db.StandardTree;
import de.akubix.keyminder.core.db.Tree;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.encryption.EncryptionManager;
import de.akubix.keyminder.core.events.Compliance;
import de.akubix.keyminder.core.events.ComplianceEventHandler;
import de.akubix.keyminder.core.events.DefaultEventHandler;
import de.akubix.keyminder.core.events.EventTypes;
import de.akubix.keyminder.core.events.EventTypes.ComplianceEvent;
import de.akubix.keyminder.core.events.EventTypes.DefaultEvent;
import de.akubix.keyminder.core.events.EventTypes.TreeNodeEvent;
import de.akubix.keyminder.core.events.TreeNodeEventHandler;
import de.akubix.keyminder.core.exceptions.IllegalCallException;
import de.akubix.keyminder.core.exceptions.StorageException;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.core.io.StorageManager;
import de.akubix.keyminder.core.io.XML;
import de.akubix.keyminder.core.modules.ModuleLoader;
import de.akubix.keyminder.locale.LocaleLoader;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.Shell;
import de.akubix.keyminder.shell.io.ShellOutputWriter;
import de.akubix.keyminder.ui.KeyMinderUserInterface;
import de.akubix.keyminder.ui.UserInterface;
import de.akubix.keyminder.util.Utilities;

/**
 * This class is the core of this application, it provides all functions and methods for the whole event handling, manages the loading of all modules
 * and creates the database. Furthermore it is the only interface to load and store data from respectively into files.
 */
public class ApplicationInstance implements ShellOutputWriter {

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

	private static final String DEFAULT_SETTINGS_FILE = "keyminder_settings.xml";

	/* Other variables */

	private File settingsFile;
	private UserInterface ui;

	private final Map<String, String> settings = new HashMap<>();
	private FileConfiguration currentFile = null;

	private Map<String, List<Object>> eventCollection = new HashMap<>();

	private StandardTree tree;
	private final Shell shell;
	private final ModuleLoader moduleLoader;
	private final StorageManager storageManager;
	private final ResourceBundle locale;
	private final KeyMinderUserInterface userInterfaceInformation;

	private final Set<ShellOutputWriter> outputWriter;

	public ApplicationInstance(UserInterface ui){
		this(ui, false);
	}

	public ApplicationInstance(UserInterface ui, boolean forceEnglishLocale) throws IllegalArgumentException {
		this.ui = ui;

		this.userInterfaceInformation = ui.getClass().getAnnotation(KeyMinderUserInterface.class);

		if(this.userInterfaceInformation == null){
			throw new IllegalArgumentException("User interface is not annotated with '" + KeyMinderUserInterface.class.getName() + "'.");
		}

		outputWriter = new HashSet<>();
		outputWriter.add(new ConsoleOutput());

		tree = new StandardTree(this);
		storageManager = new StorageManager();

		this.settingsFile = new File(KeyMinder.environment.getOrDefault(KeyMinder.ENVIRONMENT_KEY_SETTINGS_FILE, DEFAULT_SETTINGS_FILE));

		if(KeyMinder.environment.containsKey(KeyMinder.ENVIRONMENT_KEY_SETTINGS_FILE)){
			settingsFile = new File(KeyMinder.environment.get(KeyMinder.ENVIRONMENT_KEY_SETTINGS_FILE));
		}
		else{
			settingsFile = new File(DEFAULT_SETTINGS_FILE);
			if(!settingsFile.exists()){
				// There is no configuration file next to the jar -> maybe there is a global configuration file, placed in the users home directory?
				File globalSettingsFile = new File(System.getProperty("user.home") + System.getProperty("file.separator") + DEFAULT_SETTINGS_FILE);
				if(globalSettingsFile.exists()){
					this.settingsFile = globalSettingsFile;
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
		LocaleLoader.provideBundle("core", locale);

		this.shell = new Shell(this);
		shell.loadCommandsFromFile("/de/akubix/keyminder/shell/defaultCommands");

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

	public UserInterface getUserInterface(){
		return ui;
	}

	public KeyMinderUserInterface getUserInterfaceInformation(){
		return this.userInterfaceInformation;
	}

	public StorageManager getStorageManager(){
		return this.storageManager;
	}

	public Map<String, String> getSettingsMap(){
		return this.settings;
	}

	public FileConfiguration getCurrentFile(){
		return this.currentFile;
	}

	public boolean isAnyFileOpened(){
		return (this.currentFile != null);
	}

	/*
	 * ========================================================================================================================================================
	 * Startup method
	 * ========================================================================================================================================================
	 */

	/**
	 * The Startup-Method must be called by the UserInterface when it is ready.
	 * @param enableModuleLoading specify if modules should be loaded or not
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

		String value = KeyMinder.environment.getOrDefault(KeyMinder.ENVIRONMENT_KEY_COMMANDLINE_FILE, settings.get(SETTINGS_KEY_DEFAULT_FILE));
		if(value == null || value.equals("")){
			return;
		}
		File file = new File(value);

		if(file.exists()){
			if(!KeyMinder.environment.containsKey(KeyMinder.ENVIRONMENT_KEY_PASSWORD)){
				openFile(file);
			}
			else{
				openFile(file, KeyMinder.environment.get(KeyMinder.ENVIRONMENT_KEY_PASSWORD));
				KeyMinder.environment.remove(KeyMinder.ENVIRONMENT_KEY_PASSWORD);
			}
		}
		else{
			printf("Cannot open file '%s'. File does not exist.\n", file.getAbsolutePath());
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
				XML.convertXmlToMap(XML.loadXmlDocument(settingsFile), settings, true);
				loadDefaultEnvironmentXMLFile();
			}
		} catch (SAXException | IOException e) {
			println("Cannot load settings from xml file. " + e.getMessage());
		}
	}

	private void loadDefaultEnvironmentXMLFile(){

		if(settings.containsKey("startup.default_environment")){
			try {
				File environmrntXMLFile = new File(settings.get("startup.default_environment"));
				if(environmrntXMLFile.exists()){
					XML.convertXmlToMap(XML.loadXmlDocument(environmrntXMLFile), KeyMinder.environment, false);
					if(KeyMinder.environment.containsKey("verbose_mode") && Utilities.isYes(KeyMinder.environment.get("verbose_mode"))){
						KeyMinder.verbose_mode = true;
					}
				}
				else{
					println(String.format("Warning: Cannot find environment file \"%s\"", settings.get("startup.default_environment")));
				}
			} catch (SAXException | IOException e) {
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
			return currentFile.getFileSettings().containsKey(name);
		}
		return false;
	}

	public String getFileSettingsValue(String name){
		if(currentFile != null){
			return currentFile.getFileSettings().getOrDefault(name, "");
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
		currentFile.getFileSettings().put(name, value);
	}

	public boolean removeFileSettingsValue(String name){
		if(currentFile == null){
			throw new IllegalStateException("Unable to change the file settings if not file is opened!");
		}

		return (currentFile.getFileSettings().remove(name) != null);
	}

	public Set<String> getSettingsKeySet(){
		return settings.keySet();
	}

	public Set<String> getFileSettingsKeySet(){
		if(currentFile == null){
			throw new IllegalStateException("Unable to change the file settings if not file is opened!");
		}

		return currentFile.getFileSettings().keySet();
	}

	/**
	 * Tell the ApplicationInstance that some settings may have changed an fire the assigned events
	 */
	public void saveSettings(){

		try {
			XML.writeXmlDocumentToFile(settingsFile, XML.convertMapToXmlDocument(settings, "keyminder_settings"));
			fireEvent(DefaultEvent.OnSettingsChanged);

		} catch (IOException | TransformerException e) {
			alert("Unable to save KeyMinder settings: " + e.getMessage());
		}
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
		return openFile(file, filepassword, storageManager.getIdentifierByExtension(file.getName(), StorageManager.DEFAULT_FILE_TYPE));
	}

	public synchronized boolean openFile(File file, String filepassword, String fileTypeIdentifier){
		if(fileTypeIdentifier == null || fileTypeIdentifier.equals("")){
			println("Warning: Unknown file type - assuming 'KeyMind XML file (" + StorageManager.DEFAULT_FILE_TYPE + ")'.");
			fileTypeIdentifier = StorageManager.DEFAULT_FILE_TYPE;
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
				currentFile.changeFileTypeIdentifier(this, StorageManager.DEFAULT_FILE_TYPE);
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
			if(fireEvent(ComplianceEvent.AllowFileClosing) != Compliance.AGREE){return false;}

			if(tree.treeHasBeenUpdated()){
				Compliance discardChanges = fireEvent(ComplianceEvent.DiscardChanges);

				if(discardChanges == Compliance.CANCEL){return false;}
				if(discardChanges == Compliance.DONT_AGREE){
					if(!saveFile()){return false;}
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
		return createNewFile(file, StorageManager.DEFAULT_FILE_TYPE, encryptFileWithDefaultCipher);
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
	 * Variables
	 * ========================================================================================================================================================
	 */

	public String lookup(String varName) throws IllegalArgumentException {
		try {
			return lookup(varName, (tree.getSelectedNode().getId() != 0 ? tree.getSelectedNode() : null), null);
		}
		catch(IllegalArgumentException e){
			return "";
		}
	}

	public boolean variableIsDefined(String varName){
		return variableIsDefined(varName, (tree.getSelectedNode().getId() != 0 ? tree.getSelectedNode() : null), null);
	}

	public boolean variableIsDefined(String varName, TreeNode treeNode, Map<String, String> additionalVarMap){
		try {
			lookup(varName, treeNode, additionalVarMap);
			return true;
		}
		catch(IllegalArgumentException e){
			return false;
		}
	}

	/**
	 * Returns the value of the variable
	 * @param varName The name of the variable (without ${...})
	 * @return The value of the variable OR "" if there is no value for this variable
	 */
	public String lookup(String varName, TreeNode treeNode, Map<String, String> additionalVarMap) throws IllegalArgumentException {

		// There are multiple sources for the values of the variables: The variables of this document stored in 'variables' or as part of the node attributes or ...
		// Hint: Take a look at the order - you can "overwrite" node attributes because they will be first looked up in 'variables'

		if(additionalVarMap != null){
			if(additionalVarMap.containsKey(varName)){return additionalVarMap.get(varName);}
		}

		if(treeNode != null){
			if(treeNode.hasAttribute(varName)){
				return treeNode.getAttribute(varName);
			}
		}


		if(fileSettingsContainsKey(varName)){
			return getFileSettingsValue(varName);
		}

		if(settingsContainsKey(varName)){
			return getSettingsValue(varName);
		}

		if(varName.toLowerCase().equals("text") && treeNode != null){
				return treeNode.getText();
		}

		throw new IllegalArgumentException(String.format("Undefined variable: '%s'", varName));
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

	/**
	 * Adds an event handler. All events have to be fired by the thread of the user interface (if there is one).
	 * @param eventName the name of the event
	 * @param eventHandler the handler which will be execution when the event is triggered
	 */
	public void addEventHandler(DefaultEvent eventName, DefaultEventHandler eventHandler){
		addEventHandler(eventName.toString(), eventHandler);
	}

	/**
	 * Adds an event handler. All events have to be fired by the thread of the user interface (if there is one).
	 * @param eventName the name of the event
	 * @param eventHandler the handler which will be execution when the event is triggered
	 */
	public void addEventHandler(ComplianceEvent eventName, ComplianceEventHandler eventHandler){
		addEventHandler(eventName.toString(), eventHandler);
	}

	/**
	 * Adds an event handler. All events have to be fired by the thread of the user interface (if there is one).
	 * @param eventName the name of the event
	 * @param eventHandler the handler which will be execution when the event is triggered
	 */
	public void addEventHandler(TreeNodeEvent eventName, TreeNodeEventHandler eventHandler){
		addEventHandler(eventName.toString(), eventHandler);
	}

	/**
	 * Adds an event handler. All events have to be fired by the thread of the user interface (if there is one).
	 * @param eventName the name of the event
	 * @param eventHandler the handler which will be execution when the event is triggered
	 */
	public void addEventHandler(String eventName, Object eventHandler){
		if(eventCollection.containsKey(eventName)){
			eventCollection.get(eventName).add(eventHandler);
		}
		else{
			List<Object> eventList = new ArrayList<>(16);
			eventList.add(eventHandler);
			eventCollection.put(eventName, eventList);
		}
	}

	/**
	 * This method will fire an event, according to this all registered event handlers for this event will be called.
	 * Note: If the any (graphical) user interface is been loaded, this method has to be called with the UI thread.
	 * @param event the event that should be triggered
	 * @throws IllegalCallException If the JavaFX user interface is loaded, an this method is not called with JavaFX Thread.
	 */
	public synchronized void fireEvent(EventTypes.DefaultEvent event) throws IllegalCallException {
		if(!ui.isUserInterfaceThread()){
			throw new IllegalCallException("All events must be fired with the user interface thread.");
		}

		if(eventCollection.containsKey(event.toString())){
			eventCollection.get(event.toString()).forEach((handler) -> {
				((DefaultEventHandler) handler).eventFired();
			});
		}
	}

	/**
	 * This method will fire an event, according to this all registered event handlers for this event will be called.
	 * Currently this event is only fired to ask if the file has unsaved changes an can be closed.
	 * Note: If the any (graphical) user interface is been loaded, this method has to be called with the UI thread.
	 * @param event the event that should be fired
	 * @return the {@link Compliance} of all event handler
	 * @throws IllegalCallException If the JavaFX user interface is loaded, an this method is not called with JavaFX Thread.
	 */
	public synchronized Compliance fireEvent(EventTypes.ComplianceEvent event) throws IllegalCallException {
		if(!ui.isUserInterfaceThread()){
			throw new IllegalCallException("All events must be fired with the user interface thread.");
		}

		Compliance returnValue = Compliance.AGREE;
		if(eventCollection.containsKey(event.toString())) {
			for(int i = 0; i < eventCollection.get(event.toString()).size(); i++){
				Compliance c = ((ComplianceEventHandler) eventCollection.get(event.toString()).get(i)).eventFired();
				if(c != Compliance.AGREE){
					if(returnValue != Compliance.CANCEL){
						returnValue = c;
					}
				}
			}
		}

		return returnValue;
	}

	/**
	 * This method will fire an event, according to this all registered event handlers for this event will be called.
	 * Note: If the any (graphical) user interface is been loaded, this method has to be called with the UI thread.
	 * @param event the event that should be triggered
	 * @param node the node that belongs to this event
	 * @throws IllegalCallException If this method is not called by the thread of the user interface
	 */
	public synchronized void fireEvent(EventTypes.TreeNodeEvent event, TreeNode node) throws IllegalCallException {
		if(!ui.isUserInterfaceThread()){
			throw new IllegalCallException("All events must be fired with the user interface thread.");
		}

		if(eventCollection.containsKey(event.toString())){
			eventCollection.get(event.toString()).forEach((handler) -> {
				((TreeNodeEventHandler) handler).eventFired(node);
			});
		}
	}

	public List<Object> getEventHandler(String eventName){
		return eventCollection.getOrDefault(eventName, new ArrayList<>(0));
	}

	/*
	 * ========================================================================================================================================================
	 * Plugins for JavaFX Interface
	 * ========================================================================================================================================================
	 */

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
		return ui.getStringInput(title, text, defaultValue);
	}

	/**
	 * Requests password input (if the JavaFX user interface is loaded it will be shown as graphical dialog)
	 * @param title the window title (this value is ignored in ConsoleMode)
	 * @param text the label text
	 * @param passwordHint the password hint (if any)
	 * @return The password as char array
	 * @throws UserCanceledOperationException if the user canceled the operation
	 */
	public char[] requestPasswordInput(String title, String text, String passwordHint) throws UserCanceledOperationException {
		return ui.getPasswordInput(title, text, passwordHint);
	}

	/**
	 * Request a yes no input dialog (if the JavaFX user interface is loaded it will be shown as graphical dialog)
	 * @param title the window title
	 * @param text the label text
	 * @return {@code true} if the user clicked "yes", {@code false} if it was "no"
	 */
	public boolean requestYesNoDialog(String title, String text) {
		return ui.getYesNoChoice(title, null, text);
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
		ui.updateStatus(text);
	}

	/**
	 * Write something to the application log (will be written to "standard out" or maybe to an open KeyMinder Terminal)
	 * @param text the text you want to log
	 */
	public void log(String text){
		ui.log(text);
	}

	/**
	 * Show a message window (or print to "standard out" if the JavaFX user interface is not available)
	 * @param text the text you want to display
	 */
	public void alert(String text){
		ui.alert(text);
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
