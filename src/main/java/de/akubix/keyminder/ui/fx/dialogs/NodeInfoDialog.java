/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	NodeInfoDialog.java

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
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.lib.Tools;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

/**
 * This class represents an JavaFX window which shows a lot of information about a specified TreeNode
 * This code is based on an example from http://java-buddy.blogspot.com/ (http://java-buddy.blogspot.de/2012/04/javafx-2-editable-tableview.html)
*/
public class NodeInfoDialog {

	private boolean attributesHasBeenChanged = false;
	private de.akubix.keyminder.core.ApplicationInstance app;
	private TreeNode node;

	/**
	 * Creates the node information dialog that shows several informations about a tree node
	 * @param treeNode the tree node
	 * @param instance the application instance
	 * @throws IllegalArgumentException if parameter treeNode == null
	 */
	public NodeInfoDialog(TreeNode treeNode, de.akubix.keyminder.core.ApplicationInstance instance) throws IllegalArgumentException{
		if(treeNode == null){throw new IllegalArgumentException("Node was null.");}
		app = instance;
		this.node = treeNode;
	}

	public class Record {
		private SimpleStringProperty attributeName;
		private SimpleStringProperty attributeValue;
		 
		Record(String name, String value){
			this.attributeName = new SimpleStringProperty(name);
			this.attributeValue = new SimpleStringProperty(value);
		}
		 
		public String getAttributeName() {
			return attributeName.get();
		}
		 
		public String getAttributeValue() {
			return attributeValue.get();
		}
		 
		public void setAttributeName(String name) {
			attributeName.set(name);
		}
		 
		public void setAttributeValue(String value) {
			attributeValue.set(value);
		}	
	}
 
	private TableView<Record> tableView = new TableView<>();
	
	/**
	 * Displays the dialog on the screen
	 * @param owner Another JavaFX window which will be the owner of this dialog
	 */
	public void show(Stage owner)
	{
		Stage me = new Stage();
		me.initOwner(owner);
		me.initModality( Modality.APPLICATION_MODAL );
		me.setTitle(app.getFxUserInterface().getLocaleBundleString("dialogs.nodeinfo.title"));
	 	me.setResizable(false);
	 	Tools.addDefaultIconsToStage(me);
	 	me.setWidth(400);
	 	me.setHeight(420);
	 	
		BorderPane root = new BorderPane();
		//root.setPadding(new Insets(4, 4, 4, 4));
		
	 	Scene myScene = new Scene(root);
		me.setScene(myScene);
	 	de.akubix.keyminder.lib.gui.StyleSelector.assignStylesheets(myScene);

		tableView.setEditable(true);
		Callback<TableColumn<Record, String>, TableCell<Record, String>> cellFactory =
		new Callback<TableColumn<Record, String>, TableCell<Record, String>>() {
			public TableCell<Record, String> call(TableColumn<Record, String> p) {
				return new EditingCell();
			}};
	 
		TableColumn<Record, String> columnAttribName = new TableColumn<>(app.getFxUserInterface().getLocaleBundleString("dialogs.nodeinfo.attributenamecolumn"));
		columnAttribName.setCellValueFactory(new PropertyValueFactory<Record,String>("attributeName"));

		TableColumn<Record, String> columnAttribValue = new TableColumn<>(app.getFxUserInterface().getLocaleBundleString("dialogs.nodeinfo.attributevaluecolumn"));
		columnAttribValue.setCellValueFactory(new PropertyValueFactory<Record,String>("attributeValue"));
	 
		// Add for Editable Cell of Value field
		columnAttribValue.setCellFactory(cellFactory);
		columnAttribValue.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent<Record, String>>() {
										@Override public void handle(TableColumn.CellEditEvent<Record, String> t) {
											((Record) t.getTableView().getItems().get(t.getTablePosition().getRow())).setAttributeValue(t.getNewValue());
											attributesHasBeenChanged = true;
										}});
	 
		//Add for Editable Cell of Attribute Name
		columnAttribName.setCellFactory(cellFactory);
		columnAttribName.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent<Record, String>>() {
										@Override public void handle(TableColumn.CellEditEvent<Record, String> t) {
												((Record) t.getTableView().getItems().get(t.getTablePosition().getRow())).setAttributeName(t.getNewValue());
												attributesHasBeenChanged = true;
											}
										});

		columnAttribName.setMinWidth((me.getWidth() / 2) - 10);
		columnAttribValue.setMinWidth((me.getWidth() / 2) - 10);

		tableView.getColumns().add(columnAttribName);
		tableView.getColumns().add(columnAttribValue);
		tableView.setItems(getAttributesFromNode(node));

		final VBox nodeInfo = new VBox(16);
		nodeInfo.getChildren().add(createVerticalNodeList(new Label(app.getFxUserInterface().getLocaleBundleString("dialogs.nodeinfo.attrib_text")), new Label(node.getText())));
		nodeInfo.getChildren().add(createVerticalNodeList(new Label(app.getFxUserInterface().getLocaleBundleString("dialogs.nodeinfo.attrib_creationdate")), new Label(getTimeFromAttribte(node, de.akubix.keyminder.core.ApplicationInstance.NODE_ATTRIBUTE_CREATION_DATE))));
		nodeInfo.getChildren().add(createVerticalNodeList(new Label(app.getFxUserInterface().getLocaleBundleString("dialogs.nodeinfo.attrib_modificationdate")), new Label(getTimeFromAttribte(node, de.akubix.keyminder.core.ApplicationInstance.NODE_ATTRIBUTE_MODIFICATION_DATE))));

		final VBox nodeLinkBox = new VBox(4);

		ScrollPane scrollPane = new ScrollPane(nodeLinkBox);
		scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
		scrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		scrollPane.setStyle("-fx-min-width: " + (me.getWidth() - 16) + "; -fx-max-width: -fx-min-width;");
		nodeLinkBox.setStyle("-fx-min-width: " + (me.getWidth() - 16) + "; -fx-max-width: -fx-min-width;");
		Label title = new Label(app.getFxUserInterface().getLocaleBundleString("dialogs.nodeinfo.linkednodestitle"));
		title.getStyleClass().add("h2");
		title.setStyle("-fx-padding: 0 0 0 4");
		nodeLinkBox.getChildren().add(title);
		app.forEachLinkedNode(node, (linkedNode) -> nodeLinkBox.getChildren().add(createLinkedNodeDataRow(linkedNode, nodeLinkBox)));

		final Accordion accordion = new Accordion();
		final TitledPane nodePropertiesPart = new TitledPane(app.getFxUserInterface().getLocaleBundleString("dialogs.nodeinfo.tab_properties"), nodeInfo);
		final TitledPane nodeAttribsPart = new TitledPane(app.getFxUserInterface().getLocaleBundleString("dialogs.nodeinfo.tab_attributes"), tableView);

		accordion.getPanes().addAll(nodePropertiesPart, nodeAttribsPart);

		if(nodeLinkBox.getChildren().size() > 1){
			accordion.getPanes().add(new TitledPane(app.getFxUserInterface().getLocaleBundleString("dialogs.nodeinfo.tab_nodelinks"), scrollPane));
		}

		accordion.setExpandedPane(nodePropertiesPart);

		Label titleLabel = de.akubix.keyminder.lib.Tools.createFxLabelWithStyleClass(app.getFxUserInterface().getLocaleBundleString("dialogs.nodeinfo.title"), "h2");
		titleLabel.setAlignment(Pos.CENTER_LEFT);
		Pane top = new Pane(titleLabel);
		top.getStyleClass().add("header");
		root.setTop(top);
		root.setCenter(accordion);

		// Buttons
		final HBox bottom = new HBox(4);
		final Button ok = new Button(app.getFxUserInterface().getLocaleBundleString("okay"));
		ok.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if(attributesHasBeenChanged){
					WriteAttributesToNode(node, tableView.getItems());
				}
				me.close();
			}
		});

		Button cancel = new Button(app.getFxUserInterface().getLocaleBundleString("cancel"));
		cancel.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				me.close();
			}
		});

		ok.setMinWidth(120);
		cancel.setMinWidth(120);
		bottom.getChildren().addAll(ok, cancel);
		bottom.setAlignment(Pos.CENTER_RIGHT);

		cancel.setCancelButton(true);
		ok.setDefaultButton(true);

		bottom.setPadding(new Insets(4, 4, 4, 4));

		root.setBottom(bottom);

		me.showAndWait();
	}
  
	private HBox createVerticalNodeList(Label leftLabel, Label rightLabel){
		HBox hbox = new HBox(4);
		leftLabel.setMinWidth(150);
		hbox.getChildren().addAll(leftLabel, rightLabel);
		return hbox;
	}

	private String getTimeFromAttribte(TreeNode node, String attribName)
	{
		if(node.hasAttribute(attribName)){
			return de.akubix.keyminder.lib.Tools.getTimeFromEpochMilli(node.getAttribute(attribName), false, "-");
		}
		else{
			return "-";
		}
	}

	private ObservableList<Record> getAttributesFromNode(TreeNode node)
	{
		ObservableList<Record> attribList = FXCollections.observableArrayList();
		for(String key: node.listAttributes())
		{
			attribList.add(new Record(key,node.getAttribute(key)));	
		}
		
		return attribList;
	}

	private void WriteAttributesToNode(TreeNode node, ObservableList<Record> attribList)
	{
		synchronized(node.getTree()){
			synchronized(node){
				node.getTree().beginUpdate();
				node.getUnrestrictedAccess().clearAttributes(false);
				
				for(Record entry: attribList){
					node.setAttribute(entry.getAttributeName(), entry.getAttributeValue());
				}
				node.getTree().endUpdate();
			}
		}
	}

	private Pane createLinkedNodeDataRow(TreeNode linkedNode, Pane container)
	{
		Hyperlink link = new Hyperlink(linkedNode.getText());
		link.setMinHeight(24);
		link.setOnAction((event) -> {linkedNode.getTree().setSelectedNode(linkedNode); link.setVisited(false);});
		link.setMinWidth(80);
		link.setMinHeight(24);
		link.setStyle("-fx-padding: 0 0 0 12");

		BorderPane bp = new BorderPane(link);
		BorderPane.setAlignment(link, Pos.CENTER_LEFT);
		bp.setRight(de.akubix.keyminder.ui.fx.MainWindow.createSmallButton(app.getFxUserInterface().getLocaleBundleString("dialogs.nodeinfo.removenodelink"), "icon_delete", 24, (event) -> {container.getChildren().remove(bp); app.removeLinkedNode(node, linkedNode.getId());}));
		return bp;
	}

	class EditingCell extends TableCell<Record, String> {
		private TextField textField;
		public EditingCell() {}
		 
		@Override
		public void startEdit() {
			super.startEdit();

			if(textField == null){createTextField();}
			setGraphic(textField);
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
			textField.selectAll();
		}

		@Override
		public void cancelEdit() {
			super.cancelEdit();
			setText(String.valueOf(getItem()));
			setContentDisplay(ContentDisplay.TEXT_ONLY);
		}
	 
		@Override
		public void updateItem(String item, boolean empty) {
			super.updateItem(item, empty);

			if(empty) {
				setText(null);
				setGraphic(null);
			} else {
				if(isEditing()) {
					if(textField != null) {
						textField.setText(getString());
					}
					setGraphic(textField);
					setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
				} else {
						setText(getString());
						setContentDisplay(ContentDisplay.TEXT_ONLY);
				}
			}
		}

		private void createTextField() {
			textField = new TextField(getString());
			textField.setMinWidth(this.getWidth() - this.getGraphicTextGap()*2);
			textField.setOnKeyPressed(new EventHandler<KeyEvent>() {
			 
				@Override
				public void handle(KeyEvent t) {
					if(t.getCode() == KeyCode.ENTER){
						commitEdit(textField.getText());
					} else if (t.getCode() == KeyCode.ESCAPE) {
						cancelEdit();
					}
				}});
		}
		 
		private String getString() {
			return getItem() == null ? "" : getItem().toString();
		}
	}
}
