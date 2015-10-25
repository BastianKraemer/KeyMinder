/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	SSHTools.java

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
package de.akubix.keyminder.modules;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.Launcher;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.core.interfaces.Command;
import de.akubix.keyminder.core.interfaces.CommandOutputProvider;
import de.akubix.keyminder.core.interfaces.FxUserInterface;
import de.akubix.keyminder.core.interfaces.events.BooleanEventHandler;
import de.akubix.keyminder.core.interfaces.events.DefaultEventHandler;
import de.akubix.keyminder.core.interfaces.events.SettingsEventHandler;
import de.akubix.keyminder.core.interfaces.events.EventTypes.BooleanEvent;
import de.akubix.keyminder.core.interfaces.events.EventTypes.DefaultEvent;
import de.akubix.keyminder.core.interfaces.events.EventTypes.SettingsEvent;
import de.akubix.keyminder.lib.XMLCore;
import de.akubix.keyminder.lib.sidebar.FxSidebar;
import de.akubix.keyminder.modules.sshtools.AppStarter;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.PasswordField;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

@de.akubix.keyminder.core.interfaces.ModuleProperties(
	name="SSH-Tools",
	description = "Adds a sidebar to " + ApplicationInstance.APP_NAME + ", which allows you to store the configuration of a 'ssh connection' in a single node." +
				  "You also will be able to launch these configurations with \"PuTTY\" or another custom application by using the context menu.\n\nThis module is for advanced users.",		
	version = ".",
	dependencies = "KeyClip;Sidebar",
	author="Bastian Kraemer")
public class SSHTools implements de.akubix.keyminder.core.interfaces.Module {

	private ApplicationInstance app;
	private de.akubix.keyminder.core.interfaces.FxUserInterface fxUI = null;
	
	private String defaultUsername, defaultPassword;
	
	private List<String> socksProfileIDs = new ArrayList<String>();
	private Map<String, Process> runningSocksProfiles = new HashMap<>();
	private Map<String, CheckMenuItem> socksMenuItems = new HashMap<>();
	
	private Menu socksMenu;
	private List<AppStarter> appStarterList = new ArrayList<>();
	private AppStarter socksAppStarter = null;
	
	private static final String SETTINGS_KEY_SOCKS_ACTION = "sshtools.actionprofile_socks"; //Contains the path to the XML application profile for "Socks"
	private static final String SETTINGS_KEY_APP_PROFILES_PATH = "sshtools.app_profile_path";
	@Override
	public void onStartup(ApplicationInstance instance) throws de.akubix.keyminder.core.exceptions.ModuleStartupException {

		app = instance;

		if(Launcher.environment.get("os").equals("Linux") && !System.getProperty("user.name").equals("root")){
			instance.log("Warning (SSH-Tools): Privileged ports can only be forwarded by root. " + ApplicationInstance.APP_NAME + " won't be able to forward ports lower than 1024.");
		}
	
		if(!checkProfileSettings(SETTINGS_KEY_SOCKS_ACTION)){app.setSettingsValue(SETTINGS_KEY_SOCKS_ACTION, "default:putty_socks");}
		if(!checkProfileSettings(SETTINGS_KEY_APP_PROFILES_PATH)){app.setSettingsValue(SETTINGS_KEY_APP_PROFILES_PATH, "./sshtools");}
		
		provideSocksCommand();

		if(app.isFxUserInterfaceAvailable())
		{
			fxUI = app.getFxUserInterface();

			FxSidebar sidebar = new FxSidebar(app, fxUI.getLocaleBundleString("module.sshtools.tabtitle"), true, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					app.execute("keyclip", app.getTree().getSelectedNode().getAttribute("ssh_user"),
							 app.getTree().getSelectedNode().getAttribute("ssh_password"),
							 "yes");
				}}
			);
			sidebar.addLabel(fxUI.getLocaleBundleString("module.sshtools.ssh_host"));
			sidebar.addElementToSidebar(sidebar.createDefaultSidebarTextbox("ssh_host"), "ssh_host");
			
			sidebar.addLabel(fxUI.getLocaleBundleString("module.sshtools.ssh_port"));
			sidebar.addElementToSidebar(sidebar.createDefaultSidebarTextbox("ssh_port"), "ssh_port");
			
			sidebar.addLabel(fxUI.getLocaleBundleString("module.sshtools.ssh_portforwarding"));
			sidebar.addElementToSidebar(sidebar.createDefaultSidebarTextarea("ssh_portforwarding"), "ssh_portforwarding");
			sidebar.addSeperator();
			
			sidebar.addLabel(fxUI.getLocaleBundleString("module.sshtools.ssh_user"));
			sidebar.addElementToSidebar(sidebar.createDefaultSidebarTextbox("ssh_user"), "ssh_user");
			
			sidebar.addLabel(fxUI.getLocaleBundleString("module.sshtools.ssh_password"));
			sidebar.addElementToSidebar(sidebar.createDefaultSidebarPasswordbox("ssh_password"), "ssh_password");
			sidebar.addSeperator();
			sidebar.addElementToSidebar(sidebar.createDefaultSidebarCheckbox("ssh_x11", "Use X11"), "ssh_x11");
			
			// Menu to start new Socks Connections

			socksMenu = new Menu(fxUI.getLocaleBundleString("module.sshtools.menu_socks"));
			fxUI.addMenu(socksMenu, true);
				
			// Add etc hosts menu item

			if(Launcher.environment.containsKey("module.sshtools.etchosts")){
				fxUI.addMenuEntry(de.akubix.keyminder.lib.Tools.createFxMenuItem(fxUI.getLocaleBundleString("module.sshtools.open_etchosts"), "", (event) -> {
					try{
						String[] cmd = de.akubix.keyminder.core.ApplicationInstance.splitParameters(Launcher.environment.get("module.sshtools.etchosts"));

						File f = new File(cmd[0]);
						if(f.exists()){
							de.akubix.keyminder.lib.Tools.runProcess(java.util.Arrays.asList(cmd));
						}
						else{
							fxUI.alert(String.format("Cannot execute file '%s': File does not exist.", cmd[0]));	
						}
					}
					catch(Exception ex)
					{
						fxUI.alert("Cannot execute etc hosts command: " + ex.getMessage());
						if(de.akubix.keyminder.core.Launcher.verbose_mode){ex.printStackTrace();}
					}
				}), de.akubix.keyminder.core.etc.MenuEntryPosition.TOOLS, false);
			}
		}

		// add event handlers
		handleCreation();
		
		// create socks application starter
		createSocksAppStarter();
		app.addEventHandler(DefaultEvent.OnSettingsChanged, () -> createSocksAppStarter());
		
		// create default application starter
		loadAllApplicationStarter();
	}
	
	private void loadAllApplicationStarter(){

		loadDefaultApplicationStarter("PuTTY", "sshtools.enable_putty", "default:putty");
		loadDefaultApplicationStarter("WinSCP", "sshtools.enable_winscp", "default:winscp");

		File dir = new File(app.getSettingsValue(SETTINGS_KEY_APP_PROFILES_PATH));
		if(dir.exists()){
			File [] files = dir.listFiles(new java.io.FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith(".xml");
				}
			});

			for(File xmlFile : files){
				try{
					AppStarter as = new AppStarter(app, this, () -> {
						try {
							return XMLCore.loadDocumentFromFile(xmlFile);
						} catch (Exception e) {
							return null; 
						}
					});
					if(Launcher.verbose_mode){
						app.printf(" - Loading application profile from file '%s'...\n", xmlFile);
					}
					appStarterList.add(as);
				}
				catch(IllegalArgumentException ex){
					app.log(String.format("SSH-Tools: Cannot load AppStarter: %s\nXML-File: %s", ex.getMessage(), xmlFile.getAbsolutePath()));
				}
			}
		}
	}
	
	private void loadDefaultApplicationStarter(String name, String settings_value, String inputStreamSrc){
		try{
			if(app.getSettingsValueAsBoolean(settings_value, false)){
				AppStarter as = new AppStarter(app, this, () -> {try {
						return XMLCore.loadDocumentFromStream(getXMLProfileInputStream(inputStreamSrc));
					} catch (Exception e) {
						throw new IllegalArgumentException(String.format("Cannot parse XML-File: '%s'\n\n%s", app.getSettingsValue(SETTINGS_KEY_SOCKS_ACTION), e.getMessage()));
				}});
				appStarterList.add(as);
			}
		}
		catch(IllegalArgumentException e){
			// This will occur if a built-in profile has a syntax error
			String message = String.format("Warning: Syntax error in application profile '%s.", name);
			if(Launcher.verbose_mode){app.alert(message);}else{app.log(message);}
		}
	}

	private void createSocksAppStarter(){
		// create socks application starter
		if(app.settingsContainsKey(SETTINGS_KEY_SOCKS_ACTION)){
			try{
				socksAppStarter = new AppStarter(app, this, true, () -> {
					try {
						return XMLCore.loadDocumentFromStream(getXMLProfileInputStream(app.getSettingsValue(SETTINGS_KEY_SOCKS_ACTION)));
					} catch (Exception e) {
						throw new IllegalArgumentException(String.format("Cannot parse XML-File: '%s'\n\n%s", app.getSettingsValue(SETTINGS_KEY_SOCKS_ACTION), e.getMessage()));
				}});
			}
			catch(IllegalArgumentException e){
				String message = "Warning: Syntax error in socks application profile.";
				if(Launcher.verbose_mode){app.alert(message);}else{app.log(message);}
				socksAppStarter = null;
			}
		}
	}
	
	private boolean checkProfileSettings(String key)
	{
		if(app.settingsContainsKey(key))
		{
			if(!app.getSettingsValue(key).equals("")){return true;}	
		}
		
		return false;
	}

	private InputStream getXMLProfileInputStream(String path) throws FileNotFoundException{

		InputStream in;
		switch(path)
		{
			case "":
				throw new FileNotFoundException("Path for XML application profile is not defined.");

			case "default:putty":
				in = this.getClass().getResourceAsStream("sshtools/putty.xml");
				break;

			case "default:winscp":
				in = this.getClass().getResourceAsStream("sshtools/winscp.xml");
				break;

			case "default:putty_socks":
				in = this.getClass().getResourceAsStream("sshtools/putty_runsocksprofile.xml");
				break;
				
			case "default:plink_socks":
				in = this.getClass().getResourceAsStream("sshtools/plink_runsocksprofile.xml");
				break;

			default:
				File f = new File(path);
				if(f.exists()){
					return new FileInputStream(f);
				}
				else{
					throw new FileNotFoundException(String.format("File '%s' does not exist", path));
				}
		}

		if(in != null){
			return in;
		}
		else{
			throw new FileNotFoundException(String.format("Cannot find ressource '%s' in Jar-File.", path));
		}
	}
	
	private void handleCreation()
	{
		DefaultEventHandler handler = () -> reloadSocksConfig();

		// If a file has been opened
		app.addEventHandler(DefaultEvent.OnFileOpened, handler);
		
		// If the current file has been closed
		app.addEventHandler(DefaultEvent.OnFileClosed, handler);
		
		// If the file settings has been changed
		app.addEventHandler(DefaultEvent.OnFileSettingsChanged, handler);
		
		app.addEventHandler(BooleanEvent.DONTAllowFileClosing, new BooleanEventHandler() {
			@Override
			public boolean eventFired() {

				boolean ask4Close = false;

				for(String key: runningSocksProfiles.keySet())
				{
					Process p = runningSocksProfiles.get(key) ;
					if(p != null)
					{
						if(p.isAlive()){ask4Close = true;}
					}
				}

				if(ask4Close)
				{
					if(app.requestYesNoDialog(de.akubix.keyminder.core.ApplicationInstance.APP_NAME, fxUI.getLocaleBundleString("module.sshtools.terminate_all_connections")))
					{
						for(String key: runningSocksProfiles.keySet())
						{
							if(runningSocksProfiles.get(key) != null)
							{
								stopSocksProfile(key);
							}
						}
					}
					else
					{
						return true;
					}
				}
				return false;
			}});
		
		// This will only be executed if the JavaFX user interface is available
		if(fxUI != null)
		{
			app.addEventHandler(SettingsEvent.OnSettingsDialogOpened, new SettingsEventHandler() {
				@Override
				public void eventFired(TabPane tabControl, Map<String, String> settings) {
					onSettingsDialogOpened(tabControl, settings);
				}
			});

			app.addEventHandler(SettingsEvent.OnFileSettingsDialogOpened, new SettingsEventHandler() {
				@Override
				public void eventFired(TabPane tabControl, Map<String, String> settings) {
					onFileSettingsDialogOpened(tabControl, settings);
				}
			});
			
		}
	}
	
	private void provideSocksCommand()
	{
		app.provideNewCommand("socks", new Command() {
			@Override
			public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
				if(args.length == 2){
					if(args[0].toLowerCase().equals("start")){
						if(startSocksProfile(args[1]))
						{
							out.println("Socks-Profile started."); return "ok";
						} else {
							out.println("Start of Socks-Profile failed!"); return "failed";
						}
					}else if(args[0].toLowerCase().equals("stop")){
						if(socksProfileIDs.contains(args[1])){
							out.println("Stopping Socksprofile...");
							stopSocksProfile(args[1]);
							return "ok";
						} else {
							out.println("Socksprofile '" + args[1] + " does not exist.");
							return "Invalid profilename";
						}
					}
				} else if(args.length == 1)
				{
					if(args[0].toLowerCase().equals("status")){
						out.println("All available Socks-Profiles: (" + socksProfileIDs.size() + ")");
						for(String profile: socksProfileIDs)
						{
							out.println("	- " + profile);
						}

						out.println("\n\nCurrently running Socks-Profiles: (" + runningSocksProfiles.keySet().size() + ")");
						for(String profile: runningSocksProfiles.keySet())
						{
							out.println("	- " + profile);
						}
						return null;
					}
				}

				out.println("Usage: socks <start|stop> <socksprofile name>\n\tsocks status");

				return null;
			}}, "(Module SSH-Tools) Start a socks profile.\nUsage:\tsocks <start|stop> [socksprofile name]\n\tsocks status");
	}

	private void reloadSocksConfig()
	{
		socksProfileIDs.clear();
	
		defaultUsername = app.getFileSettingsValue("sshtools.defaultusername");
		defaultPassword = app.getFileSettingsValue("sshtools.defaultpassword");

		socksProfileIDs = loadSocksProfileIDsFromString(app.getFileSettingsValue("sshtools.socksprofiles"));

		if(fxUI != null)
		{
			socksMenu.getItems().clear();
			socksMenuItems.clear();
			
			if(socksProfileIDs.size() > 0)
			{
				appStarterList.forEach((as) -> as.clearSocksItems());
				for(String profileId: socksProfileIDs)
				{
					final String text2display = app.fileSettingsContainsKey("sshtools.socksprofile:" + profileId + ".name") ? app.getFileSettingsValue("sshtools.socksprofile:" + profileId + ".name") : profileId;
					appStarterList.forEach((as) -> as.createUsingSocksItem(profileId, text2display));

					CheckMenuItem socksMainMenuProfileItem = new CheckMenuItem(text2display);
				
					socksMainMenuProfileItem.setOnAction(new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent event) {
							if(!socksMainMenuProfileItem.isSelected()){
								stopSocksProfile(profileId);
							}
							else{
								startSocksProfile(profileId);
							}
						}
					});

					socksMenu.getItems().add(socksMainMenuProfileItem);
					socksMenuItems.put(profileId, socksMainMenuProfileItem);

					if(runningSocksProfiles.containsKey(profileId)){
						appStarterList.forEach((as) -> as.enableSocksItem(profileId, true));
						socksMainMenuProfileItem.setSelected(true);
					}
				}
			}
		}
	}

	private List<String> loadSocksProfileIDsFromString(String str)
	{
		List<String> list = new ArrayList<String>();
		if(!str.equals(""))
		{
			for(String item: str.split(","))
			{
				list.add(item);
			}
		}
		return list;
	}

	/* ================================== Code for starting applications like PuTTY or WinSCP via command line ================================== */

	public String startApplication(AppStarter appStarter, boolean ignorePortForwarding, String socksProfileId){
		return startApplication(appStarter, app.getTree().getSelectedNode(), ignorePortForwarding, socksProfileId);
	}
	
	public String startApplication(AppStarter appStarter, TreeNode node, boolean ignorePortForwarding, String socksProfileId)
	{
		Map<String, String> var = createVariablePreset(socksProfileId, node);
		if(ignorePortForwarding){var.put("ssh_portforwarding", "");} // Variables will be first looked up in the documents variable map, so the value of the node attribute wont be used 
		
		try
		{
			List<String> args = appStarter.getCommandLineArgs(var, (socksProfileId == null) ? null : "using_socks", node);

			if(confirmCommandlineArguments(args, node.hasAttribute("ssh_password") ? node.getAttribute("ssh_password") : null))
			{
				try {
					if(!new File(args.get(0)).exists()){
						if(fxUI == null){
							return "Cannot run process. File '" + args.get(0) + "' does not exist.";
						}
						else{
							return String.format(fxUI.getLocaleBundleString("module.sshtools.error.exec_not_found"), args.get(0));
						}
					}
					de.akubix.keyminder.lib.Tools.runProcess(args);
					return (fxUI == null ?
							"Process '" + args.get(0) + "' successfully started." :
							String.format(fxUI.getLocaleBundleString("module.sshtools.message.process_successfully_started"), args.get(0)));
				} catch (IOException e) {
					return (fxUI == null ?
							"Cannot start '" + args.get(0) + "': " + e.getMessage() :
							String.format(fxUI.getLocaleBundleString("module.sshtools.error.runapp_ioexecption"), args.get(0), e.getMessage()));
				}
			}
			else
			{
				return fxUI == null ? "Canceled by user..." : fxUI.getLocaleBundleString("module.sshtools.error.canceled_by_user");
			}
		}
		catch(IllegalArgumentException e){
			return e.getMessage();
		}
	}
	
	/* =============================================================================================================================================== */
	
	private Map<String, String> createVariablePreset(String socksProfileId, TreeNode treeNode){
		Map<String, String> variablePreset = new HashMap<>();
		
		//Socks profile variables
		if(socksProfileId != null && !socksProfileId.equals("")){
			variablePreset.put("socks_ssh_user", getSocksProfileValues(socksProfileId, "user", defaultUsername));
			variablePreset.put("socks_ssh_password", getSocksProfileValues(socksProfileId, "password", defaultPassword));
			variablePreset.put("socks_ssh_host", app.getFileSettingsValue("sshtools.socksprofile:" + socksProfileId + ".host"));
			variablePreset.put("socks_ssh_port", app.getFileSettingsValue("sshtools.socksprofile:" + socksProfileId + ".sshport"));
			variablePreset.put("socks_proxyport", app.getFileSettingsValue("sshtools.socksprofile:" + socksProfileId + ".socksport"));
			
			for(String line: app.getFileSettingsValue("sshtools.socksprofile:" + socksProfileId + ".customargs").split("\n")){
				if(!line.matches("^( |\t)*\\#.*")){
					String[] splitstr = line.split("=", 2);
					if(splitstr.length == 2){variablePreset.put(splitstr[0].trim(), splitstr[1].trim());}
				}
			}
		}
		
		// "Overwrite" the ${ssh_portforwarding} variable to exclude lines that begins with a '#'
		if(treeNode != null){
			String value = treeNode.getAttribute("ssh_portforwarding");
			if(!value.equals("") && value.contains("#")){
				StringBuilder strbuilder = new StringBuilder();
				for(String line: value.split("\n")){
					if(!line.matches("^( |\t)*\\#.*")){
						strbuilder.append(line);
						strbuilder.append("\n");
					}
				}
				// If a variable name is found in this map, then this value will be used. In this case something like the tree node attributes won't be checked.
				// Take a look at the 'getValueOfVariable()' method in the XMLApplicationProfileParser class */
				variablePreset.put("ssh_portforwarding", strbuilder.toString());
			}
		}
		
		return variablePreset;
	}
	
	/**
	 * Start a socks profile using the selected "XML application profile"
	 * @param socksProfileId
	 * @return
	 */
	private boolean startSocksProfile(String socksProfileId)
	{
		if(socksAppStarter == null){app.log("ERROR: Cannot start Profile - no application definied. Please update your settings."); return false;}

		if(!runningSocksProfiles.containsKey(socksProfileId) && socksProfileIDs.contains(socksProfileId))
		{			
			try	{
				String sshpw = getSocksProfileValues(socksProfileId, "password", defaultPassword);
				List<String> cmd = socksAppStarter.getCommandLineArgs(createVariablePreset(socksProfileId, null), null, app.getTree().getSelectedNode());

				if(confirmCommandlineArguments(cmd, sshpw.equals("") ? null : sshpw))
				{
					try
					{
						Process p = de.akubix.keyminder.lib.Tools.runProcess(cmd);
						runningSocksProfiles.put(socksProfileId, p);
						
						if(fxUI != null){
							appStarterList.forEach((as) -> as.enableSocksItem(socksProfileId, true));
							socksMenuItems.get(socksProfileId).setSelected(true);
							fxUI.updateStatus(String.format(fxUI.getLocaleBundleString("module.sshtools.message.process_successfully_started"), cmd.get(0)));
						}
						return true;
					
					} catch (IOException e) {
						app.log("ERROR: Start of socks profile caused IOException.\n" + e.getMessage());
						return false;
					}
				}
				else
				{
					if(fxUI != null){
						appStarterList.forEach((as) -> as.enableSocksItem(socksProfileId, false));
						socksMenuItems.get(socksProfileId).setSelected(false);
					}
				}
			}
			catch(IllegalArgumentException e){
				app.updateStatus(e.getMessage());
				app.log(e.getMessage());
				if(fxUI != null){
					socksMenuItems.get(socksProfileId).setSelected(false);
					appStarterList.forEach((as) -> as.enableSocksItem(socksProfileId, false));
				}
				return false;
			}
		}
		else
		{
			app.log("ERROR: Cannot start Profile. Reason(s):" +
								(runningSocksProfiles.containsKey(socksProfileId) ? "\n - The Socks-Profile is already running" : "") +
								(!socksProfileIDs.contains(socksProfileId) ? "\n - The Socks-Profile does not exist" : ""));
		}
		return false;
	}
	
	/**
	 * Stop a socks profile
	 * @param socksProfileID
	 */
	private void stopSocksProfile(String socksProfileID)
	{
		Process socksProcess = runningSocksProfiles.get(socksProfileID);
		boolean successfullyStopped = false;
		if(socksProcess != null)
		{
			if(socksProcess.isAlive())
			{
				socksProcess.destroy();
				if(socksProcess.isAlive())
				{
					try {
						socksProcess.waitFor(1000, TimeUnit.MILLISECONDS);
					} catch (InterruptedException e) {
						socksProcess.destroyForcibly();
					}
				}
			}
			
			successfullyStopped = true;
			runningSocksProfiles.remove(socksProfileID);
			fxUI.updateStatus(String.format(fxUI.getLocaleBundleString("module.sshtools.message.socks_process_terminated")));
		}
		else
		{
			successfullyStopped = true;
		}
		
		if(successfullyStopped && fxUI != null){
			socksMenuItems.get(socksProfileID).setSelected(false);
			appStarterList.forEach((as) -> as.enableSocksItem(socksProfileID, false));
		}
	}
	
	private String getSocksProfileValues(String profileID, String value, String defaultValue)
	{
		String val = app.getFileSettingsValue("sshtools.socksprofile:" + profileID + "." + value);
		if(val.equals(""))
		{
			return defaultValue;
		}
		else
		{
			return val;
		}
	}
	
	private boolean isEmpty(String str)
	{
		if(str == null || str.equals("")){return true;}
		return false;
	}
	
	// Important note: One entry of array 'radioButtonUserData' has to be ""
	private VBox createApplicationProfileSettingsArea(Map<String, String> generalSettings, String mapHashKey, String title, String[] radioButtonText, String[] radioButtonUserData)
	{
		if(radioButtonText.length != radioButtonUserData.length){return null;}

		final VBox container = new VBox();
		container.setPadding(new Insets(4, 8, 0, 8));
		final Label l = new Label(title);
		l.getStyleClass().add("h2");

		container.getChildren().add(l);

		final TextField customProfileTextfield = new TextField();
		final RadioButton customProfileRadioButton = new RadioButton();

		boolean initialSelectionDone = false;
		boolean disableTextField = true;
		final ToggleGroup toogleGroup = new ToggleGroup();
		for(int i = 0; i < radioButtonText.length; i++)
		{
			if(radioButtonUserData[i].equals("")) //should only occur one time
			{
				customProfileRadioButton.setText(radioButtonText[i]);
				customProfileRadioButton.setUserData(radioButtonUserData[i]);
				customProfileRadioButton.setToggleGroup(toogleGroup);

				if(!initialSelectionDone){
					customProfileRadioButton.setSelected(true);
					initialSelectionDone = true;
					customProfileTextfield.setText(app.getSettingsValue(mapHashKey));
					disableTextField = false;
				}

				HBox hbox = de.akubix.keyminder.lib.Tools.createFxFileInputField(customProfileTextfield, fxUI);
				hbox.setPadding(new Insets(2, 2, 2, 28));
				container.getChildren().addAll(customProfileRadioButton, hbox);
			}
			else
			{
				RadioButton r = new RadioButton(radioButtonText[i]);
				r.setUserData(radioButtonUserData[i]);
				r.setToggleGroup(toogleGroup);
				if(i == 0){r.setSelected(true);}

				if(app.getSettingsValue(mapHashKey).toLowerCase().equals(radioButtonUserData[i]))
				{
					r.setSelected(true);
					customProfileTextfield.setText("");
					disableTextField = true;
					initialSelectionDone = true;
				}

				container.getChildren().add(r);
			}
		}

		customProfileTextfield.setDisable(disableTextField);

		toogleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>(){
			public void changed(ObservableValue<? extends Toggle> ov, Toggle old_toggle, Toggle new_toggle) {
				if (toogleGroup.getSelectedToggle() != null) {
					String val = toogleGroup.getSelectedToggle().getUserData().toString();
					if(val.equals(""))
					{
						customProfileTextfield.setDisable(false);
						generalSettings.put(mapHashKey, customProfileTextfield.getText());
					}
					else
					{
						customProfileTextfield.setDisable(true);
						generalSettings.put(mapHashKey, val);
					}
				}
		 }});

		customProfileTextfield.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				generalSettings.put(mapHashKey, customProfileTextfield.getText());
				if(customProfileRadioButton != null){customProfileRadioButton.setSelected(true);}
				customProfileTextfield.setDisable(false);
			}});

		return container;
	}

	/**
	 * Append the general ssh-tools settings
	 * @param tabControl
	 * @param generalSettings
	 */
	private void onSettingsDialogOpened(TabPane tabControl, Map<String, String> generalSettings) {
	
		Tab mytab = new Tab(fxUI.getLocaleBundleString("module.sshtools.settings.tabtitle"));
		mytab.setClosable(false);

		Label title = new Label(fxUI.getLocaleBundleString("module.sshtools.settings.headline"));
		title.getStyleClass().add("h2");
		
		/* =========================== path configuration ====================================== */
		
		VBox pathConfig = new VBox(4);
		pathConfig.setPadding(new Insets(4, 8, 0, 8));
		pathConfig.setMaxWidth(de.akubix.keyminder.ui.fx.dialogs.SettingsDialog.size_x);
		pathConfig.setMinWidth(de.akubix.keyminder.ui.fx.dialogs.SettingsDialog.size_x);
				
		pathConfig.getChildren().addAll(title,
				new Label(fxUI.getLocaleBundleString("module.sshtools.settings.puttypath")), createFileInputDialog(generalSettings, "sshtools.puttypath", fxUI),
				new Label(fxUI.getLocaleBundleString("module.sshtools.settings.plinkpath")), createFileInputDialog(generalSettings, "sshtools.plinkpath", fxUI),
				new Label(fxUI.getLocaleBundleString("module.sshtools.settings.winscppath")), createFileInputDialog(generalSettings, "sshtools.winscppath", fxUI),
				new Separator(Orientation.HORIZONTAL));

		// Custom application profile path

		HBox appProfilesPathContainer = new HBox(4);
		TextField pathInput = new TextField(generalSettings.get(SETTINGS_KEY_APP_PROFILES_PATH));
		pathInput.setOnKeyReleased((event) -> generalSettings.put(SETTINGS_KEY_APP_PROFILES_PATH, pathInput.getText()));
		Button browseButton = new Button(fxUI.getLocaleBundleString("filebrowser.buttentext"));
		browseButton.setOnAction((event) -> {
				DirectoryChooser dc = new DirectoryChooser();
				dc.setTitle(fxUI.getLocaleBundleString("module.sshtools.settings.directorychooser.appprofilepath"));
				File f = new File(pathInput.getText());
				if(f.exists()){dc.setInitialDirectory(f);}
				
				File folder = dc.showDialog(tabControl.getParent().getScene().getWindow());		
				if(folder != null){
					pathInput.setText(folder.getAbsolutePath());
					pathInput.fireEvent(new KeyEvent(pathInput, null, KeyEvent.KEY_RELEASED, ".", "", KeyCode.ACCEPT, false, false, false, false)); // The text field listes to the "KeyEvent.KEY_RELEASED" event to recognize changes
				}
		});
		HBox.setHgrow(pathInput, Priority.ALWAYS);
		
		appProfilesPathContainer.getChildren().addAll(pathInput, browseButton);
		pathConfig.getChildren().addAll(new Label(fxUI.getLocaleBundleString("module.sshtools.settings.applicationprofilepath")), appProfilesPathContainer);

		final TitledPane pathConfiguration = new TitledPane(fxUI.getLocaleBundleString("module.sshtools.settings.part_pathconfig"), pathConfig);
		
		/* =========================== feature configuration ====================================== */

		Label featureHeadline = new Label(fxUI.getLocaleBundleString("module.sshtools.settings.features.headline"));
		featureHeadline.getStyleClass().add("h2");
		
		CheckBox enablePuTTY = createCheckBox(fxUI.getLocaleBundleString("module.sshtools.settings.features.enable_putty"), "sshtools.enable_putty", generalSettings, false);
		CheckBox enableWinSCP = createCheckBox(fxUI.getLocaleBundleString("module.sshtools.settings.features.enable_winscp"), "sshtools.enable_winscp", generalSettings, false);
		CheckBox showConfirmDialogCheckBox = createCheckBox(fxUI.getLocaleBundleString("module.sshtools.settings.always_confirm_cmdargs"), "sshtools.showConfirmDialog", generalSettings, true);
		
		VBox vbox = new VBox(4);
		Label info = new Label(fxUI.getLocaleBundleString("module.sshtools.settings.features.info"));
		info.setWrapText(true);
		
		info.setStyle("-fx-padding: 8; -fx-border-color: -fx-accent;");
		
		vbox.getChildren().addAll(featureHeadline, enablePuTTY, enableWinSCP, showConfirmDialogCheckBox, info);

		VBox.setMargin(enablePuTTY, new Insets(4, 0, 0, 16));
		VBox.setMargin(enableWinSCP, new Insets(0, 0, 0, 16));
		VBox.setMargin(showConfirmDialogCheckBox, new Insets(16, 0, 0, 0));
		VBox.setMargin(info, new Insets(16, 0, 0, 0));
		final TitledPane features = new TitledPane(fxUI.getLocaleBundleString("module.sshtools.settings.part_features"), vbox);

		/* =========================== socks configuration ====================================== */			

		final TitledPane socksConnections = new TitledPane(fxUI.getLocaleBundleString("module.sshtools.settings.part_socks"),
					createApplicationProfileSettingsArea(generalSettings, SETTINGS_KEY_SOCKS_ACTION,
														 fxUI.getLocaleBundleString("module.sshtools.settings.socks_action"),
														 new String[]{"PuTTY", "PuTTY plink", fxUI.getLocaleBundleString("module.sshtools.settings.custom_execution_profile")},
														 new String[]{"default:putty_socks", "default:plink_socks", ""}));
		
		/* =========================== build "Accordion" ====================================== */	
		
		final Accordion accordion = new Accordion();
		
		accordion.getPanes().addAll(features, pathConfiguration, socksConnections);
		accordion.setExpandedPane(features);
		
		mytab.setContent(accordion);
		
		tabControl.getTabs().add(mytab);
	}
	
	private CheckBox createCheckBox(String text, String settingsKey, Map<String, String> settingsReference, boolean defaultValue){
		CheckBox cb = new CheckBox(text);
		cb.setOnAction((event) -> settingsReference.put(settingsKey, Boolean.toString(cb.isSelected())));
		cb.setSelected(app.getSettingsValueAsBoolean(settingsKey, defaultValue));
		return cb;
	}
	
	/**
	 * Create a file input dialog that stores the value in the settings hash
	 * @param generalSettings
	 * @param settingsKey
	 * @param fxUI
	 * @return
	 */
	private javafx.scene.Node createFileInputDialog(Map<String, String> generalSettings, String settingsKey, FxUserInterface fxUI){
		TextField textField = new TextField(app.getSettingsValue(settingsKey));
		textField.addEventFilter(KeyEvent.KEY_RELEASED, (event) -> generalSettings.put(settingsKey, textField.getText()));
		
		return de.akubix.keyminder.lib.Tools.createFxFileInputField(textField, fxUI);
	}

	/**
	 * Append the file specific ssh-tools settings
	 * @param tabControl
	 * @param fileSettings
	 */
	private void onFileSettingsDialogOpened(TabPane tabControl, Map<String, String> fileSettings) {

			List<String> socksProfileIDs_clone = loadSocksProfileIDsFromString(app.getFileSettingsValue("sshtools.socksprofiles"));
			Tab mytab = new Tab(fxUI.getLocaleBundleString("module.sshtools.settings.tabtitle"));
			mytab.setClosable(false);

			VBox vbox = new VBox(4);
			vbox.setPadding(new Insets(4, 8, 0, 8));
			
			Label title = new Label(fxUI.getLocaleBundleString("module.sshtools.filesettings.headline"));
			title.getStyleClass().add("h2");
			
			ScrollPane scrollPane = new ScrollPane(vbox);
			scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
			scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
			
			vbox.setMaxWidth(de.akubix.keyminder.ui.fx.dialogs.FileSettingsDialog.size_x);
			vbox.setMinWidth(de.akubix.keyminder.ui.fx.dialogs.FileSettingsDialog.size_x);

			TextField defaultUserInput = new TextField();
			defaultUserInput.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent event) {
					fileSettings.put("sshtools.defaultusername", defaultUserInput.getText());
				}});

			defaultUserInput.setText(app.getFileSettingsValue("sshtools.defaultusername"));
			
			BorderPane defaultPwInput = de.akubix.keyminder.lib.Tools.createFxPasswordField(new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent event) {
					fileSettings.put("sshtools.defaultpassword", ((javafx.scene.control.TextInputControl) event.getSource()).getText());
				}}, app.getFileSettingsValue("sshtools.defaultpassword"), true, fxUI);

			HBox hbox = new HBox(4);

			ComboBox<String> profileSelection = new ComboBox<String>();
			profileSelection.setMaxWidth(Double.MAX_VALUE);

			if(socksProfileIDs_clone.size() > 0)
			{
				for(String profile: socksProfileIDs_clone)
				{
					profileSelection.getItems().add(profile);
				}
			}

			// Add socks profile button
			Button addProfile = new Button("+");
			addProfile.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					try{
						String input = fxUI.showInputDialog(fxUI.getLocaleBundleString("module.sshtools.addsocksprofile.title"),
															fxUI.getLocaleBundleString("module.sshtools.addsocksprofile.text"), "", false);

						if(!isEmpty(input)){
							if(input.matches("[a-zA-Z0-9[_]]*")){
								if(!socksProfileIDs_clone.contains(input)){
									socksProfileIDs_clone.add(input);
									profileSelection.getItems().add(input);

									if(profileSelection.getItems().size() == 1){
										profileSelection.getSelectionModel().select(0);
									}
									else{
										profileSelection.getSelectionModel().select(profileSelection.getItems().size() - 1);
									}
									fileSettings.put("sshtools.socksprofiles", socksProfiles2String(socksProfileIDs_clone));
									fileSettings.put("sshtools.socksprofile:" + profileSelection.getValue() + ".name", input);
									fileSettings.put("sshtools.socksprofile:" + profileSelection.getValue() + ".sshport", "22");
									fileSettings.put("sshtools.socksprofile:" + profileSelection.getValue() + ".customargs", "#putty_sessionname=?");
									profileSelection.getOnAction().handle(null);
									profileSelection.autosize();
								}
								else{
									app.alert(fxUI.getLocaleBundleString("module.sshtools.addsocksprofile.msg_name_already_in_use"));
								}
							}
							else{
								app.alert(fxUI.getLocaleBundleString("module.sshtools.addsocksprofile.msg_invalid_characters"));
							}
						}
					}
					catch(UserCanceledOperationException e){}
				}
			});

			Map<String, TextFieldAdapter> socksProfileTextfields = new HashMap<>();

			// Remove socks profile button
			Button removeProfile = new Button("-");
			removeProfile.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					
					if(profileSelection.getValue() != null)
					{
						if(app.requestYesNoDialog(fxUI.getLocaleBundleString("module.sshtools.removesocksprofile.title"),
																fxUI.getLocaleBundleString("module.sshtools.removesocksprofile.text"))){
							fileSettings.remove("sshtools.socksprofile:" + profileSelection.getValue() + ".name");
							fileSettings.remove("sshtools.socksprofile:" + profileSelection.getValue() + ".host");
							fileSettings.remove("sshtools.socksprofile:" + profileSelection.getValue() + ".sshport");
							fileSettings.remove("sshtools.socksprofile:" + profileSelection.getValue() + ".user");
							fileSettings.remove("sshtools.socksprofile:" + profileSelection.getValue() + ".password");
							fileSettings.remove("sshtools.socksprofile:" + profileSelection.getValue() + ".customargs");
							profileSelection.getItems().remove("sshtools.socksprofile:" + profileSelection.getSelectionModel().getSelectedIndex());

							for(String key: socksProfileTextfields.keySet())
							{
								socksProfileTextfields.get(key).setText("");
								socksProfileTextfields.get(key).setEditable(false);
							}

							socksProfileIDs_clone.remove(profileSelection.getValue());
							fileSettings.put("sshtools.socksprofiles", socksProfiles2String(socksProfileIDs_clone));

							profileSelection.getItems().clear();
							for(String profile: socksProfileIDs_clone)
							{
								profileSelection.getItems().add(profile);
							}

							if(profileSelection.getItems().size() >= 1){profileSelection.getSelectionModel().select(0);}
							profileSelection.autosize();
						}
					}	
				}});

			hbox.getChildren().addAll( profileSelection, addProfile, removeProfile);

			HBox.setHgrow(profileSelection, Priority.ALWAYS);

			VBox socksConfigGroupBoxContent = new VBox(4);
			socksConfigGroupBoxContent.getChildren().addAll(de.akubix.keyminder.lib.Tools.createFxLabelWithStyleClass(fxUI.getLocaleBundleString("module.sshtools.socksconfig.headline"), "h3"), hbox);

			// Define all text fields for a Socks-Profile
			
			socksProfileTextfields.put("name",		addSettingsDialogTextField(socksConfigGroupBoxContent, profileSelection, fxUI.getLocaleBundleString("module.sshtools.socksconfig.profilename"), fileSettings, ".name", false));

			HBox hostAndPortContainer = new HBox(4);
			socksProfileTextfields.put("host",		addSettingsDialogTextField(hostAndPortContainer, profileSelection, fxUI.getLocaleBundleString("module.sshtools.socksconfig.host"), fileSettings, ".host", false));
			socksProfileTextfields.put("sshport",	addSettingsDialogTextField(hostAndPortContainer, profileSelection, fxUI.getLocaleBundleString("module.sshtools.socksconfig.sshport"), fileSettings, ".sshport", false));
			HBox.setHgrow(hostAndPortContainer.getChildren().get(0), Priority.ALWAYS);
			socksConfigGroupBoxContent.getChildren().add(hostAndPortContainer);
			
			socksProfileTextfields.put("socksport",	addSettingsDialogTextField(socksConfigGroupBoxContent, profileSelection, fxUI.getLocaleBundleString("module.sshtools.socksconfig.socks_port_for_dynamic_forwarding"), fileSettings, ".socksport", false));
			
			HBox userAndPasswordContainer = new HBox(4);
			socksProfileTextfields.put("user",		addSettingsDialogTextField(userAndPasswordContainer, profileSelection, fxUI.getLocaleBundleString("module.sshtools.socksconfig.username"), fileSettings, ".user", false));
			socksProfileTextfields.put("password",	addSettingsDialogTextField(userAndPasswordContainer, profileSelection, fxUI.getLocaleBundleString("module.sshtools.socksconfig.password"), fileSettings, ".password", true));
			HBox.setHgrow(userAndPasswordContainer.getChildren().get(0), Priority.SOMETIMES);
			HBox.setHgrow(userAndPasswordContainer.getChildren().get(1), Priority.SOMETIMES);
			
			socksConfigGroupBoxContent.getChildren().add(new Separator(Orientation.HORIZONTAL));
			
			TextArea textarea = new TextArea();
			textarea.setMaxHeight(64);
			textarea.setOnKeyReleased((event) -> fileSettings.put("sshtools.socksprofile:" + profileSelection.getValue() + ".customargs", textarea.getText()));
			
			socksProfileTextfields.put("customargs", new TextFieldAdapter() {
				@Override
				public void setText(String text) {textarea.setText(text);}
				
				@Override
				public void setEditable(boolean value) {textarea.setEditable(value);}
				
				@Override
				public String getText() {return textarea.getText();}
			});
			socksConfigGroupBoxContent.getChildren().addAll(userAndPasswordContainer, new Label(fxUI.getLocaleBundleString("module.sshtools.socksconfig.customsocksargs")), textarea);
			
			// -----------------------------------

			VBox defaultParamsConfigBoxContent = new VBox(4);
			defaultParamsConfigBoxContent.getChildren().addAll(	de.akubix.keyminder.lib.Tools.createFxLabelWithStyleClass(fxUI.getLocaleBundleString("module.sshtools.filesettings.default_user"), "h3"),
																defaultUserInput,
																de.akubix.keyminder.lib.Tools.createFxLabelWithStyleClass(fxUI.getLocaleBundleString("module.sshtools.filesettings.default_password"), "h3"),
																defaultPwInput);

			// ...and append them to a TitledPane
			
			TitledPane socksConfigBox = new TitledPane(fxUI.getLocaleBundleString("module.sshtools.filesettings.part_socksconfig"), socksConfigGroupBoxContent);
			TitledPane defaultParamsConfigBox = new TitledPane(fxUI.getLocaleBundleString("module.sshtools.filesettings.part_default_values"), defaultParamsConfigBoxContent);
			defaultParamsConfigBox.setExpanded(false);

			vbox.getChildren().addAll(title, socksConfigBox, defaultParamsConfigBox);

			// Combo box event handling
			
			profileSelection.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if(profileSelection.getValue() != null)
					{
						for(String key: socksProfileTextfields.keySet())
						{
							socksProfileTextfields.get(key).setEditable(true);
							if(fileSettings.containsKey("sshtools.socksprofile:" + profileSelection.getValue() + "." + key))
							{
								socksProfileTextfields.get(key).setText(fileSettings.get("sshtools.socksprofile:" + profileSelection.getValue() + "." + key));
							}
							else
							{
								socksProfileTextfields.get(key).setText("");	
							}				
						}
					}
					else
					{
						for(String key: socksProfileTextfields.keySet())
						{
							socksProfileTextfields.get(key).setText("");
							socksProfileTextfields.get(key).setEditable(false);
						}
					}
				}
			});
			profileSelection.getSelectionModel().select(0);
			profileSelection.fireEvent(new ActionEvent());
			
			mytab.setContent(scrollPane);

			tabControl.getTabs().add(mytab);
	}

	// Some helper methods
	
	private TextFieldAdapter addSettingsDialogTextField(Pane container, ComboBox<String> profileSelection, String title, Map<String, String> settingsReference, String hashKey, boolean useAsPassowrdField)
	{
		VBox vbox = new VBox(2);
		if(title != null){
			vbox.getChildren().add(new Label(title));
		}

		TextFieldAdapter t = new PasswordFieldAdapter(vbox, (event) -> settingsReference.put("sshtools.socksprofile:" + profileSelection.getValue() + hashKey, ((TextInputControl) event.getSource()).getText()), useAsPassowrdField);
		t.setEditable(false);
		container.getChildren().add(vbox);
		return t;
	}
	
	private String socksProfiles2String(List<String> list)
	{
		if(list.size() == 0){return "";}
		StringBuilder ret = new StringBuilder(list.get(0));
		for(int i = 1; i < list.size(); i++)
		{
			ret.append("," + list.get(i));
		}
		
		return ret.toString();
	}

	private boolean confirmCommandlineArguments(List<String> args, String passwordString)
	{
		boolean confirmed = !app.getSettingsValueAsBoolean("sshtools.showConfirmDialog", true);
		StringBuilder sb = new StringBuilder();
		for(String part: args)
		{
			if(passwordString != null && part.contains(passwordString)){
				part = part.replace(passwordString, "*****");
			}

			if(part.contains(" ")){
				sb.append("\"" + part + "\" ");}else{sb.append(part + " ");
			}
		}
		
		if(!confirmed){
			if(fxUI == null){
				confirmed = app.requestYesNoDialog("Confirm command line arguments", "Please confirm these ommandline arguments:\n\n" + sb.toString());
			}
			else{
				confirmed = app.requestYesNoDialog(fxUI.getLocaleBundleString("module.sshtools.confirmargs.title"),
																 String.format(fxUI.getLocaleBundleString("module.sshtools.confirmargs.text"), sb.toString()));	
			}
		}
		return confirmed;
	}

	private interface TextFieldAdapter{
		public String getText();
		public void setText(String text);
		public void setEditable(boolean value);
	}

	private class PasswordFieldAdapter implements TextFieldAdapter
	{
		private TextField t1;
		private PasswordField t2;
		public PasswordFieldAdapter(Pane container, EventHandler<KeyEvent> onKeyReleased, boolean createPasswordField)
		{
				t1 = new TextField();
				if(createPasswordField)
				{
					t2 = new PasswordField();
					container.getChildren().add(de.akubix.keyminder.lib.Tools.createFxPasswordField(onKeyReleased, t1, t2, true, fxUI));
				}
				else
				{
					t1.addEventFilter(KeyEvent.KEY_RELEASED, onKeyReleased);
					container.getChildren().add(t1);
				}
		}

		public String getText(){return t1.getText();}

		public void setText(String text)
		{
			t1.setText(text);
			if(t2 != null){t2.setText(text);}
		}

		public void setEditable(boolean value)
		{
			t1.setEditable(value);
			if(t2 != null){t2.setEditable(value);}
		}
	}
}
