/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	FileSettingsDialog.java

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

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.encryption.EncryptionManager;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.ui.fx.JavaFxUserInterface;
import de.akubix.keyminder.ui.fx.events.FxSettingsEvent;
import de.akubix.keyminder.ui.fx.utils.ImageMap;
import de.akubix.keyminder.ui.fx.utils.StylesheetMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class FileSettingsDialog {

	public static final double size_x = 480;
	public static final double size_y = 520;

	private Stage me;
	private ApplicationInstance app;
	private de.akubix.keyminder.ui.fx.JavaFxUserInterfaceApi fxUI;
	private Map<String, String> fileSettingsCopy;

	private Map<String, String> originalFileSettingsReference;
	boolean saveSettings = false;

	public FileSettingsDialog(Stage primaryStage, ApplicationInstance instance){

		this.app = instance;
		this.fxUI = JavaFxUserInterface.getInstance(instance);
		this.originalFileSettingsReference = app.getCurrentFile().getFileSettings();
		this.fileSettingsCopy = new HashMap<>();

		de.akubix.keyminder.lib.Tools.hashCopy(app.getCurrentFile().getFileSettings(), fileSettingsCopy);

		me = new Stage();
		me.setTitle(ApplicationInstance.APP_NAME + " - " + fxUI.getLocaleBundleString("filesettings.title"));
		me.initOwner(primaryStage);
	}

	/**
	 * This mehtod shows the FileSettingsDialig an will return if the has saved the settings or not.
	 * Important: ALL EVENTS WILL BE FIRED AUTOMATICALLY
	 * @return TRUE if the user has saved the new changes, FALSE if not
	 */
	public boolean show(){
		BorderPane root = new BorderPane();
		TabPane tabs = new TabPane();

		// Allgemeine Einstellungen
		if(!app.isAnyFileOpened()){return false;}

		tabs.getTabs().add(createSecuritySettingsTab());

		fireFileSettingsDialogOpenedEvent(tabs);
		root.setCenter(tabs);

		HBox bottom = new HBox(4);

		Button ok = new Button(fxUI.getLocaleBundleString("filesettings.button_save"));
		ok.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {

				de.akubix.keyminder.lib.Tools.hashCopy(fileSettingsCopy, originalFileSettingsReference);

				app.fileSettingsHasBeenUpdated();

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
		bottom.getChildren().add(ok);
		bottom.getChildren().add(cancel);

		cancel.setCancelButton(true);
		ok.setDefaultButton(true);

		root.setBottom(bottom);
		BorderPane.setMargin(bottom, new Insets(8,8,8,8));

		Scene myScene = new Scene(root, size_x, size_y);
		StylesheetMap.assignStylesheets(myScene);

		me.setScene(myScene);

		//Set position of second window, related to primary window.
		me.setResizable(false);
		me.initModality(Modality.APPLICATION_MODAL);
		ImageMap.addDefaultIconsToStage(me);
		me.showAndWait();

		return saveSettings;
	}

	@SuppressWarnings("unchecked")
	private void fireFileSettingsDialogOpenedEvent(TabPane tabs){
		app.getEventHandler(FxSettingsEvent.OnFileSettingsDialogOpened.toString()).forEach((consumer) -> {
			((BiConsumer<TabPane, Map<String, String>>) consumer).accept(tabs, fileSettingsCopy);
		});
	}

	private Tab createSecuritySettingsTab(){
		Tab settings_security = new Tab(fxUI.getLocaleBundleString("filesettings.tabs.security.title"));
		settings_security.setClosable(false);

		Button setEncryptFile = new Button(fxUI.getLocaleBundleString("filesettings.security.button_enable_encryption"));
		Button setNotEncryptFile = new Button(fxUI.getLocaleBundleString("filesettings.security.button_disable_encryption"));

		final Separator passwordHintSeparator = new Separator(Orientation.HORIZONTAL);
		final Label passwordHintLabel = new Label(fxUI.getLocaleBundleString("filesettings.security.label_password_hint"));
		final TextField passwordHintTextField = new TextField(app.getCurrentFile().getPasswordHint());

		passwordHintTextField.setOnKeyReleased((event) -> {
				app.getCurrentFile().setPasswordHint(passwordHintTextField.getText());
				app.getTree().setTreeChangedStatus(true);
			});

		// select encryption cipher

		final Separator cipherSeparator = new Separator(Orientation.HORIZONTAL);
		final Label cipherLabel = new Label(fxUI.getLocaleBundleString("filesettings.security.label_cipher_selection"));
		final ComboBox<String> cipherSelection = new ComboBox<>();
		for(String cipherName: EncryptionManager.getCipherAlgorithms()){
			if(!cipherName.equals("None")){
				cipherSelection.getItems().add(cipherName);
			}
		}
		cipherSelection.setMaxWidth(Double.MAX_VALUE);

		cipherSelection.setOnAction((event) -> {
				try {
					app.getCurrentFile().getEncryptionManager().setCipher(cipherSelection.getSelectionModel().getSelectedItem());
				} catch (NoSuchAlgorithmException e) {
					app.alert("ERROR: Cannot change encryption algorithm. The cipher does not exist."); // Should never occur.
				}
			});

		// info label

		final Separator infoLabelSeparator = new Separator(Orientation.HORIZONTAL);
		final Label infoLabel = new Label(fxUI.getLocaleBundleString("filesettings.security.infolabel"));
		infoLabel.setWrapText(true);
		infoLabel.getStyleClass().add("borderedArea");

		Consumer<Boolean> changeElementVisibility = (value) -> {
			passwordHintSeparator.setVisible(value);
			passwordHintLabel.setVisible(value);
			passwordHintTextField.setVisible(value);
			cipherSelection.setVisible(value);
			cipherSeparator.setVisible(value);
			cipherLabel.setVisible(value);
			infoLabelSeparator.setVisible(value);
		};

		changeElementVisibility.accept(app.getCurrentFile().isEncrypted());

		if(app.getCurrentFile().isEncrypted()){
			setEncryptFile.setText(fxUI.getLocaleBundleString("filesettings.security.button_changepassword"));
			cipherSelection.getSelectionModel().select(app.getCurrentFile().getEncryptionManager().getCipher().getCipherName());
		}
		else{
			setNotEncryptFile.setDisable(true);
		}

		setEncryptFile.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
					boolean wasEncrypted = app.getCurrentFile().isEncrypted();
					boolean enable = !wasEncrypted;

					if(!enable){
						try {
							String currentPw = InputDialog.show(fxUI, fxUI.getLocaleBundleString("filesettings.security.dialog_changepassword.headline"), fxUI.getLocaleBundleString("filesettings.security.enter_current_password"), "", true);
							enable = app.getCurrentFile().getEncryptionManager().checkPassword(currentPw.toCharArray());
						} catch (UserCanceledOperationException e) {return;}
					}

					if(enable){
						if(app.getCurrentFile().getEncryptionManager() == null){app.getCurrentFile().encryptFile(new EncryptionManager(true));}
						try {
							if(app.getCurrentFile().getEncryptionManager().requestPasswordInputWithConfirm(app,
								fxUI.getLocaleBundleString("filesettings.security.button_changepassword"),
								fxUI.getLocaleBundleString("filesettings.security.dialog_changepassword.text"),
								fxUI.getLocaleBundleString("filesettings.security.dialog_changepassword.confirmtext")))
							{
								// New password has been set
								setNotEncryptFile.setDisable(false);
								setEncryptFile.setText(fxUI.getLocaleBundleString("filesettings.security.button_changepassword"));
								cipherSelection.getSelectionModel().select(app.getCurrentFile().getEncryptionManager().getCipher().getCipherName());
								app.saveFile();
							}
							else{
								// Operation canceled
								if(!wasEncrypted){
									// Undo everything -> delete the created encryption manager
									app.getCurrentFile().disableEncryption();
								}
								fxUI.alert(AlertType.INFORMATION, ApplicationInstance.APP_NAME, fxUI.getLocaleBundleString("filesettings.security.wrong_password_notification_title"), fxUI.getLocaleBundleString("filesettings.security.dialog_changepassword.passwords_not_equal"));
							}
						} catch (UserCanceledOperationException e) {} // The user knows that he has canceled the operation.
					}
					else{
						fxUI.alert(AlertType.INFORMATION, ApplicationInstance.APP_NAME, fxUI.getLocaleBundleString("filesettings.security.wrong_password_notification_title"), fxUI.getLocaleBundleString("filesettings.security.wrong_password"));
					}
					changeElementVisibility.accept(app.getCurrentFile().isEncrypted());
				}
		});

		setNotEncryptFile.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {

				if(app.getCurrentFile().isEncrypted()){
					Alert alert = new Alert(AlertType.CONFIRMATION);
					alert.setTitle(ApplicationInstance.APP_NAME);
					alert.setHeaderText(fxUI.getLocaleBundleString("filesettings.security.confirm_disable_encryption.title"));
					alert.setContentText(fxUI.getLocaleBundleString("filesettings.security.confirm_disable_encryption.message"));

					ButtonType buttonYes = new ButtonType(fxUI.getLocaleBundleString("yes"), ButtonData.YES);
					ButtonType buttonNo = new ButtonType(fxUI.getLocaleBundleString("no"), ButtonData.NO);
					ButtonType buttonCancel = new ButtonType(fxUI.getLocaleBundleString("cancel"), ButtonData.CANCEL_CLOSE);

					alert.getButtonTypes().setAll(buttonYes, buttonNo, buttonCancel);

					Stage s = (Stage) alert.getDialogPane().getScene().getWindow();
					ImageMap.addDefaultIconsToStage(s);
					s.initOwner(me);
					StylesheetMap.assignDefaultStylesheet(s.getScene());

					if(alert.showAndWait().get() == buttonYes){
						try {
							String currentPw = InputDialog.show(fxUI, fxUI.getLocaleBundleString("filesettings.security.confirm_disable_encryption.title"), fxUI.getLocaleBundleString("filesettings.security.enter_current_password"), "", true);

							if(app.getCurrentFile().getEncryptionManager().checkPassword(currentPw.toCharArray())){
								app.getCurrentFile().disableEncryption();
								setNotEncryptFile.setDisable(true);
								setEncryptFile.setText(fxUI.getLocaleBundleString("filesettings.security.button_enable_encryption"));
							}
							else{
								fxUI.alert(AlertType.WARNING, ApplicationInstance.APP_NAME, fxUI.getLocaleBundleString("filesettings.security.wrong_password_notification_title"), fxUI.getLocaleBundleString("filesettings.security.wrong_password"));
							}
						} catch (UserCanceledOperationException e) {}
					}
					changeElementVisibility.accept(app.getCurrentFile().isEncrypted());
				}
			}
		});

		VBox vbox = new VBox(8);
		vbox.setPadding(new Insets(4, 8, 0, 8));
		Label title = new Label(fxUI.getLocaleBundleString("filesettings.tabs.security.headline"));
		title.getStyleClass().add("h2");

		HBox hbox = new HBox(8);
		hbox.setAlignment(Pos.TOP_CENTER);
		hbox.getChildren().addAll(setEncryptFile, setNotEncryptFile);
		vbox.getChildren().addAll(title, hbox, passwordHintSeparator, passwordHintLabel, passwordHintTextField,
								  cipherSeparator, cipherLabel, cipherSelection,
								  infoLabelSeparator, infoLabel);

		final Insets defaultMargin = new Insets(8,0,8,0);
		VBox.setMargin(passwordHintSeparator, defaultMargin);
		VBox.setMargin(cipherSeparator, defaultMargin);
		VBox.setMargin(infoLabelSeparator, defaultMargin);

		settings_security.setContent(vbox);
		return settings_security;
	}
}
