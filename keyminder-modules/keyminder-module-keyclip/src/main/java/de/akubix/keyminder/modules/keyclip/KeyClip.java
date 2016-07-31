/*	KeyMinder
	Copyright (C) 2015-2016 Bastian Kraemer

	KeyClip.java

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
package de.akubix.keyminder.modules.keyclip;

import java.awt.AWTException;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.events.DefaultEventHandler;
import de.akubix.keyminder.core.events.EventTypes.DefaultEvent;
import de.akubix.keyminder.ui.fx.IdentifiableElement;
import de.akubix.keyminder.ui.fx.JavaFxUserInterfaceApi;
import de.akubix.keyminder.ui.fx.MainWindow;
import de.akubix.keyminder.ui.fx.sidebar.FxSidebar;
import de.akubix.keyminder.ui.fx.utils.ImageMap;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;

/**
 * Module KeyClip: Provides a functionality which allows the user to transfer a certain user name and password to another application using the clip board.
 */
public class KeyClip {

	private final JavaFxUserInterfaceApi fxUI;
	private final ApplicationInstance app;
	private boolean trayItemCreated = false;
	private String pw = "";

	public KeyClip(ApplicationInstance instance, JavaFxUserInterfaceApi fxUI){
		this.app = instance;
		this.fxUI = fxUI;

		// Provide the KeyClip feature as command
		instance.getShell().addCommand("keyclip", KeyClipCmd.class.getName());

		Button keyClipSidebarButton = MainWindow.createSmallButton("KeyClip", "icon_arrow-rotate-box", 24, (event) -> {
			String[] data = fxUI.getCurrentSidebar().getUserNameAndPasswordSupplier().get();

			if(data.length >= 2){
				copyUserAndPassword(data[0], data[1]);
			}
		});

		DefaultEventHandler updateButtonEnableState = () -> {
			if(app.isAnyFileOpened()){
				FxSidebar sidebar = fxUI.getCurrentSidebar();
				if(sidebar != null && sidebar.hasUsernameAndPasswordSupplier()){
					keyClipSidebarButton.setDisable(false);
					return;
				}
			}
			keyClipSidebarButton.setDisable(true);
		};

		TabPane tabs = (TabPane) fxUI.lookupElement(IdentifiableElement.SIDEBAR_TAB_PANEL);
		tabs.getSelectionModel().selectedItemProperty().addListener((changeListener) -> updateButtonEnableState.eventFired());

		app.addEventHandler(DefaultEvent.OnFileOpened, updateButtonEnableState);
		app.addEventHandler(DefaultEvent.OnFileClosed, updateButtonEnableState);

		fxUI.addCustomElement(keyClipSidebarButton, IdentifiableElement.SIDEBAR_HEADER);
	}

	public void copyUserAndPassword(String username, String password){
		if(!trayItemCreated){
			if(!username.equals("")){
				fxUI.setClipboardText(username);

				if(!password.equals("")){
					this.pw = password;
					createSystemTrayIcon();
				}
			}
			else{
				if(!password.equals("")){fxUI.setClipboardText(password);}
			}
		}
	}

	private void createSystemTrayIcon(){
		if (java.awt.SystemTray.isSupported()) {

			java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
			java.awt.Image image = java.awt.Toolkit.getDefaultToolkit().getImage(getClass().getResource(ImageMap.getIcon("appicon_16")));

			final java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(image, ApplicationInstance.APP_NAME + " KeyClip");
			trayIcon.addMouseListener(new MouseListener() {
				@Override
				public void mouseReleased(MouseEvent e) {}

				@Override
				public void mousePressed(MouseEvent e) {}

				@Override
				public void mouseExited(MouseEvent e) {}

				@Override
				public void mouseEntered(MouseEvent e) {}

				@Override
				public void mouseClicked(MouseEvent e) {
					if(e.getButton() != 1){
						tray.remove(trayIcon);
						trayItemCreated = false;
					}
					else{
						if(trayItemCreated){
							trayItemCreated = false;
							fxUI.setClipboardText(pw);
							tray.remove(trayIcon);
						}
					}
				}
			});

			// set the TrayIcon properties
			trayIcon.addActionListener((e) -> {
				if(trayItemCreated){
					trayItemCreated = false;
					fxUI.setClipboardText(pw);
					tray.remove(trayIcon);
				}
			});

			trayIcon.setToolTip(String.format("%s %s - %s",
				ApplicationInstance.APP_NAME,
				"KeyClip",
				app.getLocale().getLanguage().equals("de") ?
					"Klicken Sie hier um das Passwort in die Zwischenablage zu kopieren" :
					"Click to copy your password to the clip board"
			));

			try {
				tray.add(trayIcon);
			} catch (AWTException e) {
				fxUI.alert(AlertType.ERROR, "KeyClip", "System Tray Icon", "An error occured. Unable to show system tray icon:\n" + e.getMessage());
			}
		}

		trayItemCreated = true;
	}
}
