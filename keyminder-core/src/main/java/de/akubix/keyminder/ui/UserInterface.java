/*	KeyMinder
	Copyright (C) 2016 Bastian Kraemer

	UserInterface.java

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package de.akubix.keyminder.ui;

import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;

/**
 * Java Interface for a 'KeyMinder user interface'
 *
 * Every KeyMinder application instance needs a {@link UserInterface}. Every implementation has to be annotated with {@link KeyMinderUserInterface}
 */
public interface UserInterface {
	/**
	 * Prompts a text input field to the user
	 * @param title The title for the window (this can be ignored if the UI is working on the console only)
	 * @param text The message that contains to the input dialog
	 * @param defaultValue A default value for the input data
	 * @return Then input text
	 * @throws UserCanceledOperationException if the user canceled the input
	 */
	public String getStringInput(String title, String text, String defaultValue) throws UserCanceledOperationException;

	/**
	 * Prompts a password input dialog to the user
	 * @param title The title for the window (this can be ignored if the UI is working on the console only)
	 * @param text he message that contains to the input dialog
	 * @param passwordHint A possible password hint (this string might be empty)
	 * @return The entered password
	 * @throws UserCanceledOperationException if the user canceled the input
	 */
	public char[] getPasswordInput(String title, String text, String passwordHint) throws UserCanceledOperationException;

	/**
	 * Prompts a YesNo-Choice to the user
	 * @param title The title for the window (this can be ignored if the UI is working on the console only)
	 * @param headline A headline for the question
	 * @param contentText The message that contains to the input dialog
	 * @return
	 */
	public boolean getYesNoChoice(String title, String headline, String contentText);

	/**
	 * Update the application status
	 * @param text The new status as text
	 */
	public void updateStatus(String text);

	/**
	 * Write something to the log (this log is designed to be printed somewhere in the UI and can be ignored)
	 * @param text
	 */
	public void log(String text);

	/**
	 * Prompts a alert message to the user
	 * @param text The message
	 */
	public void alert(String text);

	/**
	 * Checks if the current thread is the UI man thread
	 * @return {@code true} if the current thread is the UI thread (or if there is no special UI thread, {@code false} if not
	 */
	public boolean isUserInterfaceThread();
}
