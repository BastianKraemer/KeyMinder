/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	ConsoleMode.java

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
package de.akubix.keyminder.core;

import java.util.NoSuchElementException;
import java.util.Scanner;

import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.core.interfaces.events.DefaultEventHandler;
import de.akubix.keyminder.core.interfaces.events.EventTypes.DefaultEvent;
import de.akubix.keyminder.shell.CommandException;

/**
 * ConsoleMode for this application. This is an alternative user interface for using this with a console only.
 */
public class ConsoleMode {

	private ApplicationInstance app;

	public ConsoleMode(ApplicationInstance instance){
		this.app = instance;
	}

	private static Scanner in;

	public void start(){
		try{
			in = new Scanner(System.in);

			System.out.println("\nKeyMinder\tCopyright (C) 2015-2016  Bastian Kraemer\n\n"
							 + "This program comes with ABSOLUTELY NO WARRANTY; for details type 'license -w'.\n"
							 + "This is free software, and you are welcome to redistribute it under certain conditions; type 'license' for details.\n\n");

			app.startup(true);

			app.addEventHandler(DefaultEvent.OnExit, new DefaultEventHandler() {
				@Override
				public void eventFired() {
					in.close();
				}
			});

			app.loadDefaultFile();

			while(true){
				try{
					System.out.print("\n$ ");
					String input = in.nextLine();

					app.getShell().runShellCommand(app, input);
				}
				catch(UserCanceledOperationException ex){
					throw new NoSuchElementException(ex.getMessage()); // 'exit' command has been executed
				}
				catch(CommandException ex){
					app.println(ex.getMessage());
				}
				catch(Exception ex){
					if(ex.getClass() == NoSuchElementException.class){
						throw new NoSuchElementException(ex.getMessage()); // Program has been terminated by using CTRL+C
					}
					System.out.println("Critical Error: " + ex.getMessage() + "\n");
					ex.printStackTrace(System.out);
					System.out.println("\nAn unexpected error has occured. This should not have happened.\n"
									 + "It is recommended to be careful with the next steps, they can have unpredictable effects.\n"
									 + "The suggested way is to save your password file to another location (use \"file saveas <filepath>\" to do this) to prevent any data loss and restart " + ApplicationInstance.APP_NAME + "."
									 + "\nIf you are able to, take a look at the error message and the stack trace - it's possible that this error is more or less harmless.");
				}
			}
		}
		catch (NoSuchElementException ex){} // Program has been terminated by using CTRL+C
	}

	public static boolean askYesNo(String question){
		if(in == null){System.err.println(ApplicationInstance.APP_NAME + " console interface is not initialized."); return false;}
		System.out.print(question + "\n[Yes/No]: ");
		String input = in.nextLine().toLowerCase();
		if(input.equals("y") || input.equals("yes") || input.equals("j") || input.equals("ja")){return true;}
		return false;
	}

	public static String readLineFromSystemIn(){
		if(in == null){System.err.println(ApplicationInstance.APP_NAME + " console interface is not initialized."); return "";}
		String input = in.nextLine();
		return input;
	}

	public static String readPasswordFromSystemIn(){
		try{
			return new String(System.console().readPassword());
		}
		catch(NullPointerException nullPointEx){
			return readLineFromSystemIn();
		}
	}

	public static void setClipboardText(String str){
		try{
			java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(str);
			java.awt.datatransfer.Clipboard clipboard = java.awt.Toolkit.getDefaultToolkit().getSystemClipboard();
		    clipboard.setContents(selection, selection);
		}
		catch(Exception ex){
		}
	}

	public static String getClipboardText(){
		try{
			return (String) java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().getData(java.awt.datatransfer.DataFlavor.stringFlavor);
		}
		catch(Exception ex){
			return "";
		}
	}
}
