/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	About.java

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
package de.akubix.keyminder.ui.fx;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.KeyMinder;
import de.akubix.keyminder.core.interfaces.FxUserInterface;
import de.akubix.keyminder.ui.fx.utils.ImageMap;
import de.akubix.keyminder.ui.fx.utils.StylesheetMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class About {

	private FxUserInterface fxUI;
	private static int WINDOW_WIDTH = 640;
	private TextArea content;
	public About(ApplicationInstance app){
		this.fxUI = app.getFxUserInterface();
	}

	public void show(){
		BorderPane root = new BorderPane();
		root.setId("Body");

		ImageView appIcon = ImageMap.getFxImageView("appicon");
		Pane imageContainer = new Pane(appIcon);
		appIcon.setId("AppIcon");
		imageContainer.setId("AppIconContainer");

		HBox hbox = new HBox(4);
		hbox.setId("BottomPane");

		hbox.getChildren().addAll(
				createBottomLabel(fxUI.getLocaleBundleString("about.show_info"), 2, (e) -> content.setText(fxUI.getLocaleBundleString("gplinfo"))),
				createBottomLabel(fxUI.getLocaleBundleString("about.show_gpl"), 2, (e) -> showGPL()));

		BorderPane contentPane = new BorderPane();
		contentPane.setId("ContentPane");
		VBox headlineContainer = new VBox(0);
		Label appName = new Label(ApplicationInstance.APP_NAME);

		appName.setId("AppName");
		Label appVersion = new Label("Version " + KeyMinder.getApplicationVersion());
		appVersion.setId("AppVersion");
		headlineContainer.getChildren().addAll(appName, appVersion);
		contentPane.setTop(headlineContainer);

		content = new TextArea(fxUI.getLocaleBundleString("gplinfo"));
		content.setId("Content");
		content.setEditable(false);
		content.setWrapText(true);
		contentPane.setCenter(content);

		root.setCenter(contentPane);
		root.setBottom(hbox);
		root.setLeft(imageContainer);

		Stage aboutWindow = new Stage();
		Scene myScene = new Scene(root, WINDOW_WIDTH, 250);

		StylesheetMap.assignStylesheet(myScene, StylesheetMap.WindowSelector.About);
		aboutWindow.setTitle(fxUI.getLocaleBundleString("about.title"));
		aboutWindow.setScene(myScene);

		aboutWindow.setResizable(false);
		ImageMap.addDefaultIconsToStage(aboutWindow);

		aboutWindow.show();
	}

	private void showGPL(){
		final String filename = "/GPLv3";
		try{
			java.util.Scanner s = new java.util.Scanner(ApplicationInstance.class.getResourceAsStream(filename), "UTF-8");
			content.setText(s.useDelimiter("\\A").next());
			s.close();
		}
		catch(Exception ex){
			content.setText(String.format("ERROR, cannot find resource '%s'", filename));
		}
	}

	private Node createBottomLabel(String text, int numberOfLabels, EventHandler<ActionEvent> onAction){
		Hyperlink l = new Hyperlink(text);
		l.setOnAction(onAction);
		l.setAlignment(Pos.CENTER);
		HBox.setHgrow(l, Priority.ALWAYS);
		l.getStyleClass().add("bottomLabel");
		l.setMinWidth(WINDOW_WIDTH / numberOfLabels);
		return l;
	}
}
