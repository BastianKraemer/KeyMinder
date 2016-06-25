package de.akubix.keyminder.ui.fx.sidebar;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.KeyMinder;
import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.ui.fx.JavaFxUserInterfaceApi;
import de.akubix.keyminder.ui.fx.JavaFxUserInterface;
import de.akubix.keyminder.ui.fx.utils.ImageMap;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;

public abstract class FxSidebarHyperlink implements FxSidebarElement {

	private final Hyperlink hyperlink;
	private final BorderPane row;

	public FxSidebarHyperlink(ApplicationInstance instance){
		this(instance, false);
	}

	public FxSidebarHyperlink(ApplicationInstance instance, boolean useMailTo) throws IllegalStateException {
		final JavaFxUserInterfaceApi fxUI = JavaFxUserInterface.getInstance(instance);
		hyperlink = new Hyperlink();
		hyperlink.prefWidthProperty().bind(fxUI.getSidbarWidthProperty().subtract(12));
		hyperlink.setMinHeight(24);

		Button edit = new Button("", ImageMap.getFxImageView(("icon_edit")));
		edit.setMinWidth(16);
		edit.setMaxWidth(16);
		edit.setTooltip(new Tooltip(useMailTo ?
				fxUI.getLocaleBundleString("mainwindow.sidebar.hyperlink.edit_email") :
				fxUI.getLocaleBundleString("mainwindow.sidebar.hyperlink.edit_link")));
		edit.getStyleClass().add("noBorder");

		edit.setOnAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				storeData(instance.getTree().getSelectedNode());
			}
		});

		hyperlink.setOnMouseClicked(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent event) {
				if(event.getButton() == MouseButton.SECONDARY){
					fxUI.setClipboardText(getUIValue());
					fxUI.updateStatus(fxUI.getLocaleBundleString("mainwindow.sidebar.hyperlink.messages.copied_to_clipboard"));
				}
			}
		});

		if(useMailTo){
			hyperlink.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					String mailAddr = getUIValue();
					if(!mailAddr.equals("") && !mailAddr.equals("...")){
						try {
							Desktop.getDesktop().mail(new URI(mailAddr.startsWith("mailto:") ? mailAddr : "mailto:" + mailAddr));
							hyperlink.setVisited(false);
						} catch (IOException | URISyntaxException e) {
							fxUI.alert(String.format(fxUI.getLocaleBundleString("mainwindow.sidebar.hyperlink.cannot_start_mail_app"), e.getMessage()));
						}
					}
					else{
						storeData(instance.getTree().getSelectedNode());
					}
				}
			});
		}
		else{
			hyperlink.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					String url = getUIValue();
					if(!url.equals("") && !url.equals("...")){
						if(!url.matches(".*://.*")){url = "https://" + url;}
						if(!instance.getSettingsValueAsBoolean(ApplicationInstance.SETTINGS_KEY_USE_OTHER_WEB_BROWSER, false)){
							try {
								Desktop.getDesktop().browse(new URI(url));
								hyperlink.setVisited(false);
							} catch (IOException | URISyntaxException e) {
								fxUI.alert(String.format(fxUI.getLocaleBundleString("mainwindow.sidebar.hyperlink.cannot_open_url"), e.getMessage()));
							}
						}
						else{
							try{
								String value = instance.getSettingsValue(ApplicationInstance.SETTINGS_KEY_BROWSER_PATH);
								if(value.equals("")){fxUI.alert(fxUI.getLocaleBundleString("mainwindow.sidebar.hyperlink.message_no_browser_path_set")); return;}
								if(!new java.io.File(value).exists()){fxUI.alert(String.format(fxUI.getLocaleBundleString("mainwindow.sidebar.hyperlink.browser_not_found"), value)); return;}

								java.util.List<String> l = new java.util.ArrayList<>();
								l.add(instance.getSettingsValue(ApplicationInstance.SETTINGS_KEY_BROWSER_PATH));
								l.add(url);
								de.akubix.keyminder.lib.Tools.runProcess(l);
							}
							catch(IOException ioex){
								fxUI.alert(String.format(fxUI.getLocaleBundleString("mainwindow.sidebar.hyperlink.ioexception_occured"), ioex.getMessage()));
								if(KeyMinder.verbose_mode){
									ioex.printStackTrace();
								}
							}
						}
					}
					else
					{
						storeData(instance.getTree().getSelectedNode());
					}
				}
			});
		}

		row = new BorderPane(hyperlink);
		row.setRight(edit);
	}

	@Override
	public void setUIValue(String value) {
		hyperlink.setText(value);
	}

	@Override
	public String getUIValue() {
		return hyperlink.getText();
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
