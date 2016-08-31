/*	KeyMinder
	Copyright (C) 2015-2016 Bastian Kraemer

	KeyMinder.java

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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.akubix.keyminder.core.encryption.EncryptionManager;
import de.akubix.keyminder.lib.AESCore;

public class KeyMinder {

	static final String ENVIRONMENT_KEY_COMMANDLINE_FILE = "cmd.file";
	static final String ENVIRONMENT_KEY_PASSWORD = "cmd.password";
	static final String ENVIRONMENT_KEY_SETTINGS_FILE = "cmd.settingsfile";

	public static final Map<String, String> environment = new HashMap<>();
	public static boolean environment_isLinux = false;
	public static boolean verbose_mode = false;
	public static boolean enableColoredOutput = false;
	private static String keyminderVersion = null;

	public static void prepareEnvironment(String[] args) {

		// Prepare environment
		final String os = System.getProperty("os.name").toLowerCase();
		if(os.indexOf("linux") >= 0){environment.put("os", "Linux"); environment_isLinux = true; enableColoredOutput = true;}
		else if(os.indexOf("win") >= 0){environment.put("os", "Windows");}
		else {environment.put("os", "Unknown");}

		// Initialize EncryptionManager
		EncryptionManager.loadDefaultCiphers();

		final String buildPropertiesFile = "/de/akubix/keyminder/build.properties";

		try {
			Properties keyminderProperties = new Properties();
			keyminderProperties.load(KeyMinder.class.getResourceAsStream(buildPropertiesFile));
			keyminderVersion = keyminderProperties.getProperty("keyminder.version");
		} catch (IOException e) {
			throw new IllegalStateException("Properties file '" + buildPropertiesFile + "' not found inside jar file.");
		}

		// Read command line parameters an store them in the "environment" hash
		parseCommandlineArgs(args);

		// Test available encryption methods - and print a warning if there is only AES-128 available
		try {
			if(!AESCore.isAES256EncryptionAvailable()){
				if(!environment.containsKey("silent_mode")){
					System.out.println("Important security warning: AES-256 Encryption is NOT supported on this system.\nUsing fallback to AES-128, which provides less security.\n\n"
									 + "Please upgrade your Java installation using the \"Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files\" "
									 + "if you want to enable AES-256 encryption.");
				}
			}
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Important security warning: AES Encryption is NOT supported on this system!\n\t-> You wont be able to open encrypted files");
			System.exit(2);
		}
	}

	public static String getApplicationVersion(){
		return keyminderVersion;
	}

	public static void setVerboseMode(boolean value){
		verbose_mode = value;
	}

	private static void parseCommandlineArgs(String[] args){
		Pattern switchPattern = Pattern.compile("^(-|--|\\/)([A-Za-z0-9_-]+)$");

		try	{
			for(int i = 0; i < args.length; i++){
				Matcher matcher = switchPattern.matcher(args[i]);
				if(matcher.matches()){
					switch(matcher.group(2).toLowerCase()){
						case "open":
						case "openfile":
							environment.put(ENVIRONMENT_KEY_COMMANDLINE_FILE, args[i+1]);
							i++;
							break;

						case "pw":
						case "password":
							environment.put(ENVIRONMENT_KEY_PASSWORD, args[i+1]);
							i++;
							break;

						case "settingsfile":
						case "settings":
							environment.put(ENVIRONMENT_KEY_SETTINGS_FILE, args[i+1]);
							i++;
							break;

						case "console":
							environment.put("console_mode", "true");
							break;

						case "silent":
							environment.put("silent_mode", "true");
							break;

						case "verbose":
						case "log":
							environment.put("verbose_mode", "true");
							verbose_mode = true;
							break;

						case "no-output-redirect":
						case "no-redirect":
						case "noredirect":
							environment.put("disable_output_redirect", "true");
							break;

						case "color":
							if(args[i+1].toLowerCase().equals("on")){
								enableColoredOutput = true;
							}
							else if(args[i+1].toLowerCase().equals("off")){
								enableColoredOutput = false;
							}
							i++;
							break;

						case "version":
							System.out.println(de.akubix.keyminder.core.ApplicationInstance.APP_NAME + " Version " + getApplicationVersion());
							System.exit(0);
							break;

						case "m":
						case "mod":
						case "module":
							environment.put("module." + args[i+1].toLowerCase(), args[i+2]);
							i += 2;
							break;

						case "help":
							System.out.println(de.akubix.keyminder.core.ApplicationInstance.APP_NAME + " command line options:\n\n" +
											   "KeyMinder.jar [-open <file> [-pw <password>]\n" +
											   "              [-settings <file>]\n" +
											   "              [-console]\n" +
											   "              [-verbose]\n" +
											   "              [-silent]\n" +
											   "              [-version]\n" +
											   "              [-help]\n\n" +
											   "For more information take a look at the README file.");
							System.exit(0);
							break;

						default:
							System.out.println("Unknown Parameter: " + args[i]);
							System.exit(1);
					}
				}
				else{
					if(i + 1 < args.length){
						if(environment.containsKey(ENVIRONMENT_KEY_COMMANDLINE_FILE)){
							if(environment.containsKey(ENVIRONMENT_KEY_PASSWORD)){
								System.out.println("Error while parsing commandline arguments: '" + args[i] + "' is not a parameter.");
								System.exit(0);
							}
							else{
								environment.put(ENVIRONMENT_KEY_PASSWORD, args[i+1]);
							}
						}
						else{
							environment.put(ENVIRONMENT_KEY_COMMANDLINE_FILE, args[i+1]);
						}
					}
				}
			}
		}
		catch(IndexOutOfBoundsException ex){
			System.out.println("Error while parsing commandline arguments.");
			System.exit(1);
		}
	}
}
