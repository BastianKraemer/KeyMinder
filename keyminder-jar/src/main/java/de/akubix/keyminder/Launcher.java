package de.akubix.keyminder;

import de.akubix.keyminder.core.KeyMinder;
import de.akubix.keyminder.ui.console.ConsoleMode;
import de.akubix.keyminder.ui.fx.MainWindow;

public class Launcher {
	public static void main(String[] args) {

		KeyMinder.prepareEnvironment(args);

		if(!KeyMinder.environment.containsKey("console_mode")){
			// Load the JavaFX UserInterface (It has to call "instance.startup()" when it is ready).
			MainWindow.init(args);
		}
		else{
			// Don't start the JavaFX UserInterface use the "ConsoleMode" instead (It has to call "instance.startup()" when it is ready).
			ConsoleMode console = new ConsoleMode();
			console.start();
		}
	}
}
