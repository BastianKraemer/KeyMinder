package de.akubix.keyminder.ui.fx.sidebar;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.tree.TreeNode;
import de.akubix.keyminder.ui.fx.JavaFxUserInterface;
import de.akubix.keyminder.ui.fx.JavaFxUserInterfaceApi;
import de.akubix.keyminder.ui.fx.utils.ImageMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;

abstract class FxSidebarPasswordbox implements FxSidebarElement {

	private TextInputControl inputElement;
	private final BorderPane row;

	protected FxSidebarPasswordbox(ApplicationInstance instance) {

		final JavaFxUserInterfaceApi fxUI = JavaFxUserInterface.getInstance(instance);
		row = new BorderPane();
		inputElement = createPasswordbox(instance, true);
		row.setCenter(inputElement);

		CheckBox showPw = new CheckBox(fxUI.getLocaleBundleString("passwordfield.show_password"));
		showPw.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				if(showPw.isSelected()){
					TextField t = new TextField(getUIValue());
					t.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
						@Override
						public void handle(KeyEvent event) {
							storeData(instance.getTree().getSelectedNode());
						}});
					inputElement = t;
				}
				else
				{
					inputElement = createPasswordbox(instance, false);
				}
				row.setCenter(inputElement);
			}});

		row.setBottom(showPw);

		Button copy = new Button("", ImageMap.getFxImageView(("icon_copy")));
		copy.setMinWidth(16);
		copy.setMaxWidth(16);
		copy.setTooltip(new Tooltip(fxUI.getLocaleBundleString("mainwindow.menu.edit.copy_text")));

		copy.getStyleClass().add("noBorder");
		copy.setFocusTraversable(false);

		copy.setOnAction((event) -> fxUI.setClipboardText(getUIValue()));
		row.setRight(copy);
	}

	private TextInputControl createPasswordbox(de.akubix.keyminder.core.ApplicationInstance instance, boolean init){
		PasswordField t = new PasswordField();
		if(!init){t.setText(getUIValue());}
		t.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(KeyEvent event) {
				storeData(instance.getTree().getSelectedNode());
			}});
		return t;
	}

	@Override
	public void setUIValue(String value) {
		inputElement.setText(value);
	}

	@Override
	public String getUIValue() {
		return inputElement.getText();
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
