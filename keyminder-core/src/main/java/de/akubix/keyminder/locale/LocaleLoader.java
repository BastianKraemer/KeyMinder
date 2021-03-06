package de.akubix.keyminder.locale;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class LocaleLoader {

	private static Map<String, ResourceBundle> bundleMap = new HashMap<>();

	public static ResourceBundle loadLanguagePack(String packageName, String baseName, Locale locale) throws MissingResourceException {
		try{
			return ResourceBundle.getBundle("de.akubix.keyminder.locale." + packageName + "." + baseName, locale);
		}
		catch(MissingResourceException e){
			if(!locale.getLanguage().equals("en")){
				return loadLanguagePack(packageName, baseName, Locale.ENGLISH);
			}
			else{
				throw(e);
			}
		}
	}

	public static void provideBundle(String key, ResourceBundle bundle){
		bundleMap.putIfAbsent(key, bundle);
	}

	/**
	 * Get translations of the application
	 *
	 * By default you can use the key 'core' to access the general translations.
	 * Other keys may be provided by the user interface or any KeyMinder plugin
	 *
	 * @param key The key for the {@link ResourceBundle} with the translations
	 * @return The {@link ResourceBundle} or 'null' if the key is not defined
	 */
	public static ResourceBundle getBundle(String key){
		return bundleMap.get(key);
	}
}
