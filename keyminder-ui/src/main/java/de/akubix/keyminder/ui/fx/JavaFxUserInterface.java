package de.akubix.keyminder.ui.fx;

import de.akubix.keyminder.core.ApplicationInstance;

public class JavaFxUserInterface {
	public static final String LANGUAGE_BUNDLE_KEY = "fxUI";
	public static final String USER_INTERFACE_ID = "JavaFX user interface";

	public static boolean isLoaded(ApplicationInstance app){
		return app.getUserInterfaceInformation().id().equals(USER_INTERFACE_ID);
	}

	public static JavaFxUserInterfaceApi getInstance(ApplicationInstance app) throws IllegalStateException {
		try{
			return (JavaFxUserInterfaceApi) app.getUserInterface();
		}
		catch(ClassCastException ex){
			ex.printStackTrace();
			throw new IllegalStateException("JavaFx user interface is not loaded.");
		}
	}
}
