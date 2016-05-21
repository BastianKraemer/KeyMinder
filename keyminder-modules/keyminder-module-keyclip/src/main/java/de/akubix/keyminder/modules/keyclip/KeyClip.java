/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

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
import de.akubix.keyminder.core.exceptions.ModuleStartupException;
import de.akubix.keyminder.core.interfaces.Command;
import de.akubix.keyminder.core.interfaces.CommandOutputProvider;
import javafx.scene.control.Alert.AlertType;

@de.akubix.keyminder.core.interfaces.ModuleProperties(
		name="KeyClip",
		description = "This module allows you to transfer your login credentials directly to another application using the clip board.",
		version = ".",
		dependencies = "",
		author="Bastian Kraemer")
/**
 * Module KeyClip: Provides a functionality which allows the user to transfer a certain user name and password to another application using the clip board.
 */
public class KeyClip implements de.akubix.keyminder.core.interfaces.Module {

	private de.akubix.keyminder.core.interfaces.FxUserInterface fxUI;
	private boolean trayItemCreated = false;
	private String pw = "";

	@Override
	public void onStartup(ApplicationInstance instance) throws ModuleStartupException {

		if(instance.isFxUserInterfaceAvailable()){
			this.fxUI = instance.getFxUserInterface();

			// Provide the KeyClip feature as command
			instance.provideNewCommand("keyclip", new Command() {
				@Override
				public String runCommand(CommandOutputProvider out, ApplicationInstance instance, String[] args) {
					if(args.length == 0){
						de.akubix.keyminder.core.db.TreeNode n = instance.getTree().getSelectedNode();
						copyUserAndPassword(n.getAttribute("username"), n.getAttribute("password"));
					}
					else if(args.length == 1)
					{
						copyUserAndPassword(args[0], "");
					}
					else if (args.length == 2){
						copyUserAndPassword(args[0], args[1]);
					}
					else if (args.length == 3){
						copyUserAndPassword(args[0], args[1]);
					}
					else {
						out.println("Usage: keyclip <username> <password>");
						return "failed";
					}
					return "ok";
				}});
		}
		else{
			throw new ModuleStartupException("JavaFX User Interface is not available.", ModuleStartupException.ModuleErrorLevel.FxUserInterfaceNotAvailable);
		}
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

	public void createSystemTrayIcon(){
		if (java.awt.SystemTray.isSupported()) {

			java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
			java.awt.Image image = java.awt.Toolkit.getDefaultToolkit().getImage(getClass().getResource(de.akubix.keyminder.lib.gui.ImageSelector.getIcon("appicon_16")));

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

		    trayIcon.setToolTip(ApplicationInstance.APP_NAME + " KeyClip - " + fxUI.getLocaleBundleString("module.keyclip.copy_action_tooltip"));

		    try {
		        tray.add(trayIcon);
		    } catch (AWTException e) {
		        System.err.println(e);
		       fxUI.alert(AlertType.ERROR, "KeyClip", "System Tray Icon", "An error occured. Unable to show system tray icon:\n" + e.getMessage());
		    }
		}

		trayItemCreated = true;
	}
}