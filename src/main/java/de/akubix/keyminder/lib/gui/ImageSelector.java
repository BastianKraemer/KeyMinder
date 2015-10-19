package de.akubix.keyminder.lib.gui;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * This class offers an interface to access all provided icons in a secure way using keywords
 * The "buildIndex()" method will be called by the main method as early as possible to provide this service.
 */
public class ImageSelector {
	private static Map<String, String> imageMap = new HashMap<>();
	public static void buildIndex()
	{
		imageMap.put("appicon", "/de/akubix/keyminder/images/app/AppIcon256.png");
		imageMap.put("appicon_16", "/de/akubix/keyminder/images/app/AppIcon16.png");
		imageMap.put("appicon_32", "/de/akubix/keyminder/images/app/AppIcon32.png");
		
		imageMap.put("icon_copy", "/de/akubix/keyminder/images/icons/copy.png");
		
		imageMap.put("icon_add", "/de/akubix/keyminder/images/icons/add.png");
		imageMap.put("icon_edit", "/de/akubix/keyminder/images/icons/dots.png");
		imageMap.put("icon_delete", "/de/akubix/keyminder/images/icons/delete.png");
		imageMap.put("icon_remove", "/de/akubix/keyminder/images/icons/remove.png");
		imageMap.put("icon_arrow-left", "/de/akubix/keyminder/images/icons/arrow-left.png");
		imageMap.put("icon_arrow-rotate-box", "/de/akubix/keyminder/images/icons/arrow-rotate-box.png");
		
		imageMap.put("icon_bash", "/de/akubix/keyminder/images/icons/bash.png");
		imageMap.put("icon_up", "/de/akubix/keyminder/images/icons/up.png");
		imageMap.put("icon_down", "/de/akubix/keyminder/images/icons/down.png");
		imageMap.put("icon_find", "/de/akubix/keyminder/images/icons/find.png");
		imageMap.put("icon_info", "/de/akubix/keyminder/images/icons/info.png");
		
		imageMap.put("icon_newfile", "/de/akubix/keyminder/images/icons/newfile.png");
		imageMap.put("icon_new_encrypted_file", "/de/akubix/keyminder/images/icons/newfile_lock.png");
		imageMap.put("icon_openfile", "/de/akubix/keyminder/images/icons/openfile.png");
		imageMap.put("icon_save", "/de/akubix/keyminder/images/icons/save.png");
		imageMap.put("icon_settings", "/de/akubix/keyminder/images/icons/settings.png");

		imageMap.put("icon_star", "/de/akubix/keyminder/images/icons/star.png");
		imageMap.put("icon_star_filled", "/de/akubix/keyminder/images/icons/star_filled.png");
		imageMap.put("icon_tick", "/de/akubix/keyminder/images/icons/tick.png");
		imageMap.put("icon_undo", "/de/akubix/keyminder/images/icons/undo.png");

		imageMap.put("icon_sort", "/de/akubix/keyminder/images/icons/sort.png");
		imageMap.put("icon_waiting", "/de/akubix/keyminder/images/icons/waiting.png");
		imageMap.put("icon_deadline", "/de/akubix/keyminder/images/icons/deadline.png");

		imageMap.put("icon_help_white", "/de/akubix/keyminder/images/icons/help_white.png");
		imageMap.put("icon_warning", "/de/akubix/keyminder/images/icons/warning.png");

		// Colors
		imageMap.put("icon_color", "/de/akubix/keyminder/images/icons/colorize.png");
		imageMap.put("icon_color_blue", "/de/akubix/keyminder/images/icons/colors/blue.png");
		imageMap.put("icon_color_darkblue", "/de/akubix/keyminder/images/icons/colors/darkblue.png");
		imageMap.put("icon_color_red", "/de/akubix/keyminder/images/icons/colors/red.png");
		imageMap.put("icon_color_orange", "/de/akubix/keyminder/images/icons/colors/orange.png");
		imageMap.put("icon_color_purple", "/de/akubix/keyminder/images/icons/colors/purple.png");
		imageMap.put("icon_color_yellow", "/de/akubix/keyminder/images/icons/colors/yellow.png");
		imageMap.put("icon_color_lime", "/de/akubix/keyminder/images/icons/colors/lime.png");
		imageMap.put("icon_color_green", "/de/akubix/keyminder/images/icons/colors/green.png");
		imageMap.put("icon_color_brown", "/de/akubix/keyminder/images/icons/colors/brown.png");
		imageMap.put("icon_color_black", "/de/akubix/keyminder/images/icons/colors/black.png");
		
	}
	
	public static String getIcon(String keyword)
	{
		if(imageMap.containsKey(keyword)){
			return imageMap.get(keyword);
		}
		else {
			return "";
		}
	}
		
	public static Image getFxImage(String keyword)
	{
		if(imageMap.containsKey(keyword)){
			return new Image(imageMap.get(keyword));
		}
		else {
			return null;
		}
	}
	
	public static ImageView getFxImageView(String keyword)
	{
		if(imageMap.containsKey(keyword)){
			return new ImageView(imageMap.get(keyword));
		}
		else {
			return null;
		}
	}
}
