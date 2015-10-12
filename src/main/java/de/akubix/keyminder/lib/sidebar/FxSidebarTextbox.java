package de.akubix.keyminder.lib.sidebar;

import de.akubix.keyminder.core.db.TreeNode;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;

public abstract class FxSidebarTextbox implements FxSidebarElement {

	private final TextField textfield;
	private final BorderPane row;

	public FxSidebarTextbox(de.akubix.keyminder.core.ApplicationInstance instance)
	{
		textfield = new TextField();
		textfield.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				storeData(instance.getTree().getSelectedNode());
			}});
		
		Button copy = new Button("", de.akubix.keyminder.lib.gui.ImageSelector.getFxImageView(("icon_copy")));
		copy.setMinWidth(16);
		copy.setMaxWidth(16);
		copy.setTooltip(new Tooltip(instance.getFxUserInterface().getLocaleBundleString("mainwindow.menu.edit.copy_text")));

		copy.getStyleClass().add("noBorder");
		copy.setFocusTraversable(false);
		
		copy.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
					instance.getFxUserInterface().setClipboardText(getUIValue());
			}
		});
		
		row = new BorderPane(textfield);
		row.setRight(copy);
	}

	@Override
	public void setUIValue(String value) {
		textfield.setText(value);
	}

	@Override
	public String getUIValue() {
		return textfield.getText();
	}

	@Override
	public abstract boolean loadData(TreeNode node);
	
	@Override
	public abstract void storeData(TreeNode node);
	
	@Override
	public Node getFxRootNode() {
		return row;
	}
}
