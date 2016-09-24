/*	KeyMinder
	Copyright (C) 2016 Bastian Kraemer

	FileCmd.java

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
package de.akubix.keyminder.shell.commands;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import de.akubix.keyminder.core.ApplicationInstance;
import de.akubix.keyminder.core.FileConfiguration;
import de.akubix.keyminder.core.encryption.EncryptionManager;
import de.akubix.keyminder.core.exceptions.UserCanceledOperationException;
import de.akubix.keyminder.shell.AbstractShellCommand;
import de.akubix.keyminder.shell.AnsiColor;
import de.akubix.keyminder.shell.annotations.AllowCallWithoutArguments;
import de.akubix.keyminder.shell.annotations.Command;
import de.akubix.keyminder.shell.annotations.Description;
import de.akubix.keyminder.shell.annotations.Example;
import de.akubix.keyminder.shell.annotations.Operands;
import de.akubix.keyminder.shell.annotations.Option;
import de.akubix.keyminder.shell.io.CommandInput;
import de.akubix.keyminder.shell.io.CommandOutput;
import de.akubix.keyminder.shell.io.ShellOutputWriter;
import de.akubix.keyminder.util.Utilities;

@Command("file")
@AllowCallWithoutArguments
@Description("KeyMinder file management command")
@Operands(cnt = 1, description = "COMMAND_NAME ...OPTIONS\n\nSupported commands:\n" +
								"  'create' (or 'new')\n" +
								"  'open'\n" +
								"  'save'\n" +
								"  'saveas'\n" +
								"  'close'\n" +
								"  'info'\n" +
								"  'types'\n" +
								"  'set-cipher'\n" +
								"  'set-password'\n" +
								"  'reset-password'")
@Option(name = FileCmd.OPTION_PATH, paramCnt = 1, alias={"-p", "--file", "-f"},   description = "FILE_PATH  Path of the file")
@Option(name = FileCmd.OPTION_PASSWORD, paramCnt = 1, alias={"-p", "--password"}, description = "PASSWORD  The File password")
@Option(name = FileCmd.OPTION_ENCRYPT,                                            description = "Encrypt the file (only in combination with 'create')")
@Option(name = FileCmd.OPTION_CIPHER_NAME, paramCnt = 1,                          description = "Name of the encryption chipher (only in combination with 'set-cipher')")
@Option(name = FileCmd.OPTION_FILE_TYPE, paramCnt = 1,                            description = "FILE_TYPE  The new file type (only in combination with 'open' or 'saveas')")
@Example({	"# Create a file:\n   file create --path \"/tmp/file.keymind\" --encrypt",
			"# Open a file:\n  file open --path \"/tmp/file.keymind\" --pw test",
			"# Save a file:\n  file save\n  file saveas --path \"/path/to/another/file.keymind\"",
			"# Change the encryption cipher:\n  file set-cipher --name AES-256/PBKDF2"})
public final class FileCmd extends AbstractShellCommand {

	static final String OPTION_PATH = "--path";
	static final String OPTION_PASSWORD = "--pw";
	static final String OPTION_ENCRYPT = "--encrypt";
	static final String OPTION_CIPHER_NAME = "--name";
	static final String OPTION_FILE_TYPE = "--type";

	@Override
	public CommandOutput exec(ShellOutputWriter out, ApplicationInstance instance, CommandInput in) {
		boolean result = true;
		if(!in.getParameters().containsKey("$0")){
			in.getParameters().put("$0", new String[]{"info"});
		}

		switch(in.getParameters().get("$0")[0].toLowerCase()){
			case "create":
			case "new":
				if(!requireKeys(out, in.getParameters(), OPTION_PATH)){return CommandOutput.error();}
				File newFile = new File(in.getParameters().get(OPTION_PATH)[0]);

				if(in.getParameters().containsKey(OPTION_ENCRYPT)){
					result = instance.createNewFile(newFile, true);
				}
				else{
					result = instance.createNewFile(newFile, false);
				}

				printColoredStatus(out, result, String.format("File '%s' successfully created.", newFile.getName()), "Error: Unable to create password file.");

				break;

			case "open":
				if(!requireKeys(out, in.getParameters(), OPTION_PATH)){return CommandOutput.error();}
				File filePath = new File(in.getParameters().get(OPTION_PATH)[0]);
				String pw = in.getParameters().containsKey(OPTION_PASSWORD) ? in.getParameters().get(OPTION_PASSWORD)[0] : "";

				if(in.getParameters().containsKey(OPTION_FILE_TYPE)){
					result = instance.openFile(filePath, pw, in.getParameters().get(OPTION_FILE_TYPE)[0]);
				}
				else{
					result = instance.openFile(filePath, pw);
				}

				printColoredStatus(out, result, String.format("File '%s' successfully opened.", filePath.getName()), "Error: Unable to open password file.");
				break;

			case "save":
				if(instance.isAnyFileOpened()){
					result = instance.saveFile();
					printColoredStatus(out, result, "File saved.", "Error: File couldn't be saved.");
				}
				else{
					out.println("Cannot save file: There is no file currently opened.");
					result = false;
				}

				break;

			case "saveas":
			case "save_as":
			case "save-as":
				if(instance.isAnyFileOpened()){
					if(!requireKeys(out, in.getParameters(), OPTION_PATH)){return CommandOutput.error();}
					String newFilePath = in.getParameters().get(OPTION_PATH)[0];
					if(in.getParameters().containsKey(OPTION_FILE_TYPE)){
						String newFileType = in.getParameters().get(OPTION_FILE_TYPE)[0];
						try{
							instance.getCurrentFile().changeFileTypeIdentifier(instance, newFileType);
						}
						catch(IllegalArgumentException illArgeEx){
							out.println(String.format("Unkonw file type identifier \"%s\".", newFileType));
							return CommandOutput.error();
						}
					}
					else{
						instance.getCurrentFile().changeFileTypeIdentifier(
							instance,
							instance.getStorageManager().getIdentifierByExtension(
								Utilities.getFileExtension(newFilePath),
								instance.getCurrentFile().getFileTypeIdentifier()));
					}

					instance.getCurrentFile().changeFilepath(new File(newFilePath));
					result = instance.saveFile();
					printColoredStatus(out, result, "File saved.", "Error: File couldn't be saved.");
				}
				else{
					out.println("Cannot save file: No file opened.");
					return CommandOutput.error();
				}

				break;

			case "close":
				if(instance.isAnyFileOpened()){
					result = instance.closeFile();
					printColoredStatus(out, result, "File closed.", "The action has been canceled by a plugin or the user...");
				}
				else{
					out.println("Cannot close any file: There is no file opened.");
					return CommandOutput.error();
				}

				break;

			case "info":
				if(instance.isAnyFileOpened()){
					FileConfiguration currentFile = instance.getCurrentFile();
					out.println("Filepath:\t" + currentFile.getFilepath().getAbsolutePath());
					out.println("File type:\t" + currentFile.getFileTypeIdentifier());
					out.println("Format version:\t" + currentFile.getFileFormatVersion());
					out.println("Encryption:\t" + (currentFile.isEncrypted() ? currentFile.getEncryptionManager().getCipher().getCipherName() : "Disabled") + "\n");
				}
				else{
					out.println("No file opened.");
					return CommandOutput.error();
				}

				break;

			case "types":
				out.println("Supported file types:");
				instance.getStorageManager().forEachFileType((str) -> out.print(str + " "));
				out.print("\n\nKnown file extensions:\n");
				instance.getStorageManager().forEachKnownExtension((extension, assignedType) -> out.println(String.format("%-16s\t%s", extension, assignedType)));
				break;

			case "set-cipher":
				if(instance.isAnyFileOpened()){
					if(!instance.getCurrentFile().isEncrypted()){
						out.println("Please use 'file set-password' at first to enable the file encryption.");
						return CommandOutput.error();
					}

					if(!requireKeys(out, in.getParameters(), OPTION_CIPHER_NAME)){return CommandOutput.error();}
					String cipherName = in.getParameters().get(OPTION_CIPHER_NAME)[0];
					try {
						if(cipherName.toLowerCase().equals("none")){
							out.println("Please use 'file reset-password' to disable the encryption of your password file.");
							return CommandOutput.error();
						}
						instance.getCurrentFile().getEncryptionManager().setCipher(cipherName);
						out.setColor(AnsiColor.GREEN);
						out.printf("Encryption algorithmn has been changed to '%s'\n", cipherName);
						out.setColor(AnsiColor.RESET);
						out.println("The new cipher will be active as soon as you save your password file again.");

					} catch (NoSuchAlgorithmException e) {
						out.printf("Unknown encryption algorithm: '%s'.", cipherName);
						result = false;
					}
				}
				else{
					out.println("No file opened.");
					return CommandOutput.error();
				}

				break;

			case "set-password":
			case "set-pw":
				try{
					FileConfiguration currentFile = instance.getCurrentFile();
					boolean wasEncrypted = currentFile.isEncrypted();
					boolean enableChangePw = !wasEncrypted;

					// let the user enter his current password (if the file is encrypted)
					if(!enableChangePw){
						enableChangePw = (currentFile.getEncryptionManager().checkPassword(
											instance.requestPasswordInput("Change file password", "Please enter your current file password: ", "")));
					}

					if(enableChangePw){
						if(!currentFile.isEncrypted()){
							currentFile.encryptFile(new EncryptionManager(true));
						}

						if(currentFile.getEncryptionManager().requestPasswordInputWithConfirm(
								instance,
								wasEncrypted ? "Change file password" : "Set file password",
								"Please enter your " + (wasEncrypted ? "new " : "") + "file password: ", "Please enter your password again: ")){
							// New password has been set
							out.setColor(AnsiColor.GREEN);
							out.println("Password changed.");
							out.setColor(AnsiColor.RESET);
							out.println("Save your password file to write the encrypted file to your harddisk.");
						}
						else{
							if(!wasEncrypted){
								// Undo everything -> delete the created encryption manager
								currentFile.disableEncryption();
							}

							// Operation has been canceled
							out.println("The passwords you typed in does not match.");
							result = false;
						}
					}
					else{
						out.setColor(AnsiColor.RED);
						out.println("Incorrect password.");
					}
				} catch (UserCanceledOperationException e) {
					result = false;
				}
				break;

			case "reset-password":
			case "reset-pw":
				if(!instance.getCurrentFile().isEncrypted()){
					out.println("Encryption is already disabled.");
					break;
				}

				try {
					if(instance.getCurrentFile().getEncryptionManager().checkPassword(
							instance.requestPasswordInput("Change file password", "Please enter your current file password: ", ""))){
						instance.getCurrentFile().disableEncryption();
						out.println("Encryption of passwordfile disabled (not recommended).");
						out.setColor(AnsiColor.CYAN);
						out.println("Save your password file to write the unencrypted file to your harddisk.");
					}
					else{
						out.setColor(AnsiColor.RED);
						out.println("Incorrect password.");
						result = false;
					}
				} catch (UserCanceledOperationException e) {
					result = false;
				}

				break;

			default:
				out.setColor(AnsiColor.YELLOW);
				out.printf("Invalid option '%s'. Use 'man file' for more information.", in.getParameters().get("$0")[0]);
				return CommandOutput.error();
		}

		return result ? CommandOutput.success() : CommandOutput.error();
	}

	private static boolean requireKeys(ShellOutputWriter out, Map<String, String[]> parameters, String... requiredKeys){
		for(String key: requiredKeys){
			if(!parameters.containsKey(key)){
				out.printf("Error: Missing required argument '%s'.\n", key);
				return false;
			}
		}
		return true;
	}

	private static void printColoredStatus(ShellOutputWriter out, boolean status, String successMsg, String errorMsg){
		if(status){
			out.setColor(AnsiColor.GREEN);
			out.println(successMsg);
		}
		else{
			out.setColor(AnsiColor.RED);
			out.println(errorMsg);
		}
		out.setColor(AnsiColor.RESET);
	}
}
