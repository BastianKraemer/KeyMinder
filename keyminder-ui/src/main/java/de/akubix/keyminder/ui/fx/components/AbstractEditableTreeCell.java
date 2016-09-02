package de.akubix.keyminder.ui.fx.components;

import de.akubix.keyminder.core.db.TreeNode;
import javafx.event.EventHandler;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

public abstract class AbstractEditableTreeCell extends TreeCell<TreeNode> {

	/*
	 * ======================================================================================================================================================
	 * TreeCell Factory Class
	 *
	 * This class is based on an example mentioned in the official JavaFX documentation
	 *
	 * JavaFX/Using JavaFX UI Controls, Release 2.2, Primary Author: Alla Redko
	 * https://docs.oracle.com/javafx/2/ui_controls/tree-view.htm
	 * ======================================================================================================================================================
	 */

	private TextField textField;
	public AbstractEditableTreeCell() {
		super();
	}

	@Override
	public void startEdit() {
		if(isTreeEdited()){
			super.startEdit();

			if (textField == null) {
				createTextField();
			}
			else{
				textField.setText(getItem().getText());
			}
			setText(null);
			setGraphic(textField);
			textField.requestFocus();
			textField.selectAll();
		}
	}

	@Override
	public void commitEdit(TreeNode newValue){
		super.commitEdit(newValue);

	};

	@Override
	public void cancelEdit() {
		super.cancelEdit();

		setText((String) getItem().getText());
		setGraphic(getTreeItem().getGraphic());
		setTreeEditStatus(false);
	}

	@Override
	public void updateItem(TreeNode item, boolean empty) {
		super.updateItem(item, empty);

		if (empty) {
			setText(null);
			setGraphic(null);
		} else {
			if (isEditing()) {
				if (textField != null) {
					textField.setText(getString());
				}
				setText(null);
				setGraphic(textField);
				setTreeEditStatus(false);
			} else {
				setText(getString());

				this.getStyleClass().removeAll("bold", "italic", "strikeout");
				if(item.hasAttribute("style")){
					this.getStyleClass().addAll(item.getAttribute("style").split(";"));
				}

				if(item.getColor().equals("")){
					setTextFill(Color.BLACK);
				}
				else{
					setTextFill(Color.web(item.getColor()));
				}

				setGraphic(getTreeItem().getGraphic());
			}
		}
	}

	private void createTextField() {
		textField = new TextField(getString());
		textField.setOnKeyReleased(new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent t) {
				if (t.getCode() == KeyCode.ENTER) {
					commitEdit(commitTreeNodeEdit(textField.getText()));
				} else if (t.getCode() == KeyCode.ESCAPE) {
					cancelEdit();
				}
			}
		});
	}

	private String getString() {
		TreeNode item = getItem();
		return item == null ? "" : item.getText();
	}

	public abstract boolean isTreeEdited();
	public abstract void setTreeEditStatus(boolean value);
	public abstract TreeNode commitTreeNodeEdit(String newNodeText);
}
