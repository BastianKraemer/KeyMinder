package de.akubix.keyminder.core.io;

public class FileExtension {
	private final String extension, description;
	public FileExtension(String extension, String description) {
		this.extension = extension.startsWith(".") ? extension : "." + extension;
		this.description = description;
	}

	public String getExtension(){
		return this.extension;
	}

	public String getDescription(){
		return this.description;
	}
}
