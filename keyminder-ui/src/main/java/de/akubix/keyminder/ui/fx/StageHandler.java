package de.akubix.keyminder.ui.fx;

import java.io.IOException;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.ui.fx.utils.ImageMap;
import de.akubix.keyminder.ui.fx.utils.StylesheetMap;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class StageHandler {

	static void showKeyMinderAbout(JavaFxUserInterfaceApi fxUi){

		final int windowWidth = 640;
		final int windowHeight = 250;

		try {

			Parent fxmlRoot = FXMLLoader.load(StageHandler.class.getResource("About.fxml"), fxUi.getLocaleRessourceBundle());

			Stage aboutWindow = new Stage();
			Scene myScene = new Scene(fxmlRoot, windowWidth, windowHeight);

			StylesheetMap.assignDefaultStylesheet(myScene);
			aboutWindow.setTitle(fxUi.getLocaleBundleString("about.title"));
			aboutWindow.setScene(myScene);

			aboutWindow.setResizable(false);
			ImageMap.addDefaultIconsToStage(aboutWindow);

			aboutWindow.show();

		} catch (IOException e) {
			fxUi.log("IOException: Unable to load about window");
			e.printStackTrace();
		}
	}

	static void showTerminal(ApplicationInstance app, JavaFxUserInterfaceApi fxUi){

		final int windowWidth = 640;
		final int windowHeight = 320;

		final int windowMinWidth = 560;
		final int windowMinHeight = 240;

		try {

			final FXMLLoader loader = new FXMLLoader(StageHandler.class.getResource("Terminal.fxml"), fxUi.getLocaleRessourceBundle());
			final Parent fxmlRoot = loader.load();

			final TerminalController controller = (TerminalController) loader.getController();

			Scene myScene = new Scene(fxmlRoot, windowWidth, windowHeight);
			StylesheetMap.assignDefaultStylesheet(myScene);

			Stage terminalwindow = new Stage();
			terminalwindow.setTitle(ApplicationInstance.APP_NAME + " Terminal");
			terminalwindow.setScene(myScene);

			terminalwindow.setResizable(true);
			ImageMap.addDefaultIconsToStage(terminalwindow);
			terminalwindow.setMinWidth(windowMinWidth);
			terminalwindow.setMinHeight(windowMinHeight);

			controller.setupAndShow(app, terminalwindow);

		} catch (IOException e) {
			fxUi.log("IOException: Unable to load terminal window");
			e.printStackTrace();
		}
	}
}
