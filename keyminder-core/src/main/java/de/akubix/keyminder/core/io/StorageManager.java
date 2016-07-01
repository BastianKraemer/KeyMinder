/*	KeyMinder
	Copyright (C) 2015 Bastian Kraemer

	StorageManager.java

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
package de.akubix.keyminder.core.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class StorageManager {

	public static final String defaultFileType = "xml/keymindfile";

	public StorageManager(){
		addStorageHandler(
			defaultFileType,
			(String idetifier) -> {
				return new KeyMindFileHandler(idetifier);
			},
			new FileExtension(".keymind", "KeyMinder XML File (*.keymind)"),
			new FileExtension(".xml", "XML-File (*.xml)"));
	}

	private Map<String, StorageHandlerInstanceFabric> fileTypes = new HashMap<>();
	private Map<String, String> extensionTranslator = new HashMap<>();
	private List<FileExtension> knownFileExtensions = new ArrayList<>(8);

	/**
	 * Get the type identifier that has been assigned to a specific extension
	 * For example: the extension is ".keymind", the type identifier "xml/keymindfile"
	 * @param extension the file extension
	 * @param valueIfExtensionIsUnknwon the return value if the file extension is not known
	 * @return the identifier of a {@link StorageHandler} or 'valueIfExtensionIsUnknwon' if the extension is not known
	 */
	public String getIdentifierByExtension(String extension, String valueIfExtensionIsUnknwon){
		return extensionTranslator.getOrDefault(extension, valueIfExtensionIsUnknwon);
	}

	/**
	 * Check if a there is a {@link StorageHandler} for your file type identifier
	 * @param fileTypeIdentifier the file type identifier
	 * @return {@code true} if there is a storage handler for the file type, {@code false} if not
	 */
	public boolean hasStorageHandler(String fileTypeIdentifier){
		return fileTypes.containsKey(fileTypeIdentifier.toLowerCase());
	}

	/**
	 * Get the StorageHandler by using its identifier. For example  "xml/keymindfile" for the default file type
	 * @param fileTypeIdentifier the file type identifier
	 * @return the storage handler
	 * @throws IllegalArgumentException if the file type identifier does not exist
	 */
	public StorageHandler getStorageHandler(String fileTypeIdentifier) throws IllegalArgumentException {
		fileTypeIdentifier = fileTypeIdentifier.toLowerCase();
		if(!fileTypes.containsKey(fileTypeIdentifier)){
			throw new IllegalArgumentException(String.format("Cannot find storage hander with id \"%s\".", fileTypeIdentifier));
		}
		return fileTypes.get(fileTypeIdentifier).getInstance(fileTypeIdentifier);
	}

	/**
	 * Get the {@link StorageHandler} by using its identifier.
	 * If there is no StorageHandler defined for this identifier, the default identifier will be used ("xml/keymindfile")
	 * @param fileTypeIdentifier the file type identifier
	 * @return the {@link StorageHandler} of your file type or the default {@link StorageHandler} if the file type does not exist
	 */
	public StorageHandler getStorageHandlerIfAvailable(String fileTypeIdentifier){
		fileTypeIdentifier = fileTypeIdentifier.toLowerCase();
		if(!fileTypes.containsKey(fileTypeIdentifier)){fileTypeIdentifier = defaultFileType;}
		return fileTypes.get(fileTypeIdentifier).getInstance(fileTypeIdentifier);
	}

	/**
	 * Get the {@link StorageHandler} that has been assigned to a specific file extension
	 * @param fileExtension  the file type identifier
	 * @return the {@link StorageHandler} for this file type
	 * @throws IllegalArgumentException if the extension is not known
	 */
	public StorageHandler getStorageHandlerByExtension(String fileExtension) throws IllegalArgumentException{
		fileExtension = fileExtension.toLowerCase();
		if(!extensionTranslator.containsKey(fileExtension)){throw new IllegalArgumentException("Cannot find any storage hander that is assigned with this extension");}
		return getStorageHandler(extensionTranslator.get(fileExtension));
	}

	/**
	 * Adds a {@link StorageHandler}
	 * @param identifier for example "xml/keymindfile"
	 * @param instanceFabric A lambda expression or class which is able to create the StorageHandler object
	 * @param fileExtensions A list of all file extension that could be used with this StorageHandler
	 * @throws IllegalArgumentException if the identifier is already in use
	 */
	public void addStorageHandler(String identifier, StorageHandlerInstanceFabric instanceFabric, FileExtension... fileExtensions) throws IllegalArgumentException{
		identifier = identifier.toLowerCase();
		if(fileTypes.containsKey(identifier)){throw new IllegalArgumentException(String.format("Storage Handler with identifier \"%s\" does already exist.", identifier));}
		fileTypes.put(identifier, instanceFabric);

		for(int i = 0; i < fileExtensions.length; i++){
			String ext = fileExtensions[i].getExtension().toLowerCase();
			if(!extensionTranslator.containsKey(ext)){
				extensionTranslator.put(ext, identifier);
				knownFileExtensions.add(fileExtensions[i]);
			}
		}
	}

	/**
	 * You can use a lambda expression to walk through all known file types
	 * @param lambda the lambda expression
	 */
	public void forEachFileType(Consumer<? super String> lambda){
		fileTypes.keySet().forEach(lambda);
	}

	/**
	 * You can use a lambda expression to walk through all known file extensions an their assigned file types
	 * @param lambda the lambda expression
	 */
	public void forEachKnownExtension(BiConsumer<? super String, ? super String> lambda){
		extensionTranslator.forEach(lambda);
	}

	/**
	 * Returns a list of known file extensions
	 * @return the list of known file extensions
	 */
	public List<FileExtension> getKnownExtensions(){
		return knownFileExtensions;
	}
}

interface StorageHandlerInstanceFabric {
	public StorageHandler getInstance(String fileTypeIdentifier);
}
