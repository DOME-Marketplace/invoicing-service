package it.eng.dome.invoicing.engine.exception;

public class ExternalServiceException extends Exception {

	private static final long serialVersionUID = 1916953345575799813L;

	public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ExternalServiceException(String message) {
		super(message);
	}
}
