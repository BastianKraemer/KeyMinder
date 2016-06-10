package de.akubix.keyminder.ui.fx.sidebar;

import de.akubix.keyminder.core.db.TreeNode;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;

public abstract class FxSidebarTextarea implements FxSidebarElement {

	private final TextArea textarea;

	public FxSidebarTextarea(de.akubix.keyminder.core.ApplicationInstance instance)
	{
		textarea = new TextArea();
		textarea.setMaxHeight(100);
		textarea.setFocusTraversable(false);

		textarea.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				storeData(instance.getTree().getSelectedNode());
			}});
	}

	@Override
	public void setUIValue(String value) {
		textarea.setText(value);
	}

	@Override
	public String getUIValue() {
		return textarea.getText();
	}

	@Override
	public abstract boolean loadData(TreeNode node);

	@Override
	public abstract void storeData(TreeNode node);

	@Override
	public Node getFxRootNode() {
		return textarea;
	}
}
