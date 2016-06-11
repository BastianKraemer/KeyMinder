/*	KeyMinder
	Copyright (C) 2016 Bastian Kraemer

	FxCommons.java

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
package de.akubix.keyminder.ui.fx.utils;

import de.akubix.keyminder.core.interfaces.FxUserInterface;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;

public class FxCommons {
	public static MenuItem createFxMenuItem(String text, String iconname, EventHandler<ActionEvent> event){
        MenuItem menuItem = new MenuItem(text);
        if(event != null){menuItem.setOnAction(event);}
        if(!iconname.equals("")){
        	menuItem.setGraphic(new ImageView(new Image(iconname)));
        }
        return menuItem;
	}

	public static HBox createFxFileInputField(TextField textfield, FxUserInterface fxUI){
		HBox hbox = new HBox(4);
		Button b = new Button(fxUI.getLocaleBundleString("filebrowser.buttentext"));
		b.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				java.io.File f = fxUI.showOpenFileDialog(de.akubix.keyminder.core.ApplicationInstance.APP_NAME + " - " + fxUI.getLocaleBundleString("filebrowser.dialogtitle"), "", "",
														 new FileChooser.ExtensionFilter[]{new FileChooser.ExtensionFilter(fxUI.getLocaleBundleString("filebrowser.allfiles_selector"), "*.*")});
				if(f != null){
					textfield.setText(f.getAbsolutePath());
					textfield.fireEvent(new KeyEvent(textfield, null, KeyEvent.KEY_RELEASED, ".", "", KeyCode.ACCEPT, false, false, false, false)); // Some input dialogs listen to the "KeyEvent.KEY_RELEASED" event to recognize changes
				}
			}
		});

		hbox.getChildren().addAll(textfield, b);
		HBox.setHgrow(textfield, Priority.ALWAYS);

		return hbox;
	}

	public static BorderPane createFxPasswordField(EventHandler<KeyEvent> onKeyReleased, String initalValue, boolean initialValueOfHidePw, FxUserInterface fxUI){
		TextField textField = new TextField(initalValue);
		PasswordField passwordField = new PasswordField();
		passwordField.setText(initalValue);
		return createFxPasswordField(onKeyReleased, textField, passwordField, initialValueOfHidePw, fxUI);
	}

	public static BorderPane createFxPasswordField(EventHandler<KeyEvent> onKeyReleased, TextField textField, PasswordField passwordField, boolean initialValueOfHidePw, FxUserInterface fxUI) {
		BorderPane row = new BorderPane(textField);

		CheckBox showPw = new CheckBox(fxUI.getLocaleBundleString("passwordfield.hide_password"));
		EventHandler<ActionEvent> toggle = new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if(showPw.isSelected()){
					passwordField.setDisable(false);
					row.setCenter(passwordField);
					textField.setDisable(true);
				}
				else{
					textField.setDisable(false);
					row.setCenter(textField);
					passwordField.setDisable(true);
				}
			}
		};

		textField.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
		    @Override
		    public void handle(KeyEvent event) {
		    	passwordField.setText(textField.getText());
		    	onKeyReleased.handle(event);
		    }});


		passwordField.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
		    @Override
		    public void handle(KeyEvent event) {
		    	textField.setText(passwordField.getText());
		    	onKeyReleased.handle(event);
		    }});

		showPw.setOnAction(toggle);

		if(initialValueOfHidePw == true){
			showPw.setSelected(initialValueOfHidePw);
			toggle.handle(new ActionEvent());
		}

		row.setBottom(showPw);

		return row;
	}

	public static Label createFxLabelWithStyleClass(String text, String styleClass){
		Label l = new Label(text);
		l.getStyleClass().add(styleClass);
		return l;
	}
}
