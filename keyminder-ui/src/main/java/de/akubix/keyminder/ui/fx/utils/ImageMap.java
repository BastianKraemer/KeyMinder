package de.akubix.keyminder.ui.fx.utils;

import java.io.IOException;
import java.util.Properties;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

/**
 * This class offers a possibility to access all provided icons in a secure way using keywords
 */
public class ImageMap {
	private static Properties imageMap = null;
	private static final String IMAGE_LIST_PROPERTIES_FILE = "/de/akubix/keyminder/images/images.properties";

	private static void loadProperties(){
		if(imageMap == null){
			try {
				imageMap = new Properties();
				imageMap.load(ImageMap.class.getResourceAsStream(IMAGE_LIST_PROPERTIES_FILE));
			} catch (IOException e){
				System.err.println("Properties file '" + IMAGE_LIST_PROPERTIES_FILE +"' not found inside jar file.");
			}
		}
	}

	/**
	 * Returns the path of an image file selected by a keyword
	 * @param keyword the key of the image
	 * @return the path to the image
	 */
	public static String getIcon(String keyword){
		loadProperties();
		return (String) imageMap.getOrDefault(keyword, "");
	}

	/**
	 * Returns a JavaFX Image selected by a keyword
	 * @param keyword the key of the image
	 * @return the image
	 */
	public static Image getFxImage(String keyword){
		loadProperties();
		if(imageMap.containsKey(keyword)){
			return new Image((String) imageMap.get(keyword));
		}
		else {
			return null;
		}
	}

	/**
	 * Returns a JavaFX ImageView selected by a keyword
	 * @param keyword the key of the image
	 * @return the {@link ImageView}
	 */
	public static ImageView getFxImageView(String keyword){
		loadProperties();
		if(imageMap.containsKey(keyword)){
			return new ImageView((String) imageMap.get(keyword));
		}
		else {
			return null;
		}
	}

	/**
	 * Adds the KeyMinder icons with the sizes 16x16, 32x32 and 256x256 pixels to a stage
	 * @param stage the JavaFx {@link Stage}
	 */
	public static void addDefaultIconsToStage(Stage stage){
		loadProperties();
		stage.getIcons().addAll(new Image((String) imageMap.get("appicon_16")),
								new Image((String) imageMap.get("appicon_32")),
								new Image((String) imageMap.get("appicon")));
	}
}
