package it.eng.dome.invoicing.engine.exception;


import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import it.eng.dome.brokerage.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {
	
	@ExceptionHandler(InvoicingBadRelatedPartyException.class)
	protected ResponseEntity<Object> handleBillingBadRequestException(HttpServletRequest request,InvoicingBadRelatedPartyException ex) {
		return buildResponseEntity(new ErrorResponse(request,HttpStatus.BAD_REQUEST, ex));
	}
	
	@ExceptionHandler(IllegalArgumentException.class)
	protected ResponseEntity<Object> handleIllegalArgumentException(HttpServletRequest request, IllegalArgumentException ex) {
		return buildResponseEntity(new ErrorResponse(request,HttpStatus.BAD_REQUEST, ex));
	}

	private ResponseEntity<Object> buildResponseEntity(ErrorResponse errorResponse) {
		return new ResponseEntity<>(errorResponse, errorResponse.getStatus());
	}
	
}
