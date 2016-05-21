package de.akubix.keyminder.lib.sidebar;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import de.akubix.keyminder.core.db.TreeNode;
import de.akubix.keyminder.core.interfaces.FxUserInterface;
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

	public FxSidebarHyperlink(de.akubix.keyminder.core.ApplicationInstance instance){
		this(instance, false);
	}

	public FxSidebarHyperlink(de.akubix.keyminder.core.ApplicationInstance instance, boolean useMailTo){
		FxUserInterface fxUI = instance.getFxUserInterface();
		hyperlink = new Hyperlink();
		hyperlink.prefWidthProperty().bind(instance.getFxUserInterface().getSidbarWidthProperty().subtract(12));
		hyperlink.setMinHeight(24);

		Button edit = new Button("", de.akubix.keyminder.lib.gui.ImageSelector.getFxImageView(("icon_edit")));
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
					instance.getFxUserInterface().setClipboardText(getUIValue());
					instance.getFxUserInterface().updateStatus(fxUI.getLocaleBundleString("mainwindow.sidebar.hyperlink.messages.copied_to_clipboard"));
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
							instance.getFxUserInterface().alert(String.format(fxUI.getLocaleBundleString("mainwindow.sidebar.hyperlink.cannot_start_mail_app"), e.getMessage()));
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
						if(!instance.getSettingsValueAsBoolean(de.akubix.keyminder.core.ApplicationInstance.SETTINGS_KEY_USE_OTHER_WEB_BROWSER, false)){
							try {
								Desktop.getDesktop().browse(new URI(url));
								hyperlink.setVisited(false);
							} catch (IOException | URISyntaxException e) {
								instance.getFxUserInterface().alert(String.format(fxUI.getLocaleBundleString("mainwindow.sidebar.hyperlink.cannot_open_url"), e.getMessage()));
							}
						}
						else{
							try{
								String value = instance.getSettingsValue(de.akubix.keyminder.core.ApplicationInstance.SETTINGS_KEY_BROWSER_PATH);
								if(value.equals("")){instance.getFxUserInterface().alert(fxUI.getLocaleBundleString("mainwindow.sidebar.hyperlink.message_no_browser_path_set")); return;}
								if(!new java.io.File(value).exists()){instance.getFxUserInterface().alert(String.format(fxUI.getLocaleBundleString("mainwindow.sidebar.hyperlink.browser_not_found"), value)); return;}

								java.util.List<String> l = new java.util.ArrayList<>();
								l.add(instance.getSettingsValue(de.akubix.keyminder.core.ApplicationInstance.SETTINGS_KEY_BROWSER_PATH));
								l.add(url);
								de.akubix.keyminder.lib.Tools.runProcess(l);
							}
							catch(IOException ioex){
								instance.getFxUserInterface().alert(String.format(fxUI.getLocaleBundleString("mainwindow.sidebar.hyperlink.ioexception_occured"), ioex.getMessage()));
								if(de.akubix.keyminder.core.KeyMinder.verbose_mode){
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
