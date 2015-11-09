/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	SaveChangesDialog.java

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

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.Launcher;
import de.akubix.keyminder.core.interfaces.FxUserInterface;
import de.akubix.keyminder.lib.Tools;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class SaveChangesDialog {
	private Stage dialog;
	private Result result = Result.Cancel;
	private final int sceneWidth = 400;
	private final int sceneHeight = 106;
	public SaveChangesDialog(Stage primaryStage, FxUserInterface fxUI)
	{
		createScene(fxUI);
		dialog.setX(primaryStage.getX() + (primaryStage.getWidth() / 2) - sceneWidth / 2);
		dialog.setY(primaryStage.getY() + (primaryStage.getHeight() / 2) - (sceneHeight / 2) - 32);
		dialog.initOwner(primaryStage);
	}
	
	public SaveChangesDialog(FxUserInterface fxUI)
	{
		createScene(fxUI);
		dialog.centerOnScreen();
	}

	private void createScene(FxUserInterface fxUI)
	{
		BorderPane root = new BorderPane();
		
		Label title = new Label(fxUI.getLocaleBundleString("dialogs.savechanges.headline"));
		Pane top = new Pane(title);
		top.getStyleClass().add("header");
		root.setTop(top);
		 
		VBox vbox = new VBox(2);
		Label content = new Label(fxUI.getLocaleBundleString("dialogs.savechanges.contenttext"));
		content.setWrapText(true);
		content.setMinHeight(40);
		vbox.getChildren().add(content);
		vbox.setPadding(new Insets(6,4,8,10));
		root.setCenter(vbox);
		
		HBox bottom = new HBox(4);
		
		Button save = new Button(fxUI.getLocaleBundleString("dialogs.savechanges.button_save"));
		save.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				result = Result.SaveChanges;
				dialog.close();
			}
		});

		Button discard = new Button(fxUI.getLocaleBundleString("dialogs.savechanges.button_discard"));
		discard.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				result = Result.DiscardChanges;
				dialog.close();
			}
		});

		Button cancel = new Button(fxUI.getLocaleBundleString("cancel"));
		cancel.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				result = Result.Cancel;
				dialog.close();
				}
		});

		save.setMinWidth(120);
		discard.setMinWidth(120);
		cancel.setMinWidth(100);
		
		bottom.setAlignment(Pos.CENTER_RIGHT);
		bottom.getChildren().add(save);
		bottom.getChildren().add(discard);
		bottom.getChildren().add(cancel);

		cancel.setCancelButton(true);
		save.setDefaultButton(true);
		 
		root.setBottom(bottom); 
		BorderPane.setMargin(bottom, new Insets(0,10,10,10));

		Scene myScene = new Scene(root, sceneWidth, sceneHeight);
		de.akubix.keyminder.lib.gui.StyleSelector.assignStylesheets(myScene);
		
		dialog = new Stage();
		dialog.setTitle(ApplicationInstance.APP_NAME + fxUI.getLocaleBundleString("dialogs.savechanges.title"));
		dialog.centerOnScreen();
		dialog.setScene(myScene);

		if(!Launcher.environment_isLinux){
			dialog.setResizable(false); //Workaround, otherwise the window will "jump" a bit over the screen
		}
		dialog.initModality( Modality.APPLICATION_MODAL );
		Tools.addDefaultIconsToStage(dialog);
	}
	
	public Result getInput()
	{
		dialog.showAndWait();
		return result;
	}
	
	public static Result show(FxUserInterface fxUI)
	{
		SaveChangesDialog dialog = new SaveChangesDialog(fxUI);
		return dialog.getInput();
	}
	
	public enum Result {
		SaveChanges, DiscardChanges, Cancel
	}
}
