package de.akubix.keyminder.ui.fx.components;

import de.akubix.keyminder.core.tree.TreeNode;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeCell;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

public abstract class AbstractEditableTreeCell extends TreeCell<TreeNodeReference> {

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
				createTextField(getItem());
			}
			else{
				textField.setText(getString());
			}
			setText(null);
			setGraphic(textField);
			textField.requestFocus();
			textField.selectAll();
		}
	}

	@Override
	public void cancelEdit() {
		super.cancelEdit();

		setText(getString());
		setGraphic(getTreeItem().getGraphic());
		setTreeEditStatus(false);
	}

	@Override
	public void updateItem(TreeNodeReference ref, boolean empty) {
		super.updateItem(ref, empty);

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

				TreeNode node = ref.getTreeNode();

				this.getStyleClass().removeAll("bold", "italic", "strikeout");
				if(node.hasAttribute("style")){
					this.getStyleClass().addAll(node.getAttribute("style").split(";"));
				}

				if(node.getColor().equals("")){
					setTextFill(Color.BLACK);
				}
				else{
					setTextFill(Color.web(node.getColor()));
				}

				setGraphic(getTreeItem().getGraphic());
			}
		}
	}

	private void createTextField(TreeNodeReference ref) {
		textField = new TextField(getString());
		textField.setOnKeyReleased((KeyEvent t) -> {
			if (t.getCode() == KeyCode.ENTER) {
				ref.getTreeNode().setText(textField.getText());
				commitEdit(ref);
			} else if (t.getCode() == KeyCode.ESCAPE) {
				cancelEdit();
			}
		});
	}

	private String getString() {
		TreeNodeReference item = getItem();
		return item == null ? "" : item.getTreeNode().getText();
	}

	public abstract boolean isTreeEdited();
	public abstract void setTreeEditStatus(boolean value);
}
