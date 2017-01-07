package de.akubix.keyminder.ui.fx;

import java.io.IOException;

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
}
