/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	Tools.java

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
package de.akubix.keyminder.lib;

import java.io.IOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

/**
 * This class is just a collection of some useful methods
 */
public class Tools {

	private Tools(){}

	public static <T1, T2> void hashCopy(Map<T1, T2> source, Map<T1, T2> target){
		target.clear();
		for(T1 key: source.keySet())
		{
			target.put(key, source.get(key));
		}
	}

	/* The following method/function was written by StackOverflow (stackoverflow.com) user Umesh Awasthi and is licensed under CC BY-SA 3.0
	 * "Creative Commons Attribution-ShareAlike 3.0 Unported", http://creativecommons.org/licenses/by-sa/3.0/)
	 *
	 * Source: http://stackoverflow.com/questions/4165832/sorting-values-of-set
	 * The code has not been modified.
	 */

	public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
		  List<T> list = new ArrayList<T>(c);
		  Collections.sort(list);
		  return list;
	}

	public static String getFileExtension(String absoluteFileName){
		if(absoluteFileName.contains(System.getProperty("file.separator"))){
			absoluteFileName = absoluteFileName.substring(absoluteFileName.lastIndexOf(System.getProperty("file.separator")) + 1);
		}

		if(absoluteFileName.contains(".")){
			return absoluteFileName.substring(absoluteFileName.lastIndexOf("."));
		}
		else
		{
			return "";
		}
	}

	public static Process runProcess(List<String> commands) throws IOException{
		 ProcessBuilder pb = new ProcessBuilder(commands);
		 Process p = pb.start();
		 return p;
	}

	public static int arrayIndexOf(String[] arr, String value, boolean useLowerCases){
		if(useLowerCases){
			value = value.toLowerCase();
			for(int i = 0; i < arr.length; i++){
				if(arr[i].toLowerCase().equals(value)){return i;}
			}
		}
		else
		{
			for(int i = 0; i < arr.length; i++){
				if(arr[i].equals(value)){return i;}
			}
		}

		return -1;
	}

	public static String forceLineBreak(String src, int maxCharacterPerLine){
		// Code from: http://stackoverflow.com/questions/1033563/java-inserting-a-new-line-at-the-next-space-after-30-characters
		StringBuilder sb = new StringBuilder(src);
		int i = 0;
		while ((i = sb.indexOf(" ", i + maxCharacterPerLine)) != -1) {
		    sb.replace(i, i + 1, "\n");
		}
		return sb.toString();
	}

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

	public static boolean isYes(String s){
		s = s.toLowerCase();
		return (s.equals("true") || s.equals("yes") || s.equals("ja") || s.equals("y") ||  s.equals("1"));
	}

	/**
	 * Converts a number of milliseconds since 1970 to an regular date
	 * @param value The number of milliseconds as string
	 * @param shortFormat Controls the output format: "dd. MMMM yyyy, HH:mm" (false) or "dd.MM.yy HH:mm" (true)
	 * @param errorText The string the should be returned if the conversion fails
	 * @return The date as string with the selected format or the text specified by "errorText"
	 */
	public static String getTimeFromEpochMilli(String value, boolean shortFormat, String errorText){
		try{
			return Instant.ofEpochMilli(Long.parseLong(value)).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(shortFormat ? "dd.MM.yy HH:mm" : "dd. MMMM yyyy, HH:mm"));
		}
		catch(NumberFormatException | DateTimeException ex){
			return errorText;
		}
	}
}
