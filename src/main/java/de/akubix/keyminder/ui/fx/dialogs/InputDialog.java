/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	InputDialog.java

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
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.core.interfaces.FxUserInterface;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class InputDialog {
	
	private Stage inputDialog;
	private TextField input;
	private boolean canceled = false;
	private final int sceneWidth = 400;
	private final int sceneHeight = 100;
	public InputDialog(Stage primaryStage, FxUserInterface fxUI, String windowTitle, String labelText, String defaultValueOrPasswordHint, boolean useAsPasswordDialog)
	{
		createScene(fxUI, windowTitle, labelText, defaultValueOrPasswordHint, useAsPasswordDialog);
		inputDialog.initOwner(primaryStage);
		inputDialog.setX(primaryStage.getX() + (primaryStage.getWidth() / 2) - sceneWidth / 2);
		inputDialog.setY(primaryStage.getY() + (primaryStage.getHeight() / 2) - (sceneHeight / 2) - 32);
	}
	
	public InputDialog(FxUserInterface fxUI, String windowTitle, String labelText, String defaultValueOrPasswordHint, boolean useAsPasswordDialog)
	{
		createScene(fxUI, windowTitle, labelText, defaultValueOrPasswordHint, useAsPasswordDialog);
		inputDialog.centerOnScreen();
	}

	private void createScene(FxUserInterface fxUI, String windowTitle, String labelText, String defaultValueOrPasswordHint, boolean useAsPasswordDialog)
	{
		BorderPane root = new BorderPane();
		
		Label title = new Label(labelText);
		BorderPane top = new BorderPane(title);
		top.getStyleClass().add("header");
		root.setTop(top);
		BorderPane.setAlignment(title, Pos.CENTER_LEFT);
		if(useAsPasswordDialog)
		{
			if(!defaultValueOrPasswordHint.equals(""))
			{
				Tooltip tooltip = new Tooltip(fxUI.getLocaleBundleString("dialogs.inputdialog.passwordhint") + ":\n" + defaultValueOrPasswordHint);
				Label l = new Label();
				l.setGraphic(new ImageView(de.akubix.keyminder.lib.gui.ImageSelector.getIcon("icon_help_white")));
				l.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				top.setRight(l);
				BorderPane.setAlignment(l, Pos.CENTER);
				BorderPane.setMargin(l, new Insets(0,8,0,0));

				EventHandler<? super MouseEvent> shwToolTip = (MouseEvent event) -> {
					Point2D p = l.localToScreen(l.getLayoutBounds().getMaxX(), l.getLayoutBounds().getMaxY()); //I position the tooltip at bottom right of the node (see below for explanation)
					tooltip.show(l, p.getX(), p.getY());
				};

				l.setCursor(Cursor.HAND);
				l.setOnMouseEntered(shwToolTip);
				l.setOnMouseClicked(shwToolTip);

				l.setOnMouseExited((MouseEvent event) -> tooltip.hide());
			}
			input = new PasswordField();
		}
		else
		{
			input = new TextField(defaultValueOrPasswordHint);
		}

		BorderPane.setMargin(input, new Insets(0,10,0,10));
		root.setCenter(input);

		HBox bottom = new HBox(4);
		
		Button ok = new Button(fxUI.getLocaleBundleString("okay"));
		ok.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				inputDialog.close();
			}
		});

		Button cancel = new Button(fxUI.getLocaleBundleString("cancel"));
		cancel.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				input.setText("");
				canceled = true;
				inputDialog.close();
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
		BorderPane.setMargin(bottom, new Insets(0,10,10,10));

		Scene myScene = new Scene(root, sceneWidth, sceneHeight);
		de.akubix.keyminder.lib.gui.StyleSelector.assignStylesheets(myScene);
		
		inputDialog = new Stage();
		if(!Launcher.environment_isLinux){
			inputDialog.setResizable(false); //Workaround, otherwise the window will "jump" a bit over the screen
		}
		
		inputDialog.setTitle(windowTitle);
		inputDialog.setScene(myScene);

		inputDialog.initModality( Modality.APPLICATION_MODAL );
		inputDialog.getIcons().add(new Image(ApplicationInstance.APP_ICON));
	}
	
	public String getInput() throws UserCanceledOperationException
	{
		inputDialog.showAndWait();
		if(this.canceled){throw new UserCanceledOperationException("User has canceled the operation.");}
		return input.getText();
	}
	
	public static String show(FxUserInterface fxUI, String windowTitle, String labelText, String defaultValue, boolean useAsPasswordDialog) throws UserCanceledOperationException
	{
		InputDialog inputDialog = new InputDialog(fxUI, windowTitle, labelText, defaultValue, useAsPasswordDialog);
		return inputDialog.getInput();
	}
}
