/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	SettingsDialog.java

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
package de.akubix.keyminder.ui.fx.dialogs;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.plugins.PluginInfo;
import de.akubix.keyminder.core.plugins.PluginLoader;
import de.akubix.keyminder.ui.fx.JavaFxUserInterface;
import de.akubix.keyminder.ui.fx.JavaFxUserInterfaceApi;
import de.akubix.keyminder.ui.fx.events.FxSettingsEvent;
import de.akubix.keyminder.ui.fx.utils.FxCommons;
import de.akubix.keyminder.ui.fx.utils.ImageMap;
import de.akubix.keyminder.ui.fx.utils.StylesheetMap;
import de.akubix.keyminder.util.Utilities;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SettingsDialog {

	public static final double size_x = 450;
	public static final double size_y = 450;

	private Stage me;
	private final ApplicationInstance app;
	private JavaFxUserInterfaceApi fxUI;
	private Map<String, String> settingscopy;

	private Map<String, CheckBox> pluginList = new HashMap<>();

	public SettingsDialog(Stage primaryStage, ApplicationInstance instance) throws IllegalStateException {
		this.app = instance;
		this.fxUI = JavaFxUserInterface.getInstance(instance);
		this.settingscopy = new HashMap<>();
		Utilities.hashCopy(app.getSettingsMap(), settingscopy);

		me = new Stage();
		me.setTitle(ApplicationInstance.APP_NAME + " - " + fxUI.getLocaleBundleString("settings.title"));
		me.initOwner(primaryStage);
	}

	private boolean saveSettings = false;


	public boolean show(){
		BorderPane root = new BorderPane();

		TabPane tabs = new TabPane();

		// General Settings
		tabs.getTabs().addAll(createGeneralSettingsTab(), createPluginSettingsTab());

		fireSettingsDialogOpenedEvent(tabs);

		root.setCenter(tabs);

		HBox bottom = new HBox(4);

		Button ok = new Button(fxUI.getLocaleBundleString("settings.button_save"));
		ok.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {

				Utilities.hashCopy(settingscopy, app.getSettingsMap());

				//Check if the user enabled or disabled some plugins
				PluginLoader pluginLoader =  app.getPluginLoader();
				for(String pluginName: pluginList.keySet()) {
					PluginInfo m = pluginLoader.getPluginInfo(pluginName);
					if(m.isEnabled() != pluginList.get(pluginName).isSelected()) {
						if(pluginList.get(pluginName).isSelected()) {
							pluginLoader.enablePlugin(pluginName);
						}
						else {
							pluginLoader.disablePlugin(pluginName);
						}
					}
				}

				app.saveSettings();

				saveSettings = true;
				me.close();
			}
		});

		Button cancel = new Button(fxUI.getLocaleBundleString("cancel"));
		cancel.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				saveSettings = false;
				me.close();
			}
		});

		ok.setMinWidth(120);
		cancel.setMinWidth(120);

		bottom.setAlignment(Pos.CENTER_RIGHT);
		bottom.getChildren().addAll(ok, cancel);

		cancel.setCancelButton(true);
		ok.setDefaultButton(true);

		root.setBottom(bottom);
		BorderPane.setMargin(bottom, new Insets(8,8,8,8));

		Scene myScene = new Scene(root, size_x, size_y);
		StylesheetMap.assignStylesheets(myScene);

		me.setScene(myScene);

		//Set position of second window, related to primary window.
		me.setResizable(false);
		me.initModality( Modality.APPLICATION_MODAL );
		ImageMap.addDefaultIconsToStage(me);
		me.showAndWait();

		return saveSettings;
	}

	@SuppressWarnings("unchecked")
	private void fireSettingsDialogOpenedEvent(TabPane tabs){
		app.getEventHandler(FxSettingsEvent.OnSettingsDialogOpened.toString()).forEach((consumer) -> {
			((BiConsumer<TabPane, Map<String, String>>) consumer).accept(tabs, settingscopy);
		});
	}

	private Separator createSeperator(){
		Separator s = new Separator(Orientation.HORIZONTAL);
		s.setStyle("-fx-padding: 8 0 8 0");
		return s;
	}

	private Tab createGeneralSettingsTab(){
		Tab settings_general = new Tab(fxUI.getLocaleBundleString("settings.tabs.general.title"));
		settings_general.setClosable(false);

		VBox vbox = new VBox(4);
		vbox.setPadding(new Insets(4, 8, 0, 8));
		vbox.setStyle("-fx-min-width: " + size_x + "; -fx-max-width: -fx-min-width");
		Label title = new Label(fxUI.getLocaleBundleString("settings.tabs.general.headline"));
		title.getStyleClass().add("h2");

		TextField defaultFile = new TextField("");
		if(settingscopy.containsKey(ApplicationInstance.SETTINGS_KEY_DEFAULT_FILE))
		{
			defaultFile.setText(settingscopy.get(ApplicationInstance.SETTINGS_KEY_DEFAULT_FILE));
		}

		defaultFile.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				settingscopy.put(ApplicationInstance.SETTINGS_KEY_DEFAULT_FILE, defaultFile.getText());
			}});

		final CheckBox windowTitleFilename = new CheckBox(fxUI.getLocaleBundleString("settings.general.show_current_file_in_window_title"));
		windowTitleFilename.setSelected(app.getSettingsValueAsBoolean("windowtitle.showfilename", true));
		windowTitleFilename.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
				settingscopy.put("windowtitle.showfilename", (newValue == true ? "yes" : "no"));
			}
		});

		final CheckBox windowTitleVersion = new CheckBox(fxUI.getLocaleBundleString("settings.general.show_version_in_window_title"));
		windowTitleVersion.setSelected(app.getSettingsValueAsBoolean("windowtitle.showversion", false));
		windowTitleVersion.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
				settingscopy.put("windowtitle.showversion", (newValue == true ? "yes" : "no"));
			}
		});
		final CheckBox useQuicklinks = new CheckBox(fxUI.getLocaleBundleString("settings.general.enable_quicklinks"));
		useQuicklinks.setSelected(app.getSettingsValueAsBoolean("nodes.disable_quicklinks", true));
		useQuicklinks.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
				settingscopy.put("nodes.disable_quicklinks", (newValue == true ? "no" : "yes"));
			}
		});

		final CheckBox hideEmptySidebar = new CheckBox(fxUI.getLocaleBundleString("settings.general.hide_sidebar_if_empty"));
		hideEmptySidebar.setSelected(app.getSettingsValueAsBoolean("sidebar.autohide", true));
		hideEmptySidebar.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
				if(newValue){settingscopy.remove("sidebar.autohide");}else{settingscopy.put("sidebar.autohide", "yes");}
			}
		});

		final TextField webBrowserPathTextField = new TextField(app.getSettingsValue(ApplicationInstance.SETTINGS_KEY_BROWSER_PATH));
		webBrowserPathTextField.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				String value = webBrowserPathTextField.getText();
				if(!value.equals("")){
					settingscopy.put(ApplicationInstance.SETTINGS_KEY_BROWSER_PATH, webBrowserPathTextField.getText());
				}
				else{
					settingscopy.remove(ApplicationInstance.SETTINGS_KEY_BROWSER_PATH);
				}
			}});

		Parent browserFileInputField = FxCommons.createFxFileInputField(webBrowserPathTextField, fxUI);

		final CheckBox useOtherWebBrowserCheckBox = new CheckBox(fxUI.getLocaleBundleString("settings.general.label_otherwebbrowser"));
		boolean useOtherBrowserValue = app.getSettingsValueAsBoolean(ApplicationInstance.SETTINGS_KEY_USE_OTHER_WEB_BROWSER, false);
		useOtherWebBrowserCheckBox.setSelected(useOtherBrowserValue);
		browserFileInputField.setDisable(!useOtherBrowserValue);

		useOtherWebBrowserCheckBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> ov, Boolean oldValue, Boolean newValue) {
				if(newValue){settingscopy.put(ApplicationInstance.SETTINGS_KEY_USE_OTHER_WEB_BROWSER, "yes");}else{settingscopy.remove(ApplicationInstance.SETTINGS_KEY_USE_OTHER_WEB_BROWSER);}
				browserFileInputField.setDisable(!newValue);
			}
		});

		vbox.getChildren().addAll(title, new Label(fxUI.getLocaleBundleString("settings.general.label_defaultfile")), FxCommons.createFxFileInputField(defaultFile, fxUI),
								  createSeperator(), windowTitleFilename, windowTitleVersion, useQuicklinks, hideEmptySidebar,
								  createSeperator(), useOtherWebBrowserCheckBox, browserFileInputField);

		ScrollPane scrollPane = new ScrollPane(vbox);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

		settings_general.setContent(scrollPane);

		return settings_general;
	}

	private Tab createPluginSettingsTab(){
		Tab pluginSettingsTab = new Tab(fxUI.getLocaleBundleString("settings.tabs.plugins.title"));
		pluginSettingsTab.setClosable(false);
		BorderPane pane = new BorderPane();
		pane.setPadding(new Insets(4, 8, 0, 8));

		Label title = new Label(fxUI.getLocaleBundleString("settings.tabs.plugins.headline"));
		title.getStyleClass().add("h2");

		VBox vbox = new VBox(4);
		vbox.getChildren().addAll(title, new Label(fxUI.getLocaleBundleString("settings.plugins.infolabel")));
		pane.setTop(vbox);

		VBox list = new VBox(6);
		list.setPadding(new Insets(4));
		PluginLoader pluginLoader = app.getPluginLoader();
		for(String pluginName: Utilities.asSortedList(pluginLoader.getPlugins())){
			PluginInfo pluginInfo = pluginLoader.getPluginInfo(pluginName);
			final CheckBox cb = new CheckBox(pluginName);
			try{
				Properties properties = pluginInfo.getProperties();
				cb.setTooltip(
						new Tooltip(
							String.format(
								fxUI.getLocaleBundleString("settings.plugins.plugininfo_author") + ": %s\n" +
								fxUI.getLocaleBundleString("settings.plugins.plugininfo_version") + ": %s\n\n%s",
								properties.getOrDefault("author", "-"),
								properties.getOrDefault("version", "-"),
								forceLineBreak(properties.getOrDefault("description_" + app.getLocale().getLanguage(),	properties.getOrDefault("description", "-")).toString(), 80)
				)));
			}
			catch(IOException | NullPointerException ex){
				cb.setTooltip(new Tooltip("Error loading plugin property file."));
			}

			if(pluginInfo.isEnabled() && !pluginInfo.isStarted()){cb.setText(cb.getText() + " (!)");}
			if(!pluginInfo.isEnabled() && pluginInfo.isStarted()){cb.setText(cb.getText() + " (*)");}

			cb.setSelected(pluginInfo.isEnabled());
			cb.setMaxWidth(size_x - 18);
			cb.setMinWidth(size_x - 18);
			pluginList.put(pluginName, cb);
			list.getChildren().addAll(cb, new Separator(Orientation.HORIZONTAL));
		}

		ScrollPane scrollPane = new ScrollPane(list);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

		scrollPane.setMaxWidth(size_x - 6);
		scrollPane.setMinWidth(size_x - 6);

		pane.setCenter(scrollPane);
		Label l = new Label(fxUI.getLocaleBundleString("settings.plugins.restart_hint"));
		l.setStyle("-fx-font-size: 10px; -fx-padding: 2px 0px 1px 1px;");
		pane.setBottom(l);
		pluginSettingsTab.setContent(pane);

		return pluginSettingsTab;
	}

	private static String forceLineBreak(String src, int maxCharacterPerLine){
		// Code from: http://stackoverflow.com/questions/1033563/java-inserting-a-new-line-at-the-next-space-after-30-characters
		StringBuilder sb = new StringBuilder(src);
		int i = 0;
		while ((i = sb.indexOf(" ", i + maxCharacterPerLine)) != -1) {
		    sb.replace(i, i + 1, "\n");
		}
		return sb.toString();
	}
}
