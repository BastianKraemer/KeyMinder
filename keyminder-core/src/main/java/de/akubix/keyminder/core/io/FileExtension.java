package de.akubix.keyminder.core.io;

public class FileExtension {
	private final String extension, description;

	/**
	 * Create a new file extension
	 * @param extension The file extension (for example <b>*.txt</b>)
	 * @param description The description if this file extension
	 */
	public FileExtension(String extension, String description) {
		this.extension = extension;
		this.description = description;
	}

	/**
	 * Gets the extension
	 * @return the file extension
	 */
	public String getExtension(){
		return this.extension;
	}

	/**
	 * Gets the description
	 * @return the file description
	 */
	public String getDescription(){
		return this.description;
	}
}
