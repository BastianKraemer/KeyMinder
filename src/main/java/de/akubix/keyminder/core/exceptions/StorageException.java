package de.akubix.keyminder.core.exceptions;


public class StorageException extends Exception {
	private static final long serialVersionUID = 1060331359956713524L;
	
	private StorageExceptionType reason;
	public StorageException(StorageExceptionType reason, String additionalnformations)
	{
		super(additionalnformations);
		this.reason = reason;
	}
	
	public StorageExceptionType getReason()
	{
		return reason;
	}
}
