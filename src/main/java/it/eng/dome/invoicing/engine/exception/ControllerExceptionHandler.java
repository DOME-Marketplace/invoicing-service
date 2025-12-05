package it.eng.dome.invoicing.engine.exception;


import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import it.eng.dome.brokerage.exception.DefaultErrorResponse;
import it.eng.dome.brokerage.exception.ErrorResponse;
import it.eng.dome.brokerage.exception.IllegalEnumException;
import jakarta.servlet.http.HttpServletRequest;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ControllerExceptionHandler extends ResponseEntityExceptionHandler {
	
	private static final Logger logger = LoggerFactory.getLogger(ControllerExceptionHandler.class);
	
	// Exception to manage all IllegalEnumException in deserialize task
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
	        HttpMessageNotReadableException ex,
	        HttpHeaders headers,
	        HttpStatusCode status,
	        WebRequest request) {

		// generic fallback
        Throwable root = ex.getCause();
        String message = "Failed to read request"; // default message for handleHttpMessageNotReadable()


        while (root != null) {
            if (root instanceof IllegalEnumException iee) {
                message = iee.getMessage();
                break;
            }
            root = root.getCause();
        }

        HttpServletRequest httpRequest = ((ServletWebRequest) request).getRequest();
       
        //return buildResponseEntity(new ErrorResponse(httpRequest, HttpStatus.BAD_REQUEST, message));
        return buildDefaultResponseEntity(new DefaultErrorResponse(HttpStatus.BAD_REQUEST, message, URI.create(httpRequest.getRequestURI())));
    }

	@ExceptionHandler(IllegalArgumentException.class)
	protected ResponseEntity<Object> handleIllegalArgumentException(HttpServletRequest request, IllegalArgumentException ex) {
		return buildResponseEntity(new ErrorResponse(request, HttpStatus.BAD_REQUEST, ex));
	}

	private ResponseEntity<Object> buildResponseEntity(ErrorResponse errorResponse) {
		logger.error("buildResponseEntity {} - {}", errorResponse.getStatus(), errorResponse.getMessage());
		return new ResponseEntity<>(errorResponse, errorResponse.getStatus());
	}
	
	private ResponseEntity<Object> buildDefaultResponseEntity(DefaultErrorResponse defaultErrorResponse) {
		logger.error("buildDefaultResponseEntity {} - {}", defaultErrorResponse.getStatus(), defaultErrorResponse.getDetail());
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(defaultErrorResponse);
	}
}
