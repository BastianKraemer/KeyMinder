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

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.tree.TreeStore;
import de.akubix.keyminder.ui.fx.JavaFxUserInterfaceApi;
import de.akubix.keyminder.ui.fx.utils.ImageMap;
import de.akubix.keyminder.ui.fx.utils.StylesheetMap;
import de.akubix.keyminder.util.search.MatchReplace;
import de.akubix.keyminder.util.search.NodeMatchResult;
import de.akubix.keyminder.util.search.NodeWalker;
import de.akubix.keyminder.util.search.matcher.NodeMatcher;
import de.akubix.keyminder.util.search.matcher.TextMatcher;
import de.akubix.keyminder.util.search.matcher.TimeMatcher;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
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

public final class FindAndReplaceDialog {

	private static FindAndReplaceDialog instance = null;
	private final Stage me;
	private TreeStore tree;
	private TextField findTextField;
	private TextField replaceTextField;
	private CheckBox isCaseSensitive;
	private ComboBox<String> compareType;
	private ComboBox<String> nodeAttribSelector;
	private DatePicker datePicker;
	private Button findButton;
	private Button replaceButton;

	private JavaFxUserInterfaceApi fxUI;

	private FindAndReplaceDialog(Stage primaryStage, TreeStore tree, JavaFxUserInterfaceApi fxUI){
		this.tree = tree;
		this.fxUI = fxUI;

		Scene myScene = new Scene(createSceneContent(), 300, 270);
		StylesheetMap.assignStylesheets(myScene);

		me = new Stage();
		me.setTitle(ApplicationInstance.APP_NAME + " - " + fxUI.getLocaleBundleString("dialogs.findreplace.title"));
		me.setScene(myScene);

		me.setResizable(false);
		//me.initModality( Modality.NONE );
		ImageMap.addDefaultIconsToStage(me);

		me.setOnCloseRequest((event) -> instance = null);
		me.initOwner(primaryStage);
	}

	public void show(){
		me.show();
	}

	public void bringToFront(){
		me.requestFocus();
	}

	public static synchronized void showInstance(Stage primaryStage, TreeStore tree, de.akubix.keyminder.ui.fx.JavaFxUserInterfaceApi fxUI){
		if(instance == null){
			instance = new FindAndReplaceDialog(primaryStage, tree, fxUI);
			instance.show();
		}
		else{
			instance.bringToFront();
		}
	}

	private Parent createSceneContent(){

		// Textfields
		findTextField = new TextField();
		replaceTextField = new TextField();
		isCaseSensitive = new CheckBox(fxUI.getLocaleBundleString("dialogs.findreplace.ignorecaselabel"));

		// Date-Picker
		datePicker = new DatePicker();
		datePicker.setMinWidth(290);

		nodeAttribSelector = new ComboBox<>();
		nodeAttribSelector.getItems().addAll(fxUI.getLocaleBundleString("dialogs.findreplace.combobox.modificationdate"), fxUI.getLocaleBundleString("dialogs.findreplace.combobox.creationdate"));
		nodeAttribSelector.getSelectionModel().select(0);
		nodeAttribSelector.setMinWidth(190);

		compareType = new ComboBox<>();
		compareType.getItems().addAll(fxUI.getLocaleBundleString("dialogs.findreplace.combobox.selector_before"),
									  fxUI.getLocaleBundleString("dialogs.findreplace.combobox.selector_after"),
									  fxUI.getLocaleBundleString("dialogs.findreplace.combobox.selector_at"));

		compareType.getSelectionModel().select(0);
		compareType.setMinWidth(96);

		final HBox dateHBox = new HBox(4);
		dateHBox.getChildren().addAll(nodeAttribSelector, compareType);

		final Separator s = new Separator();
		s.setPadding(new Insets(10,0,10,0));

		findButton = new Button(fxUI.getLocaleBundleString("dialogs.findreplace.findbuttontext"));
		findButton.setMinWidth(138);
		findButton.setDefaultButton(true);

		replaceButton = new Button(fxUI.getLocaleBundleString("dialogs.findreplace.replacebuttontext"));
		replaceButton.setMinWidth(137);

		findButton.setOnAction((event) -> findNextNode());
		replaceButton.setOnAction((event) -> {
			replaceNodeContent();
			findNextNode();
		});

		HBox bottom = new HBox(4);
		bottom.setAlignment(Pos.CENTER);
		bottom.getChildren().addAll(findButton, replaceButton);

		final BorderPane root = new BorderPane();

		final Label title = new Label(fxUI.getLocaleBundleString("dialogs.findreplace.headerlabel"));
		final Pane top = new Pane(title);
		top.getStyleClass().add("header");


		final VBox vbox = new VBox(4);
		vbox.getChildren().addAll(new Label(fxUI.getLocaleBundleString("dialogs.findreplace.textfieldlabel")),
								  findTextField, isCaseSensitive, new Label("Ersetzen durch:"), replaceTextField, s, dateHBox, datePicker);

		BorderPane.setMargin(vbox, new Insets(4,10,0,10));

		root.setTop(top);
		root.setCenter(vbox);
		root.setBottom(bottom);
		BorderPane.setMargin(bottom, new Insets(0,10,10,10));

		return root;
	}

	private String getTimeConditionNodeAttrubiteName(){
		return nodeAttribSelector.getValue().equals(fxUI.getLocaleBundleString("dialogs.findreplace.combobox.modificationdate")) ? "modified" : "created";
	}

	private void findNextNode(){

		final NodeWalker.SearchResult result = NodeWalker.find(tree, getNodeMatchConditions());

		if(result.getState() == NodeWalker.SearchState.NOT_FOUND){
			fxUI.updateStatus(fxUI.getLocaleBundleString("mainwindow.find.text_not_found"));
		}
		else if(result.getState() == NodeWalker.SearchState.END_REACHED){
			fxUI.updateStatus(fxUI.getLocaleBundleString("mainwindow.find.end_of_document_reached"));
		}
	}

	private void replaceNodeContent(){

		if(!replaceTextField.getText().equals("")){

			final NodeMatchResult matchResult = NodeWalker.nodeMatches(tree.getSelectedNode(), getNodeMatchConditions());

			if(matchResult != null){
				MatchReplace.replaceContent(matchResult, replaceTextField.getText(), false);
			}

			findNextNode();
		}
	}

	private NodeMatcher[] getNodeMatchConditions(){

		if(datePicker.getValue() != null) {
			return new NodeMatcher[]{
				new TextMatcher(findTextField.getText(), true, isCaseSensitive.isSelected()),
				new TimeMatcher(getTimeConditionNodeAttrubiteName(), TimeMatcher.getCompareTypeFromString(compareType.getValue()), datePicker.getValue().atStartOfDay(ZoneId.systemDefault()).toInstant())
			};
		}
		else{
			return new NodeMatcher[]{
				new TextMatcher(findTextField.getText(), true, isCaseSensitive.isSelected())
			};
		}
	}
}
