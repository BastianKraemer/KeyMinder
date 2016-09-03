package de.akubix.keyminder.core.exceptions;


public class InvalidValueException extends Exception {

	private static final long serialVersionUID = 5912876301834202013L;

	public InvalidValueException(String message) {
		super(message);
	}

	public InvalidValueException(String message, Throwable cause) {
		super(message, cause);
	}
}
