package de.akubix.keyminder.locale;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class LocaleLoader {
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
}
