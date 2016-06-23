package de.akubix.keyminder.core.interfaces;

import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;

public interface UserInterface {
	public String getStringInput(String title, String text, String defaultValue) throws UserCanceledOperationException;
	public char[] getPasswordInput(String title, String text, String passwordHint) throws UserCanceledOperationException;
	public boolean getYesNoChoice(String title, String headline, String contentText);

	public void updateStatus(String text);
	public void log(String text);
	public void alert(String text);

	public boolean isUserInterfaceThread();
}
