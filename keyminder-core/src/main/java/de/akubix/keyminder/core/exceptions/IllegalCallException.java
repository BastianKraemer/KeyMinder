package de.akubix.keyminder.core.exceptions;

/**
 * This exception will be thrown if the fireEvent method won't be call as Java FX Thread, although the JavaFX UserInterface is in use
 */
public class IllegalCallException extends RuntimeException{
	private static final long serialVersionUID = 7608276453696074764L;
	public IllegalCallException(String message){
		super(message);
	}
}
