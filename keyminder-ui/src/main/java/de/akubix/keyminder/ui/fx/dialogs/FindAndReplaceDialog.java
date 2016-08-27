/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	FindAndReplaceDialog.java

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

import java.time.ZoneId;
import java.util.function.Supplier;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.db.Tree;
import de.akubix.keyminder.ui.fx.JavaFxUserInterfaceApi;
import de.akubix.keyminder.ui.fx.utils.ImageMap;
import de.akubix.keyminder.ui.fx.utils.StylesheetMap;
import de.akubix.keyminder.util.search.MatchReplace;
import de.akubix.keyminder.util.search.NodeMatchResult;
import de.akubix.keyminder.util.search.NodeWalker;
import de.akubix.keyminder.util.search.matcher.TextMatcher;
import de.akubix.keyminder.util.search.matcher.TimeMatcher;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class FindAndReplaceDialog {

	private static FindAndReplaceDialog instance = null;
	private Stage me;
	private Tree tree;
	private TextField findTextField;
	private TextField replaceTextField;
	private CheckBox ignoreCase;
	private JavaFxUserInterfaceApi fxUI;

	private FindAndReplaceDialog(Stage primaryStage, Tree tree, de.akubix.keyminder.ui.fx.JavaFxUserInterfaceApi fxUI){
		this.tree = tree;
		this.fxUI = fxUI;
		createScene();
		me.initOwner(primaryStage);
	}

	public void show(){
		me.show();
	}

	public void bringToFront(){
		me.requestFocus();
	}

	public static synchronized void showInstance(Stage primaryStage, Tree tree, de.akubix.keyminder.ui.fx.JavaFxUserInterfaceApi fxUI){
		if(instance == null){
			instance = new FindAndReplaceDialog(primaryStage, tree, fxUI);
			instance.show();
		}
		else{
			instance.bringToFront();
		}
	}

	private void createScene(){
		BorderPane root = new BorderPane();

		Label title = new Label(fxUI.getLocaleBundleString("dialogs.findreplace.headerlabel"));
		Pane top = new Pane(title);
		top.getStyleClass().add("header");
		root.setTop(top);

		VBox vbox = new VBox(4);

		// Textfields
		findTextField = new TextField();
		replaceTextField = new TextField();

		ignoreCase = new CheckBox(fxUI.getLocaleBundleString("dialogs.findreplace.ignorecaselabel"));

		// Date-Picker
		DatePicker datePicker = new DatePicker();
		datePicker.setMinWidth(290);

		final ComboBox<String> nodeAttribSelector = new ComboBox<>();
		nodeAttribSelector.getItems().addAll(fxUI.getLocaleBundleString("dialogs.findreplace.combobox.modificationdate"), fxUI.getLocaleBundleString("dialogs.findreplace.combobox.creationdate"));
		nodeAttribSelector.getSelectionModel().select(0);
		nodeAttribSelector.setMinWidth(190);

		HBox dateHBox = new HBox(4);
		ComboBox<String> compareType = new ComboBox<>();
		compareType.getItems().addAll(fxUI.getLocaleBundleString("dialogs.findreplace.combobox.selector_before"),
									  fxUI.getLocaleBundleString("dialogs.findreplace.combobox.selector_after"),
									  fxUI.getLocaleBundleString("dialogs.findreplace.combobox.selector_at"));
		compareType.getSelectionModel().select(0);
		compareType.setMinWidth(96);
		dateHBox.getChildren().addAll(nodeAttribSelector, compareType);

		Separator s = new Separator();
		s.setPadding(new Insets(10,0,10,0));
		// Add to vbox
		vbox.getChildren().addAll(new Label(fxUI.getLocaleBundleString("dialogs.findreplace.textfieldlabel")),
								  findTextField, ignoreCase, new Label("Ersetzen durch:"), replaceTextField, s, dateHBox, datePicker);

		BorderPane.setMargin(vbox, new Insets(4,10,0,10));
		root.setCenter(vbox);

		// Event methods

		final Supplier<String> timeConditionNodeAttrubiteNameSupplier = () -> nodeAttribSelector.getValue().equals(fxUI.getLocaleBundleString("dialogs.findreplace.combobox.modificationdate")) ? "modified" : "created";

		final EventHandler<ActionEvent> onFindButtonClicked = (event) -> {
			NodeWalker.SearchResult result;
			if(datePicker.getValue() != null) {

				result = NodeWalker.find(
					tree,
					new TextMatcher(findTextField.getText(), true, ignoreCase.isSelected()),
					new TimeMatcher(
						timeConditionNodeAttrubiteNameSupplier.get(),
						TimeMatcher.getCompareTypeFromString(compareType.getValue()), datePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));

			}
			else {
				result = NodeWalker.find(tree, new TextMatcher(findTextField.getText(), true, ignoreCase.isSelected()));
			}

			if(result.getState() == NodeWalker.SearchState.NOT_FOUND){
				fxUI.updateStatus(fxUI.getLocaleBundleString("mainwindow.find.text_not_found"));
			}
			else if(result.getState() == NodeWalker.SearchState.END_REACHED){
				fxUI.updateStatus(fxUI.getLocaleBundleString("mainwindow.find.end_of_document_reached"));
			}
		};

		// find and replace buttons

		HBox bottom = new HBox(4);

		Button findButton = new Button(fxUI.getLocaleBundleString("dialogs.findreplace.findbuttontext"));
		findButton.setOnAction(onFindButtonClicked);

		Button replaceButton = new Button(fxUI.getLocaleBundleString("dialogs.findreplace.replacebuttontext"));
		replaceButton.setOnAction((event) -> {
			if(replaceTextField.getText().equals("")){

				NodeMatchResult matchResult = NodeWalker.nodeMatches(
						tree.getSelectedNode(),
						new TextMatcher(findTextField.getText(), true, ignoreCase.isSelected()),
						new TimeMatcher(
							timeConditionNodeAttrubiteNameSupplier.get(),
							TimeMatcher.getCompareTypeFromString(compareType.getValue()), datePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant()));

				if(matchResult != null){
					MatchReplace.simpleReplace(matchResult, replaceTextField.getText());
				}

				onFindButtonClicked.handle(null);
			}
		});

		findButton.setMinWidth(138);
		replaceButton.setMinWidth(137);

		bottom.setAlignment(Pos.CENTER);
		bottom.getChildren().add(findButton);
		bottom.getChildren().add(replaceButton);

		findButton.setDefaultButton(true);

		root.setBottom(bottom);
		BorderPane.setMargin(bottom, new Insets(0,10,10,10));

		Scene myScene = new Scene(root, 300, 270);
		StylesheetMap.assignStylesheets(myScene);

		me = new Stage();
		me.setTitle(ApplicationInstance.APP_NAME + " - " + fxUI.getLocaleBundleString("dialogs.findreplace.title"));
		me.setScene(myScene);

		me.setResizable(false);
		//me.initModality( Modality.NONE );
		ImageMap.addDefaultIconsToStage(me);

		me.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent event) {
				instance = null;
			}
		});
	}
}
