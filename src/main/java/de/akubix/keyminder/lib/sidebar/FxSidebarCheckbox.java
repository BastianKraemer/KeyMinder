package de.akubix.keyminder.lib.sidebar;

import de.akubix.keyminder.core.db.TreeNode;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;

public abstract class FxSidebarCheckbox implements FxSidebarElement {

	private CheckBox checkbox;
	public FxSidebarCheckbox(de.akubix.keyminder.core.ApplicationInstance instance, String checkboxLabelText)
	{
		checkbox = new CheckBox(checkboxLabelText);
		checkbox.setPadding(new Insets(15,0,5,10));
		checkbox.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				storeData(instance.getTree().getSelectedNode());
			}
		});
	}
	
	@Override
	public void setUIValue(String value) {
		if(value != null)
		{
			checkbox.setSelected(de.akubix.keyminder.lib.Tools.isYes(value));
		}
	}

	@Override
	public String getUIValue() {
		return checkbox.isSelected() ? "true" : "false";
	}

	@Override
	public abstract boolean loadData(TreeNode node);

	@Override
	public abstract void storeData(TreeNode node);

	@Override
	public Node getFxRootNode() {
		return checkbox;
	}

}
