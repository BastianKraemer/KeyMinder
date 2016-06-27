/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	FileConfiguration.java

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

import java.io.File;
import java.util.Map;

import de.akubix.keyminder.core.encryption.EncryptionManager;

public class FileConfiguration {
	private File filepath;
	private boolean encrypt = false;
	private String version;
	private String fileTypeIdentifier;
	private EncryptionManager encryptionManager;
	private final Map<String, String> fileAttributes;
	private final Map<String, String> fileSettings;

	public FileConfiguration(File file, String fileVersion, boolean encrypt, String fileTypeIdentifier, EncryptionManager encryptionManager,
							 Map<String, String> fileAttributes, Map<String, String> fileSettings){
		this.filepath = file;
		this.version = fileVersion;
		this.encrypt = encrypt;
		this.fileTypeIdentifier = fileTypeIdentifier;
		this.encryptionManager = encryptionManager;
		this.fileAttributes = fileAttributes;
		this.fileSettings = fileSettings;
	}

	/**
	 * Get the file path of the currently opened file
	 * @return the file path
	 */
	public File getFilepath() {
		return this.filepath;
	}

	/**
	 * Change the file path of the currently opened file
	 * @param newFilepath the new file path
	 */
	public void changeFilepath(File newFilepath) {
		if(this.filepath != null){
			this.filepath = newFilepath;
		}
	}

	/**
	 * Get the encryption status of this file
	 * @return Returns {@code true} if the file is encrypted, {@code false} if not
	 */
	public boolean isEncrypted() {
		return this.encrypt;
	}

	/**
	 * Switch on the encryption for this file
	 * @param encMan the encryption manager for this file
	 */
	public void encryptFile(EncryptionManager encMan) {
		if(encMan != null){
			this.encrypt = true;
			this.encryptionManager = encMan;
		}
	}

	/**
	 * Switch off the encryption for this file
	 */
	public void disableEncryption() {
			this.encrypt = false;
			this.encryptionManager.destroy();
			this.encryptionManager = null;
	}

	/**
	 * Get the current file version
	 * @return the current file format version
	 */
	public String getFileFormatVersion() {
		return version;
	}

	/**
	 * Set the current file format version to another value
	 * @param version the new file version
	 */
	public void setFileFormatVersion(String version) {
		this.version = version;
	}

	/**
	 * Get the current file type identifier
	 * @return the current file type identifier
	 */
	public String getFileTypeIdentifier() {
		return fileTypeIdentifier;
	}

	/**
	 * Change the current file type identifier
	 * @param instance the application instance
	 * @param newFileTypeIdentifier the new file type identifier
	 * @throws IllegalArgumentException if the fileTypeIdentifier is not valid
	 */
	public void changeFileTypeIdentifier(ApplicationInstance instance, String newFileTypeIdentifier) throws IllegalArgumentException {
		if(instance.getStorageManager().hasStorageHandler(newFileTypeIdentifier)){
			this.fileTypeIdentifier = newFileTypeIdentifier;
		}
		else{
			throw new IllegalArgumentException("Cannot find a 'StorageHandler' for file type '" + newFileTypeIdentifier + "'");
		}
	}

	/**
	 * Use this method to access the encryption manager which will encrypt the document.
	 * @return the current file type identifier or {@code null} if the file is not encrypted.
	 */
	public EncryptionManager getEncryptionManager() {
		return encryptionManager;
	}

	/**
	 * @return the file attributes
	 */
	public Map<String, String> getFileAttributes() {
		return this.fileAttributes;
	}

	/**
	 * @return the file settings
	 */
	public Map<String, String> getFileSettings() {
		return this.fileSettings;
	}
}
