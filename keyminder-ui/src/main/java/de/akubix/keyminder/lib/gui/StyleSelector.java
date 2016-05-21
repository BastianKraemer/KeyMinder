package de.akubix.keyminder.lib.gui;

import javafx.scene.Scene;

public class StyleSelector {
	public static void assignStylesheets(Scene scene){
		assignStylesheets(scene, WindowSelector.Other);
	}
	private static final String defaultCSSFile = "/de/akubix/keyminder/ui/css/style.css";

	public static void assignDefaultStylesheet(Scene scene){
		scene.getStylesheets().add(defaultCSSFile);
	}

	public static void assignStylesheets(Scene scene, WindowSelector window){
		switch(window){
			case MainWindow:
				scene.getStylesheets().addAll(defaultCSSFile, "/de/akubix/keyminder/ui/css/treeview.css");
				break;
			case Terminal:
				scene.getStylesheets().addAll(defaultCSSFile, "/de/akubix/keyminder/ui/css/terminal.css");
				break;
			case About:
				scene.getStylesheets().addAll(defaultCSSFile, "/de/akubix/keyminder/ui/css/about.css");
				break;
			case Other:
				scene.getStylesheets().add(defaultCSSFile);
				break;
			default:
				break;
		}
	}

	public enum WindowSelector {
		MainWindow, Terminal, About, Other
	}
}
