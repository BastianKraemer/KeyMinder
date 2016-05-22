package de.akubix.keyminder.core;

public class KeyMinderInstanceBuilder {
	public static ApplicationInstance getNewInstance(String settingsFile){
		// Prepare environment
		final String os = System.getProperty("os.name").toLowerCase();
		if(os.indexOf("linux") >= 0){KeyMinder.environment.put("os", "Linux"); KeyMinder.environment_isLinux = true;}
		else if(os.indexOf("win") >= 0){KeyMinder.environment.put("os", "Windows");}
		else {KeyMinder.environment.put("os", "Unknown");}

		if(settingsFile != null){
			KeyMinder.environment.put("cmd.settingsfile", settingsFile);
		}

		// Initialize EncryptionManager
		de.akubix.keyminder.core.encryption.EncryptionManager.loadDefaultCiphers();

		ApplicationInstance app = new ApplicationInstance();
		app.startup(false);
		return app;
	}
}
