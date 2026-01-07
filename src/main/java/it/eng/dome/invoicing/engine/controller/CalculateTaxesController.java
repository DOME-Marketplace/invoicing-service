package it.eng.dome.invoicing.engine.controller;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.dome.brokerage.api.ProductInventoryApis;
import it.eng.dome.brokerage.billing.dto.BillingResponseDTO;
import it.eng.dome.brokerage.exception.DefaultErrorResponse;
import it.eng.dome.brokerage.exception.ErrorResponse;
import it.eng.dome.brokerage.model.Invoice;
import it.eng.dome.invoicing.engine.service.TaxService;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;

@SuppressWarnings("unused")
@RestController
@RequestMapping("/invoicing")
@Tag(name = "Calculate Taxes Controller", description = "APIs to calculate the taxes for the ProductOrder")
public class CalculateTaxesController {
	
	protected static final Logger logger = LoggerFactory.getLogger(CalculateTaxesController.class);

	@Autowired
	protected TaxService taxService;
	
    /**
     * The REST API POST /invoicing/applyTaxes is invoked to calculate the taxes that must be applied to the bill 
     * 
     * @param invoices A list of {@link Invoice} to which the taxes must be applied
     * @return A list of {@link Invoice} with taxes
     */
    @PostMapping(value="/applyTaxes", consumes=MediaType.APPLICATION_JSON)
   	public ResponseEntity<?> applyTaxes(@RequestBody List<Invoice> invoices, HttpServletRequest request) {
   		try {

   			if(invoices!=null && !invoices.isEmpty()) {
   				List<Invoice> invoicesWithTaxes= taxService.applyTaxes(invoices);
   				return ResponseEntity.ok(invoicesWithTaxes);
   			}else {
   				return ResponseEntity.ok(invoices);
   			}
   		}
   		catch(Exception e) {
			logger.error("Error in applyTaxes: {}", e.getMessage());

			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ErrorResponse(request, HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage()));
   		}
   	}
	
    /**
     * The POST /invoicing/previewTaxes REST API is invoked to calculate the price preview of a {@link ProductOrder} with taxes
     * 
     * @param order the {@link ProductOrder} to which the taxes must be applied
     * @return The ProductOrder updated with applied taxes
     */
	@PostMapping(value = "/previewTaxes", consumes = MediaType.APPLICATION_JSON)
	public ResponseEntity<?> previewTaxes(@RequestBody ProductOrder order, HttpServletRequest request) {
		try {
			ProductOrder orderWithTaxes = taxService.applyTaxes(order);
			return ResponseEntity.ok(orderWithTaxes);
		} catch (Exception e) {
			logger.error("Error in previewTaxes: {}", e.getMessage());
			
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new DefaultErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), URI.create(request.getRequestURI())));
		}
	}
	
}
