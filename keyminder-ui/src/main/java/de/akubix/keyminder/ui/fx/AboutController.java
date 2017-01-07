package de.akubix.keyminder.ui.fx;

import java.net.URL;
import java.util.ResourceBundle;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.KeyMinder;
import de.akubix.keyminder.ui.fx.utils.ImageMap;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;

public class AboutController implements Initializable {

	@FXML
    private ImageView applicationIconImageView;

	@FXML
	private Label applicationVersionLabel;

	@FXML
	private TextArea aboutContent;

	private ResourceBundle locale;

	@Override
	public void initialize(URL location, ResourceBundle resources) {

		this.locale = resources;

		applicationIconImageView.setImage(ImageMap.getFxImage("appicon"));
		applicationVersionLabel.setText("Version " + KeyMinder.getApplicationVersion());

		aboutContent.setWrapText(true);

		showInfo();
	}

	@FXML
	protected void showInfo(){
		aboutContent.setText(locale.getString("gplinfo"));
	}

	@FXML
	protected void showLicense(){
		final String filename = "/GPLv3";
		try{
			java.util.Scanner s = new java.util.Scanner(ApplicationInstance.class.getResourceAsStream(filename), "UTF-8");
			String gplText = s.useDelimiter("\\A").next();
			s.close();

			aboutContent.setText(gplText.replaceAll("\n([A-Za-z0-9])", "$1"));
		}
		catch(Exception ex){
			aboutContent.setText(String.format("ERROR, cannot find resource '%s'", filename));
		}
	}
}
