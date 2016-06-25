package de.akubix.keyminder.core;

import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.ui.KeyMinderUserInterface;
import de.akubix.keyminder.ui.UserInterface;

public class KeyMinderInstanceBuilder {
	public static ApplicationInstance getNewInstance(){
		return getNewInstance("./keyminder_settings.xml");
	}
	public static ApplicationInstance getNewInstance(String settingsFile){

		KeyMinder.prepareEnvironment(new String[]{});
		ApplicationInstance app = new ApplicationInstance(new TestInputSourceProvider());
		app.startup(false);
		return app;
	}

	@KeyMinderUserInterface(id="testUI", name="")
	private static class TestInputSourceProvider implements UserInterface {
		@Override
		public void updateStatus(String text) {}

		@Override
		public void log(String text) {}

		@Override
		public boolean getYesNoChoice(String title, String headline, String contentText) {
			return false;
		}

		@Override
		public String getStringInput(String title, String text, String defaultValue) throws UserCanceledOperationException {
			return "";
		}

		@Override
		public char[] getPasswordInput(String title, String text, String passwordHint) throws UserCanceledOperationException {
			return new char[]{};
		}

		@Override
		public void alert(String text) {}

		@Override
		public boolean isUserInterfaceThread() {
			return true;
		}
	};
}
