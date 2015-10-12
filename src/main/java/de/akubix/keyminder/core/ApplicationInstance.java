/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

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
import java.lang.reflect.Constructor;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.modelmbean.XMLParseException;

import de.akubix.keyminder.core.db.StandardTree;
import de.akubix.keyminder.core.db.Tree;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.exceptions.StorageException;
import de.akubix.keyminder.core.interfaces.Command;
import de.akubix.keyminder.core.interfaces.CommandOutputProvider;
import de.akubix.keyminder.core.interfaces.FxAdministrationInterface;
import de.akubix.keyminder.core.interfaces.FxUserInterface;
import de.akubix.keyminder.core.interfaces.Module;
import de.akubix.keyminder.core.interfaces.ModuleProperties;
import de.akubix.keyminder.core.interfaces.events.DefaultEventHandler;
import de.akubix.keyminder.core.interfaces.events.EventHost;
import de.akubix.keyminder.core.interfaces.events.EventTypes.BooleanEvent;
import de.akubix.keyminder.core.interfaces.events.EventTypes.DefaultEvent;
import de.akubix.keyminder.core.interfaces.events.EventTypes.SettingsEvent;
import de.akubix.keyminder.core.interfaces.events.EventTypes.TreeNodeEvent;
import de.akubix.keyminder.core.io.StorageManager;
import de.akubix.keyminder.lib.Tools;
import de.akubix.keyminder.lib.XMLCore;
import de.akubix.keyminder.ui.fx.MainWindow;
import javafx.scene.control.TabPane;

/**
 * This class is the core of this application, it provides all functions and methods for the whole event handling, manages the loading of all modules
 * and creates the database. Furthermore it is the only interface to load and store data from respectively into files.
 */
public class ApplicationInstance implements EventHost, CommandOutputProvider {

	/* Static configurations variables */
	
	public static String APP_VERSION = "0.1";
	public static final String APP_ICON = "/de/akubix/keyminder/images/app/AppIcon32.png";
	public static final String APP_NAME = "KeyMinder";

	public static final String NODE_ATTRIBUTE_CREATION_DATE = "created";
	public static final String NODE_ATTRIBUTE_MODIFICATION_DATE = "modified";
	public static final String NODE_ATTRIBUTE_FAVORITE_NODE = "favorite";
	public static final String NODE_ATTRIBUTE_LINKED_NODES = "linked_nodes";
	
	private static final String SETTINGS_KEY_ENABLED_MODULES = "enabled_modules";
	public static final String SETTINGS_KEY_DEFAULT_FILE = "startup.defaultfile";
	public static final String SETTINGS_KEY_USE_OTHER_WEB_BROWSER = "etc.useotherbrowser";
	public static final String SETTINGS_KEY_BROWSER_PATH = "etc.browserpath";

	/* Other variables */
	
	private File settingsFile;
	private FxAdministrationInterface fxInterface = null;

	public Map<String, String> settings = new HashMap<String, String>();
	public FileConfiguration currentFile = null;

	private Map<String, Command> commands = new HashMap<String, Command>();
	private Map<String, String> commandManual = new HashMap<String, String>();
	
	private StandardTree tree;
	private CommandOutputProvider outputRedirect = null;
	public final StorageManager storageManager;
	public final Locale applicationLocale;
	
	public ApplicationInstance()
	{
		Package p = getClass().getPackage();
		if(p.getImplementationVersion() != null){
			APP_VERSION = p.getImplementationVersion();
		}

		tree = new StandardTree(this);
		storageManager = new StorageManager();

		final String defaultSettingsFile = "keyminder_settings.xml";
		
		if(Launcher.environment.containsKey("cmd.settingsfile"))
		{
			settingsFile = new File(Launcher.environment.get("cmd.settingsfile"));
		}
		else
		{
			settingsFile = new File(defaultSettingsFile);
			if(!settingsFile.exists())
			{
				// There is no configuration file next to the jar -> maybe there is a global configuration file, placed in the users home directory?
				File globalSettingsFile = new File(System.getProperty("user.home") + System.getProperty("file.separator") + defaultSettingsFile);
				if(globalSettingsFile.exists())
				{
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
				if(Launcher.verbose_mode){printf("Unsupported locale: '%s'. Using 'en/EN' instead...\n", language);}
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
	}

	public Tree getTree()
	{
		return tree;
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
	public void startup()
	{
		loadModules();

		provideCoreCommands();
		ConsoleMode.provideDefaultCommands(this);
		
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
	public void loadDefaultFile()
	{
		if(Launcher.environment.containsKey("cmd.file"))
		{
			File f = new File(Launcher.environment.get("cmd.file"));
			
			if(f.exists())
			{
				if(!Launcher.environment.containsKey("cmd.password"))
				{
					openFile(f);
				}
				else
				{
					openFile(f, Launcher.environment.get("cmd.password"));
					Launcher.environment.remove("cmd.password");
				}
			}
			else
			{
				println("Cannot open file \"" + f.getAbsolutePath() + "\". File does not exist.");
			}
		}
		else
		{
			if(settings.containsKey(SETTINGS_KEY_DEFAULT_FILE))
			{
				File f = new File(settings.get(SETTINGS_KEY_DEFAULT_FILE));
				if(f.exists()){
					if(!Launcher.environment.containsKey("cmd.password"))
					{
						openFile(f);
					}
					else
					{
						openFile(f, Launcher.environment.get("cmd.password"));
						Launcher.environment.remove("cmd.password");
					}
				}
				else
				{
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
	
	private void presetDefaultSettings()
	{	
		settings.put(SETTINGS_KEY_ENABLED_MODULES, "KeyClip;Sidebar;");
	}

	private void loadSettingsFromXMLFile()
	{
		try {
			if(settingsFile.exists())
			{
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
					XMLCore.xml2Map(environmrntXMLFile, Launcher.environment, false);
					if(Launcher.environment.containsKey("verbose_mode") && de.akubix.keyminder.lib.Tools.isYes(Launcher.environment.get("verbose_mode"))){Launcher.verbose_mode = true;}
				}
				else
				{
					println(String.format("Warning: Cannot find environment file \"%s\"", settings.get("startup.default_environment")));
				}
			} catch (XMLParseException e) {
				println("Cannot load default environment from xml file: " + e.getMessage());
			}
		}
	}
	
	public boolean settingsContainsKey(String name)
	{
		return settings.containsKey(name);
	}
	
	public String getSettingsValue(String name)
	{
		return settings.getOrDefault(name, "");
	}
	
	public String setSettingsValue(String name, String value) throws IllegalArgumentException, IllegalStateException
	{
		if(!name.matches("[a-zA-Z0-9[_][:]\\.]*")){throw new IllegalArgumentException("The name for the settings value contains an illegal character. Allowed characters: 'A-Z', 'a-z', '0-9', '.', '_' and ':'.");}
		return settings.put(name, value);
	}
	
	public Boolean getSettingsValueAsBoolean(String name, Boolean onNotExist){
		if(!settings.containsKey(name)){return onNotExist;}
		String value = settings.get(name).toLowerCase();
		if(value.equals("yes") || value.equals("true") || value.equals("1")){return true;}else{return false;}
	}

	public boolean fileSettingsContainsKey(String name)
	{
		if(currentFile != null){
			return currentFile.fileSettings.containsKey(name);
		}
		return false;
	}
	
	public String getFileSettingsValue(String name)
	{
		if(currentFile != null){
			return currentFile.fileSettings.getOrDefault(name, "");
		}
		return "";
	}
	
	public void setFileSettingsValue(String name, String value) throws IllegalArgumentException, IllegalStateException
	{
		if(currentFile == null){throw new IllegalStateException("Unable to change the file settings if not file is opened!");}
		if(!name.matches("[a-zA-Z0-9[_][:]\\.]*")){throw new IllegalArgumentException("The name for the file settings value contains an illegal character. Allowed characters: 'A-Z', 'a-z', '0-9', '.', '_' and ':'.");}
		currentFile.fileSettings.put(name, value);
	}
	
	/**
	 * Tell the ApplicationInstance that some settings may have changed an fire the assigned events
	 */
	public void settingsHasBeenUpdated()
	{	
		XMLCore.saveMapAsXMLFile(settingsFile, settings, "keyminder_settings");
		fireEvent(DefaultEvent.OnSettingsChanged);
	}
	
	/**
	 * Tell the ApplicationInstance that some file settings may have changed an fire the assigned events
	 */
	public void fileSettingsHasBeenUpdated()
	{
		saveFile();
		fireEvent(DefaultEvent.OnFileSettingsChanged);
	}
		
	/*
	 * ========================================================================================================================================================
	 * File handling
	 * ========================================================================================================================================================
	 */
	
	public boolean openFile(File file)
	{
		return openFile(file, "");
	}
	
	public boolean openFile(File file, String filepassword)
	{
		return openFile(file, filepassword, storageManager.getIdentifierByExtension(file.getName(), StorageManager.defaultFileType));
	}

	public synchronized boolean openFile(File file, String filepassword, String fileTypeIdentifier)
	{
		if(fileTypeIdentifier == null || fileTypeIdentifier.equals("")){
			println("Warning: Unknown file type - assuming 'KeyMind XML file (" + StorageManager.defaultFileType + ")'.");
			fileTypeIdentifier = StorageManager.defaultFileType;
		}

		if(!storageManager.hasStorageHandler(fileTypeIdentifier)){
			println(String.format("Unknown file type identifier \"%s\". Canceling...", fileTypeIdentifier));
			return false;
		}
		
		if(currentFile != null)
		{
			if(!closeFile()){return false;}
		}
		
		try {
			if(Launcher.verbose_mode){log(String.format("Opening file \"%s\" (as \"%s\")...", file.getAbsoluteFile(), fileTypeIdentifier));}
			
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
			if(e.getReason() != de.akubix.keyminder.core.exceptions.StorageExceptionType.UserCancelation)
			{
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
	
	public synchronized boolean saveFile()
	{
		try
		{
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
	public synchronized boolean closeFile()
	{
		if(currentFile != null)
		{
			if(fireEvent(BooleanEvent.DONTAllowFileClosing, true, true)){return false;}
			if(tree.treeHasBeenUpdated())
			{
				if(isFxUserInterfaceAvailable())
				{
					int ret = fxInterface.showSaveChangesDialog();
					if(ret < 0){return false;}
					if(ret > 0){if(!saveFile()){return false;}}
				}
				else
				{
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
		else
		{
			return true;
		}
	}

	public boolean createNewFile(File file)
	{
		return createNewFile(file, StorageManager.defaultFileType);
	}

	public synchronized boolean createNewFile(File file, String fileTypeIdentifier)
	{
		if(currentFile != null)
		{
			if(!closeFile()){return false;}
		}
		
		currentFile = new FileConfiguration(file, "(not set)", false, fileTypeIdentifier, null, new HashMap<>(), new HashMap<>());		

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
		if (isFxUserInterfaceAvailable())
		{
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

	public void forEachLinkedNode(TreeNode node, Consumer<TreeNode> forEach)
	{
		boolean listNeedsToBeCorrected = false;
		for(String idString: node.getAttribute(NODE_ATTRIBUTE_LINKED_NODES).split(";"))
		{
			try
			{
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

	public synchronized void addLinkedNode(TreeNode node, int idOfLinkedNode)
	{
		if(node.hasAttribute(NODE_ATTRIBUTE_LINKED_NODES)){
			String currentValue = node.getAttribute(NODE_ATTRIBUTE_LINKED_NODES);
			node.setAttribute(NODE_ATTRIBUTE_LINKED_NODES, currentValue + (currentValue.endsWith(";") ? "" : ";") + idOfLinkedNode);
		}
		else{
			node.setAttribute(NODE_ATTRIBUTE_LINKED_NODES, Integer.toString(idOfLinkedNode));
		}
	}

	public synchronized void removeLinkedNode(TreeNode node, int linkedId)
	{
		int cnt = 0;
		StringBuilder newLinkedNodesString = new StringBuilder();
		for(String idString: node.getAttribute(NODE_ATTRIBUTE_LINKED_NODES).split(";"))
		{
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
		else
		{
			node.removeAttribute(NODE_ATTRIBUTE_LINKED_NODES);
		}
	}

	/*
	 * ========================================================================================================================================================
	 * Favorite nodes
	 * ========================================================================================================================================================
	 */
	private int[] favoriteNodes = new int[10];

	private synchronized void clearFavoriteNodeList()
	{
		for(byte i = 0; i < favoriteNodes.length; i++)
		{
			favoriteNodes[i] = 0;
		}
	}

	private synchronized void generateFavoriteNodeListFromTree()
	{
		clearFavoriteNodeList();
		if(!getSettingsValueAsBoolean("nodes.disable_favorites", false)){
			boolean undoWasEnabled = tree.undoManager.isEnabled();
			tree.undoManager.setEnable(false);
			tree.allNodes((node) -> {if(node.hasAttribute(NODE_ATTRIBUTE_FAVORITE_NODE)){loadFavoriteNode(node, node.getAttribute(NODE_ATTRIBUTE_FAVORITE_NODE));}});
			if(isFxUserInterfaceAvailable()){fxInterface.buildFavoriteNodeList(favoriteNodes);}
			tree.undoManager.setEnable(undoWasEnabled);
		}
	}

	public boolean setFavoriteNode(TreeNode node)
	{
		if(currentFile != null){
			if(!node.hasAttribute(NODE_ATTRIBUTE_FAVORITE_NODE))
			{
				for(byte i = 0; i < favoriteNodes.length; i++)
				{
					// Find first unused place
					if(favoriteNodes[i] <= 0){
						return setFavoriteNode(node, i , true);
					}
				}
			}
		}
		return false;
	}

	private void loadFavoriteNode(TreeNode node, String favoriteNumber)
	{
		try {
			setFavoriteNode(node, Byte.parseByte(favoriteNumber), false);
		}
		catch(NumberFormatException numFormatEx)
		{
			if(Launcher.verbose_mode){println("Error while parsing favorite number of tree node " + node.getText());}
			node.removeAttribute(NODE_ATTRIBUTE_FAVORITE_NODE);
		}
		
	}

	public synchronized boolean setFavoriteNode(TreeNode node, byte favoriteNumber, boolean writeNodeAttribute)
	{
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

	public void removeFavoriteNode(TreeNode node)
	{
		if(currentFile != null){
			for(byte i = 0; i < favoriteNodes.length; i++)
			{
				if(node.getId() == favoriteNodes[i]){
					removeFavoriteNode(i);
				}
			}
		}
	}

	public synchronized void removeFavoriteNode(byte favoriteNumber)
	{
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

	public TreeNode getFavoriteNode(byte favoriteNumber)
	{
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

	public void registerFXUserInterface(MainWindow fxUI)
	{
		fxInterface = fxUI;
	}
	
	private Map<String, ModuleInfo> allModules = new HashMap<String, ModuleInfo>();
	private Map<String, List<Object>> eventCollection = new HashMap<>();

	// This code should work fine, but in my opinion there's currently to much overhead - by default a simple static array is used instead 
	private List<String> buildModuleList()
	{
		List<String> moduleList = new ArrayList<>();
		final String packagePrefix = "de/akubix/keyminder";
		try
		{
			URL url = getClass().getResource("/"+ packagePrefix + "/core/" + this.getClass().getSimpleName() + ".class");
			if(!url.getProtocol().equals("jar"))
			{
				// The classes are not in a jar file (most likely you are running this application from your IDE)
				url = this.getClass().getResource("./../modules/");
				if(url != null)
				{
					File dir;
					try {
						dir = new File(url.toURI());
						for (File nextFile : dir.listFiles())
						{
							String fileName = nextFile.getName();
							if(!fileName.contains("$") && fileName.endsWith(".class"))
							{
								moduleList.add(fileName.substring(0, fileName.length() - 6));
							}
						}
					}
					catch (URISyntaxException e) {
						println("ERROR: Generation of module list failed (URISyntaxException).");
						e.printStackTrace();
					}
				}
				else
				{
					println("ERROR: Unable to generate module list.");
				}
			}
			else
			{
				// The classes are packed into the jar file
				
				/* The following code was written by StackOverflow (stackoverflow.com) user erickson and is licensed under CC BY-SA 3.0 
				 * "Creative Commons Attribution-ShareAlike 3.0 Unported", http://creativecommons.org/licenses/by-sa/3.0/)
				 *
				 * Source: http://stackoverflow.com/questions/749533/how-to-walk-through-java-class-resources
				 * The code has been modified.
				 */

				JarURLConnection con = (JarURLConnection) url.openConnection();
				JarFile archive = con.getJarFile();
				
				// Search for the entries you care about
				Enumeration<JarEntry> entries = archive.entries();
				while (entries.hasMoreElements()) {
					JarEntry entry = entries.nextElement();
					if(entry.getName().matches(packagePrefix + "/modules/([A-Za-z0-9]*).class"))
					{
						String fileName = entry.getName();
						if(!fileName.contains("$") && fileName.endsWith(".class"))
						{
							// Extract the class name
							moduleList.add(fileName.substring(packagePrefix.length() + 9, fileName.length() - 6));
						}
					}
				}
				archive.close();
			}
		}
		catch(IOException ioex)
		{
			println("ERROR: Generation of module list failed.");
			ioex.printStackTrace();
		}
		
		return moduleList;
	}
	
	private void loadModules()
	{
		if(settings.containsKey(SETTINGS_KEY_ENABLED_MODULES))
		{
			List<String> availableModules = Launcher.environment.containsKey("dynamic_moduleloading") ?
											buildModuleList() :
											Arrays.asList(new String[]{"Sidebar", "KeyClip", "SSHTools", "Deadline"});

			List<String> enabledModules = Arrays.asList(settings.get(SETTINGS_KEY_ENABLED_MODULES).split(";"));
			if(enabledModules.size() == 0){return;}

			// Create a new JavaClassLoader
			ClassLoader classLoader = this.getClass().getClassLoader();
			for(String moduleName: availableModules)
			{
				// Load the target class using its binary name
				try {
					Class<?> loadedClass = classLoader.loadClass("de.akubix.keyminder.modules." + moduleName);

					//Get the module description by reading the class annotation
					ModuleProperties desc = loadedClass.getAnnotation(de.akubix.keyminder.core.interfaces.ModuleProperties.class);

					if(enabledModules.contains(moduleName))
					{
						// Create a new instance of the loaded class
						Constructor<?> constructor = loadedClass.getConstructor();
						Object myClassObject = constructor.newInstance();

						if((myClassObject instanceof Module))
						{
							allModules.put(moduleName, new ModuleInfo((Module) myClassObject, desc, true));
						}
						else
						{
							println(String.format("Cannot load module '%s'. Class is not a instance of 'Module'.", moduleName));
						}
					}
					else
					{
						//Module is not enabled
						allModules.put(moduleName, new ModuleInfo(null, desc, false));
					}

				} catch (ClassNotFoundException e) {
					println(String.format("Cannot load module '%s'. Class does not exit.", moduleName));
					if(Launcher.environment.containsKey("verbose_mode")){e.printStackTrace();}
				} catch (Exception e) {
					println(String.format("Cannot load module '%s'. Unknown exception.", moduleName));
					if(Launcher.environment.containsKey("verbose_mode")){e.printStackTrace();}
				}
			}

			//Start all modules, observing their dependencies to other modules
			for(String moduleName: enabledModules)
			{
				startModule(moduleName, new ArrayList<String>());
			}
		}
	}

	private void startModule(String moduleName, List<String> initiators)
	{
		if(!allModules.containsKey(moduleName)){return;}

		ModuleInfo m = allModules.get(moduleName);
		if(!m.isStarted() && m.isEnabled)
		{
			String dependencies = (m.properties == null ? "" : m.properties.dependencies().replace(" ", ""));
			if(!dependencies.equals(""))
			{
				initiators.add(moduleName);
				for(String dependent_module: dependencies.split(";"))
				{
					if(!initiators.contains(dependent_module))
					{
						startModule(dependent_module, initiators);
					}
					else
					{
						println(String.format("Warning: Cannot resolve module dependencies of '%s', because they are cyclic.", initiators.get(0)));
					}
				}
				initiators.remove(moduleName);
			}
			
			if(startModule(moduleName, m.moduleInstance))
			{
				m.setStarted();
			}
			else
			{
				allModules.put(moduleName, new ModuleInfo(null, m.properties, true)); //Remove the instance from the module list
			}
		}
	}
	
	/**
	 * Starts a module an handles the errors if the start fails
	 * @param name
	 * @param moduleInstance
	 * @return {@code true} if the module has been successfully started, {@code false} if not
	 */
	private boolean startModule(String name, Module moduleInstance)
	{
		if(moduleInstance == null){return false;}
		try {
			if(Launcher.environment.containsKey("verbose_mode")){println(String.format("Starting module \"%s\"... ", name));}
			moduleInstance.onStartup(this);
			return true;

		} catch (de.akubix.keyminder.core.exceptions.ModuleStartupException e) {
			switch(e.getErrorLevel())
			{
			case Critical:
				println("Critical error while loading module \"" + name + "\": " +e.getMessage());
				break;
			case Default:
				println("Cannot load module \"" + name + "\": " +e.getMessage());
				break;
			case FxUserInterfaceNotAvailable:
			case OSNotSupported:
				if(!Launcher.environment.containsKey("silent_mode"))
				{
					println("Cannot load module \"" + name + "\": " +e.getMessage());
				}
				break;
			}
		}
		return false;
	}
	
	/**
	 * Enable a module (will be loaded on next start)
	 * @param moduleClassName class name of the module
	 * @throws IllegalArgumentException if the module does not exist
	 */
	public void enableModule(String moduleClassName) throws IllegalArgumentException
	{
		if(allModules.containsKey(moduleClassName))
		{
			if(settings.containsKey(SETTINGS_KEY_ENABLED_MODULES))
			{
				String currentlyEnabledModules = settings.get(SETTINGS_KEY_ENABLED_MODULES);
				List<String> enabledModules = Arrays.asList(currentlyEnabledModules.split(";"));
				if(!enabledModules.contains(moduleClassName))
				{
					if(currentlyEnabledModules.endsWith(";")) {
						settings.put(SETTINGS_KEY_ENABLED_MODULES, currentlyEnabledModules + moduleClassName);
					}
					else {
						settings.put(SETTINGS_KEY_ENABLED_MODULES, currentlyEnabledModules + ";" + moduleClassName);
					}

					allModules.get(moduleClassName).isEnabled = true;
					settingsHasBeenUpdated();
				}
			}
			else
			{
				settings.put(SETTINGS_KEY_ENABLED_MODULES, moduleClassName);
			}
		}
		else
		{
			throw new IllegalArgumentException("Module does not exist.");
		}
	}

	/**
	 * Disable a module (won't be loaded on next start)
	 * @param moduleClassName the class name of the module
	 * @throws IllegalArgumentException if the module does not exist
	 */
	public void disableModule(String moduleClassName) throws IllegalArgumentException
	{
		if(allModules.containsKey(moduleClassName))
		{
			if(settings.containsKey(SETTINGS_KEY_ENABLED_MODULES))
			{
				List<String> enabledModules = new ArrayList<>();
				for(String s: settings.get(SETTINGS_KEY_ENABLED_MODULES).split(";"))
				{
					enabledModules.add(s);
				}

				if(enabledModules.contains(moduleClassName))
				{
					enabledModules.remove(moduleClassName);
					
					if(enabledModules.size() > 0)
					{
						StringBuilder sb = new StringBuilder("");
						for(String moduleName: enabledModules)
						{
							sb.append(moduleName + ";");
						}
						
						settings.put(SETTINGS_KEY_ENABLED_MODULES, sb.toString());
					}
					allModules.get(moduleClassName).isEnabled = false;
					settingsHasBeenUpdated();
				}
			}
		}
		else
		{
			throw new IllegalArgumentException("Module does not exist.");
		}
	}

	/**
	 * Returns a list, respectively a Set of all modules
	 * @return a list (as Set) of all modules
	 */
	public Set<String> getModules()
	{
		return allModules.keySet();
	}

	/**
	 * Returns the properties of a specific module
	 * @param moduleName the name of the module
	 * @return The ModuleInfo OR null if the module does not exist respectively the module does not have any properties
	 */
	public ModuleInfo getModuleInfo(String moduleName)
	{
		return allModules.get(moduleName);
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
	public synchronized void fireEvent(de.akubix.keyminder.core.interfaces.events.EventTypes.DefaultEvent event) throws de.akubix.keyminder.core.exceptions.IllegalCallException
	{
		if(isFxUserInterfaceAvailable()){
			if(!fxInterface.isFXThread()){throw new de.akubix.keyminder.core.exceptions.IllegalCallException("If there is an JavaFX UserInterface, all events must be fired with the FXThread!");}
		}

		if(eventCollection.containsKey(event.toString()))
		{
			for(int i = 0; i < eventCollection.get(event.toString()).size(); i++)
			{
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
	public synchronized boolean fireEvent(de.akubix.keyminder.core.interfaces.events.EventTypes.BooleanEvent event, boolean cancelOn, boolean cancelValue) throws de.akubix.keyminder.core.exceptions.IllegalCallException
	{
		if(isFxUserInterfaceAvailable()){
			if(!fxInterface.isFXThread()){throw new de.akubix.keyminder.core.exceptions.IllegalCallException("If there is an JavaFX UserInterface, all events must be fired with the FXThread!");}
		}

		if(eventCollection.containsKey(event.toString()))
		{
			for(int i = 0; i < eventCollection.get(event.toString()).size(); i++)
			{
				if(((de.akubix.keyminder.core.interfaces.events.BooleanEventHandler) eventCollection.get(event.toString()).get(i)).eventFired() == cancelOn){return cancelValue;}
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
	public synchronized void fireEvent(de.akubix.keyminder.core.interfaces.events.EventTypes.TreeNodeEvent event, TreeNode node) throws de.akubix.keyminder.core.exceptions.IllegalCallException
	{
		if(isFxUserInterfaceAvailable()){
			if(!fxInterface.isFXThread()){throw new de.akubix.keyminder.core.exceptions.IllegalCallException("If there is an JavaFX UserInterface, all events must be fired with the FXThread!");}
		}

		if(eventCollection.containsKey(event.toString()))
		{
			for(int i = 0; i < eventCollection.get(event.toString()).size(); i++)
			{
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
	public synchronized void fireEvent(de.akubix.keyminder.core.interfaces.events.EventTypes.SettingsEvent event, TabPane tabControl, Map<String, String> settings) throws de.akubix.keyminder.core.exceptions.IllegalCallException
	{
		if(isFxUserInterfaceAvailable()){
			if(!fxInterface.isFXThread()){throw new de.akubix.keyminder.core.exceptions.IllegalCallException("If there is an JavaFX UserInterface, all events must be fired with the FXThread!");}
		}

		if(eventCollection.containsKey(event.toString()))
		{
			for(int i = 0; i < eventCollection.get(event.toString()).size(); i++)
			{
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
	public boolean isFxUserInterfaceAvailable()
	{
		return (fxInterface != null);
	}
	
	/**
	 * This method allows you to access the JavaFX user interface
	 * @return Reference to the JavaFX user interface OR null if it is currently not available.
	 */
	public FxUserInterface getFxUserInterface()
	{
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
	 */
	public String requestStringInput(String windowTitle, String labelText, String defaultValue, boolean useAsPasswordDialog)
	{
		if(isFxUserInterfaceAvailable())
		{
			return fxInterface.showInputDialog(windowTitle, labelText, defaultValue, useAsPasswordDialog);
		}
		else
		{
			println("\n" + labelText + (defaultValue.equals("") ? "" : " [" + defaultValue + "]"));
			if(!useAsPasswordDialog)
			{
				return ConsoleMode.readLineFromSystemIn();
			}
			else
			{
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
	public boolean requestYesNoDialog(String windowTitle, String labelText)
	{
		if(isFxUserInterfaceAvailable())
		{
			return fxInterface.showYesNoDialog(windowTitle, null, labelText);
		}
		else
		{
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
		if(isFxUserInterfaceAvailable())
		{
			fxInterface.updateStatus(text);
		}
	}
	
	/**
	 * Write something to the application log (will be written to "standard out" or maybe to an open KeyMinder Terminal)
	 * @param text the text you want to log
	 */
	public void log(String text){
		if(isFxUserInterfaceAvailable())
		{
			fxInterface.log(text);
		}
		else
		{
			println(text);
		}
	}
	
	/**
	 * Show a message window (or print to "standard out" if the JavaFX user interface is not available)
	 * @param text the text you want to display
	 */
	public void alert(String text){
		if(isFxUserInterfaceAvailable())
		{
			fxInterface.alert(text);
		}
		else
		{
			println(" > " + text);
		}
	}
	
	/*
	 * ========================================================================================================================================================
	 * Execute commands
	 * ========================================================================================================================================================
	 */	
	
	/**
	 * Check if a command is available
	 * @param commandName the name of the command
	 * @return {@code true} if there is a command with this name, {@code} false if not 
	 */
	public boolean commandAvailable(String commandName)
	{
		return commands.containsKey(commandName);
	}
	
	/**
	 * Execute a command
	 * @param commandName the name of the command
	 * @return {@code null} if the command does not exist or the text that has been return from the command (note: this can be even {@code null})
	 */
	public String execute(String commandName)
	{
		return execute(this, commandName, new String[0]);
	}
	
	/**
	 * Execute a command with parameters
	 * @param commandName the name of the command
	 * @param parameters the parameters for this command
	 * @return {@code null} if the command does not exist or the text that has been return from the command (note: this can be even {@code null})
	 */
	public String execute(String commandName, String... parameters)
	{
		return execute(this, commandName, parameters);
	}
	
	/**
	 * Execute a command with parameters
	 * @param outputProvider the output provider for this command (by default the output provider is this class)
	 * @param commandName the name of the command
	 * @param args the parameters (or arguments) for this command
	 * @return {@code null} if the command does not exist or the text that has been return from the command (note: this can be even {@code null})
	 */
	public synchronized String execute(CommandOutputProvider outputProvider, String commandName, String[] args)
	{
		if(commands.containsKey(commandName.toLowerCase()))
		{
			return commands.get(commandName.toLowerCase()).runCommand(outputProvider, this, args);
		}
		else
		{
			return null;
		}
	}
	
	/**
	 * Splits a String by spaces that are not quoted. 'This is "an example"' will return the result {'This', 'is', 'an example'}
	 * @param paramString The string which should be split
	 * @return The array that contains the split strings
	 */
	public static String[] splitParameters(String paramString)
	{
		/* The following code was written by StackOverflow (stackoverflow.com) user Jan Goyvaerts and is licensed under CC BY-SA 3.0 
		 * "Creative Commons Attribution-ShareAlike 3.0 Unported", http://creativecommons.org/licenses/by-sa/3.0/)
		 *
		 * Source: http://stackoverflow.com/questions/366202/regex-for-splitting-a-string-using-space-when-not-surrounded-by-single-or-double
		 * The code hasn't been modified.
		 */

		List<String> matchList = new ArrayList<String>();
		Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
		Matcher regexMatcher = regex.matcher(paramString);
		while (regexMatcher.find()) {
			if (regexMatcher.group(1) != null) {
				// Add double-quoted string without the quotes
				matchList.add(regexMatcher.group(1));
			} else if (regexMatcher.group(2) != null) {
				// Add single-quoted string without the quotes
				matchList.add(regexMatcher.group(2));
			} else {
				// Add unquoted word
				matchList.add(regexMatcher.group());
			}
		}
		return matchList.toArray(new String[matchList.size()]);
	}
	
	public void provideNewCommand(String commandkey, Command kcmd)
	{
		commands.put(commandkey.toLowerCase(), kcmd);
	}
	
	public void provideNewCommand(String commandkey, Command kcmd, String manual)
	{
		commands.put(commandkey.toLowerCase(), kcmd);
		if(manual != null){
			if(!manual.equals("")){commandManual.put(commandkey.toLowerCase(), manual);}
		}
		
	}
	
	public void provideCommandAlias(String alias, String commandkey)
	{
		provideNewCommand(alias, new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				return instance.execute(out, commandkey, args);
			}}, commandManual.get(alias));
	}
	
	private void provideCoreCommands()
	{
		provideNewCommand("man", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				if(args.length == 1)
				{
					if(commands.containsKey(args[0])){
						if(commandManual.containsKey(args[0]))	{
							out.println("Manual entry for command \"" + args[0] + "\":\n\n" + commandManual.get(args[0]));
						}
						else {
							out.println("No manual entry for \"" + args[0] + "\" available.");
						}
					}
					else{
						out.println("Command \"" + args[0] + "\" is not available.");
					}
				}
				else {
					out.println("Usage: man <command>");
				}
				return null;
			}}, "Displays a manual (if available) to a specific command.");
		
		provideNewCommand("help", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				out.println("Available commands:\n");
				Tools.asSortedList(commandManual.keySet()).forEach((cmd) -> out.println(cmd));
				return null;
			}}, "Displays a list of all possible commands.");
		
		provideNewCommand("debug", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				if(args.length > 0){
					String[] newargs = new String[args.length -1];
					for(int i = 0; i < newargs.length; i++){newargs[i] = args[i + 1];}
					String ret = instance.execute(args[0], newargs);
					out.println((ret != null) ? "---\n" + ret : "The command returned nothing (null).");
					return ret;
				}
				else{
					return null;
				}
			}});
		
		provideNewCommand("file", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				if(args.length > 0)
				{
					switch(args[0].toLowerCase())
					{
						case "create":
						case "new":
							if(args.length == 2){
								File f = new File(args[1]);
								instance.createNewFile(f);
							}else {
								println("Usage: file create <filepath>");
							}
							return null;

						case "open":
							if(args.length == 2){
								return openFile(new File(args[1])) ? "ok" : "failed";
							} else if(args.length == 3) {
								return openFile(new File(args[1]), args[2]) ? "ok" : "failed";
							} else if(args.length == 4) {
								return openFile(new File(args[1]), args[2], args[3]) ? "ok" : "failed";
							}else {
								println("Usage: file open <file> [password] [file type identifier*]\n\n* Use 'file types' to get help.");
							}
							return null;

						case "save":
							if(currentFile != null){
								boolean status = saveFile();
								out.println(status ? "File saved." : "Error: File couldn't be saved.");
								return (status ? "ok" : "failed");
							}
							else{
								out.println("Cannot save file: There is no file currently opened.");
								return null;
							}

						case "saveas":
						case "saveat":
							if(currentFile != null){
								if(args.length == 2 || args.length == 3){
									if(args.length == 3){
										try{
											instance.currentFile.changeFileTypeIdentifier(instance, args[3]);
										}
										catch(IllegalArgumentException illArgeEx){
											out.println(String.format("Unkonw file type identifier \"%s\".", args[3]));
										}
									}
									else{
										instance.currentFile.changeFileTypeIdentifier(instance, instance.storageManager.getIdentifierByExtension(Tools.getFileExtension(args[1]), instance.currentFile.getFileTypeIdentifier()));
									}

									instance.currentFile.changeFilepath(new File(args[1]));	
									boolean saveStatus = saveFile();
									out.println(saveStatus ? "File saved." : "Error: File couldn't be saved.");
									return (saveStatus ? "ok" : "failed");
								}
								else{
									println("Usage: file saveAs <filepath> [file type identifier*]\n\n* Use 'file types' to get help.");
									return null;
								}
							}
							else{
								out.println("Cannot save file: There is no file currently opened.");
								return null;
							}

						case "close":
							if(currentFile != null){
								boolean closeStatus = closeFile();
								out.println(closeStatus ? "File closed." : "The action has been canceled by a plugin or the user...");
								return (closeStatus ? "ok" : "canceled");
							}
							else{
								out.println("Cannot close any file: There is no file currently opened.");
								return null;
							}

						case "info":
							if(currentFile != null){								
								out.println("Filepath:\t" + currentFile.getFilepath().getAbsolutePath());
								out.println("File type:\t" + currentFile.getFileTypeIdentifier());
								out.println("Format version:\t" + currentFile.getFileFormatVersion());
								out.println("Encryption:\t" + (currentFile.isEncrypted() ? currentFile.getEncryptionManager().getCipher().getCipherName() : "Disabled") + "\n");					
								return currentFile.getFilepath().getAbsolutePath();
							}
							else{
								out.println("No file opened.");
								return "";
							}
							
						case "types":
							out.println("Supported file types:");
							instance.storageManager.forEachFileType((str) -> out.print(str + " "));
							out.print("\n\nKnown file extensions:\n");
							instance.storageManager.forEachKnownExtension((extension, assignedType) -> out.println(String.format("%-16s\t%s", extension, assignedType)));
							return "";
					}
				}

				out.println("Invalid Arguments: Type in 'man file' to get more information.");
				return null;
			}}, "You can use the file command with the following arguments:\n\n" +
				"  file create		to create a new file.\n" +
				"  file info		to show the currently opened password file.\n" +
				"  file open 		to open a passowrd file.\n" +
				"  file save		to write all cghanges to your harddisk.\n" +
				"  file close		to close the currently opened password file.\n" +
				"  file types		to print a list of all supported file types.\n");
		
		provideNewCommand("module", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				if(args.length == 2)
				{
					switch(args[0].toLowerCase())
					{
						case "-info":
						case "-about":
							ModuleInfo m = allModules.get(args[1]);
							if(m != null)
							{
								if(m.properties != null)
								{
									out.println(String.format(	"Modulename: \t%s\n" +
																"Version: \t%s\n" +
																"Author: \t%s\n" +
																"Status: \t%s\n\n" +
																"Description:\n%s",
												m.properties.name(),
												m.properties.version().equals(".") ? APP_VERSION : m.properties.version(),
												m.properties.author(),
												(m.isEnabled ? (m.isStarted() ? "ENABLED" : "ENABLED (Startup error)" ): "DISABLED"),
												m.properties.description()));
								}
								else
								{
									out.println("No information for module '" + args[0] + "' available");
								}
							}
							else
							{
								out.println("Cannot finde module '" + args[1] + "'. Maybe you did not observe to match the module names case?");
							}
							break;

						case "-enable":
						case "--enable":
						case "/enable":
							try{
								enableModule(args[1]);
							}
							catch(IllegalArgumentException ex)
							{
								out.println("Cannot enable module: " + ex.getMessage());
							}
							break;

						case "-disable":
						case "--disable":
						case "/disable":
							try{
								disableModule(args[1]);
							}
							catch(IllegalArgumentException ex)
							{
								out.println("Cannot enable module: " + ex.getMessage());
							}
							break;
					}
				}
				else
				{
					out.println("Status\t\tModule name\n" +
								"------\t\t-----------");
					
					for(String name: allModules.keySet())
					{
						out.println(String.format("%s%s",
								(allModules.get(name).isEnabled ?
										(allModules.get(name).isStarted() ? "ENABLED\t\t" : "ENABLED (!)\t") :
										(allModules.get(name).isStarted() ? "ENABLED (-)\t" : "DISABLED\t")),
								name));
					}
					
					out.println("\n(!) This module is currently not started, this could happen if the module crashed on startup or if it has enabled during this session.\n" + 
								"(-) This module will be disabled at the next start.");

				}
				return null;
			}
		}, "Enables or disables a Krytonit module.\n\n" +
			"\tUsage: module [option] [modulename]\n\n" +
			"Possible options are: '-about', '-enable' or '-disable'\n" +
			"Its also possible to call 'module' without arguments to dislay a list of all modules.");

		provideNewCommand("favorites", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				if(getSettingsValueAsBoolean("nodes.disable_favorites", false)){out.println("Favorite nodes are disabled."); return null;}

				if(args.length == 0)
				{
					// Display all favorite nodes
					int cnt = 0;
					for(byte i = 0; i < favoriteNodes.length; i++)
					{
						if(favoriteNodes[i] > 0)
						{
							TreeNode node = getFavoriteNode(i);
							if(node != null){
								out.println(String.format("%d:\t%s", i, instance.tree.getNodePath(node, "/")));
								cnt++;
								continue;
							}
							else{
								favoriteNodes[i] = 0;
							}
						}
						out.println(String.format("%d:\t%s", i, "-"));
					}

					if(cnt == favoriteNodes.length){
						out.println(String.format("\nYou cannot define more favorite nodes. The limit of %d is reached.", favoriteNodes.length));
					}
					else{
						out.println(String.format("\nYou can define %d more favorite nodes.", favoriteNodes.length - cnt));
					}
				}
				else if(args.length == 1)
				{
					if(args[0].toLowerCase().equals("update") || args[0].toLowerCase().equals("rebuild"))
					{
						// Update favorite node list
						generateFavoriteNodeListFromTree();
						out.println("Favorite node list successfully updated.");
					}
					else{
						// Follow a favorite node link
						try{
							byte index = Byte.parseByte(args[0]);
							TreeNode n = getFavoriteNode(index);
							if(n != null){
								tree.setSelectedNode(n);
							}
							else{
								out.println("Cannot follow node link: Requested node does not exist.");
							}
						}
						catch(NumberFormatException numFormatEx)
						{
							out.println(String.format("Cannot parse \"%s\".", args[0]));
						}
					}
				}
				else if(args.length == 2 || args.length == 3)
				{
					// Add or remove favorite node
					TreeNode node = (args.length == 3 ? instance.getTree().getNodeByPath(args[2]) : instance.getTree().getSelectedNode());
					if(node != null){
						byte index;

						try {
							index = Byte.parseByte(args[1]);
						}
						catch(NumberFormatException numFormatEx){
							out.println(String.format("Cannot parse \"%s\".", args[0]));
							return null;
						}

						if(args[0].toLowerCase().equals("set")){
							out.print(String.format("Adding \"%s\" to favorite nodes... %s", node.getText(), setFavoriteNode(node, index, true) ? "done." : "failed!"));
						}
						else if(args[0].toLowerCase().equals("rm") || args[0].toLowerCase().equals("remove"))
						{
							removeFavoriteNode(index);
						}
					}
					else
					{
						out.println("Cannot find node" + (args.length == 3 ? ": \"" + args[2] + "\"" : "."));
					}
				}
				else{
					out.println("Syntax error. Use 'man favorites' for help.");
				}

				return null;
			}
		}, "Define your ten favorite nodes.\n\n" +
			"Usage:\n" +
			"\tTo display all defined favorite nodes:\n\t\tfavorites\n\n" +
			"\tTo set a node as favorite node:\n\t\tfavorites set <0-9> [tree node]\n\n" +
			"\tTo remove a node from the favorite list:\n\t\tfavorites rm <0-9>\n\n" +
			"\tTo update the list if you changed something manually:\n\t\tfavorites update ");

		provideCommandAlias("fav", "favorites");

		/*
		 * ================================================ settings ================================================
		 */
		provideNewCommand("config", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				if(args.length == 0){
					printHashMap(out, instance.settings);
				}
				else if(args.length == 1)
				{
					if(args[0].toLowerCase().equals("save")){
						instance.settingsHasBeenUpdated();
					}
					else if(args[0].toLowerCase().equals("reload")){
						settings.clear();
						presetDefaultSettings();
						loadSettingsFromXMLFile();
						fireEvent(DefaultEvent.OnSettingsChanged);
					}
					else{
						out.println("Unkown parameter: " + args[0]);
					}
				}
				else if(args.length == 2 && (args[0].toLowerCase().equals("-d") || args[0].toLowerCase().equals("--d"))){

					// Remove a key from the settings hash
					if(instance.settings.containsKey(args[1])){
						instance.settings.remove(args[1]);
						fireEvent(DefaultEvent.OnSettingsChanged);
					}
					else{
						out.println(String.format("Settings key '%s' does not exist.", args[1]));
					}
				}
				else if(args.length == 2 || (args.length == 3 && (args[0].toLowerCase().equals("-s") || args[0].toLowerCase().equals("--s"))))
				{	
					int index = (args.length == 3) ? 1 : 0; // If there is a "-s" then the index is "1", not "0"
					if(args[index].matches("[a-zA-Z0-9[_][:]\\.]*")){
						instance.setSettingsValue(args[index], args[index + 1]);

						if(args.length == 3){instance.settingsHasBeenUpdated();}else{fireEvent(DefaultEvent.OnSettingsChanged);}
					}
					else{
						out.println("Invalid key characters. Valid characters are only \"A-Z\", \"a-z\", \"0-9\", \".\", \"_\" and \":\".");
					}
				}
				else{
					out.println("Syntax error. Use 'man config' for help.");
				}

				return null;
			}}, "View or modify the application settings\n\n" +
				"Usage:\n" +
				"\tDisplay all settings:\n\t\tconfig\n\n" +
				"\tChange a settings value\n\t\tconfig <key> <value>\n\n" +
				"\tRemove a setting:\n\t\tconfig -d <key>\n\n" +
				"\tSave all changed settings to your harddisk\n\t\tconfig save\n\n" +
				"\tChange a settings value and directly save it\n\t\tconfig -s <key> <value>\n\n" +
				"\tReload the configuration from the settings file\n\t\tconfig reload");
		
		provideNewCommand("fileconfig", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				if(currentFile == null){out.println("No file opened!"); return null;}
				if(args.length == 0){
					printHashMap(out, instance.currentFile.fileSettings);
				}
				else if(args.length == 2 && (args[0].toLowerCase().equals("-d") || args[0].toLowerCase().equals("--d"))){
					
					// Remove a key from the settings hash				
					if(instance.currentFile.fileSettings.containsKey(args[1])){
						instance.currentFile.fileSettings.remove(args[1]);
						fireEvent(DefaultEvent.OnFileSettingsChanged);
					}
					else{
						out.println(String.format("File settings key '%s' does not exist.", args[1]));
					}
				}
				else if(args.length == 2 || (args.length == 3 && (args[0].toLowerCase().equals("-s") || args[0].toLowerCase().equals("--s"))))
				{	
					int index = (args.length == 3) ? 1 : 0; // If there is a "-s" then the index is "1", not "0"
					if(args[index].matches("[a-zA-Z0-9[_][:]\\.]*")){
						instance.setFileSettingsValue(args[index], args[index + 1]);

						if(args.length == 3){instance.fileSettingsHasBeenUpdated();}else{fireEvent(DefaultEvent.OnFileSettingsChanged);}
					}
					else{
						out.println("Invalid key characters. Valid characters are only \"A-Z\", \"a-z\", \"0-9\", \".\", \"_\" and \":\".");
					}
				}
				else{
					out.println("Syntax error. Use 'man fileconfig' for help.");
				}

				return null;
			}}, "View or modify the file settings\n" +
				"WARNING: You should be VERY CAREFUL if you want do change any value!\n\n" +
				"Usage:\n" +
				"\tDisplay all file settings:\n\t\tfileconfig\n\n" +
				"\tChange a file setting value\n\t\tfileconfig <key> <value>\n\n" +
				"\tRemove a file setting:\n\t\tconfig -d <key>\n\n" +
				"\tChange a file settings value and directly save your file\n\t\tconfig -s <key> <value>\n\n");
	}
	
	private static void printHashMap(CommandOutputProvider out, Map<String, String> hash){
		Tools.asSortedList(hash.keySet()).forEach((String key) -> {
			String value = hash.get(key);
			if(value.contains("\n")){
				out.println(String.format("%s = ", key));
				for(String str: value.split("\n")){
					out.println(String.format("    %s", str));
				}
			}
			else{
				String lCaseKey = key.toLowerCase();
				if(lCaseKey.contains("password") || lCaseKey.contains("pw") || lCaseKey.contains("paswd")){
					out.println(String.format("%s = *****", key));
				}
				else{
					out.println(String.format("%s = %s", key, value));
				}
			}
		});
	}

	/*
	 * ========================================================================================================================================================
	 * Interface CommandOutputProvider
	 * ========================================================================================================================================================
	 */	
	@Override
	public void print(String text) {
		if(outputRedirect == null)
		{
			System.out.print(text);
		}
		else
		{
			outputRedirect.print(text);
		}
	}

	@Override
	public void println(String text) {
		if(outputRedirect == null)
		{
			System.out.println(text);
		}
		else
		{
			outputRedirect.println(text);
		}
	}

	@Override
	public void printf(String text, Object... args){
		this.print(String.format(text, args));
	}
	
	public void tryToEstablishOutputRedirect(CommandOutputProvider redirectTo){
		if(!Launcher.environment.containsKey("disable_output_redirect")){
			if(redirectTo != this && outputRedirect == null){outputRedirect = redirectTo;}
		}
	}

	public void terminateOutputRedirect(CommandOutputProvider currentOwner){
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
	public void terminate()
	{
		if(closeFile())
		{
			fireEvent(DefaultEvent.OnExit);
			System.exit(0);
		}
	}

	/*
	 * ========================================================================================================================================================
	 * Module Information Class
	 * ========================================================================================================================================================
	 */
	public class ModuleInfo
	{
		public ModuleInfo(Module instance, ModuleProperties moduleProperties, boolean isEnabled)
		{
			this.moduleInstance = instance;
			this.isEnabled = isEnabled;
			this.properties = moduleProperties;
			this.moduleIsStarted = false;
		}
		public final Module moduleInstance;
		public boolean isEnabled;
		public final ModuleProperties properties;
		private boolean moduleIsStarted;
		
		public boolean isStarted(){return moduleIsStarted;}
		public void setStarted(){this.moduleIsStarted = true;}
	}
}