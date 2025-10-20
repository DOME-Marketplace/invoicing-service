package it.eng.dome.invoicing.engine.service.exception;

import lombok.Getter;

/**
 * Custom exception raised when the Billing Engine service will not process the request due to something that is perceived to be
 * a client error 
 */
public class InvoicingBadRelatedPartyException extends RuntimeException{
	

	private static final long serialVersionUID = 1L;
	@Getter
	private String message;
	
	public InvoicingBadRelatedPartyException() {
		super();
	}
	
	public InvoicingBadRelatedPartyException(String msg) {
		super(msg);
	    this.message = msg;
	}
}
