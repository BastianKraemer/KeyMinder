package de.akubix.keyminder.core.exceptions;

public class UserCanceledOperationException extends Exception {

	private static final long serialVersionUID = 7713625334672881110L;
	public UserCanceledOperationException(String message){
		super(message);
	}
}
