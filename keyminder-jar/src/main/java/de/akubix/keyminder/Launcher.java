package de.akubix.keyminder;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.KeyMinder;
import de.akubix.keyminder.ui.fx.MainWindow;

public class Launcher {
	public static void main(String[] args) {
		ApplicationInstance app = KeyMinder.init(args);

		if(!KeyMinder.environment.containsKey("console_mode")){
			// Load the JavaFX UserInterface (It has to call "instance.startup()" when it is ready).
			MainWindow.init(args, app);
		}
		else{
			// Don't start the JavaFX UserInterface use the "ConsoleMode" instead (It has to call "instance.startup()" when it is ready).
			de.akubix.keyminder.core.ConsoleMode console = new de.akubix.keyminder.core.ConsoleMode(app);
			console.start();
		}
	}
}
