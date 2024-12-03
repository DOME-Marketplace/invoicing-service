package it.eng.dome.invoicing.service.controller;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;

@RestController
@RequestMapping("/invoicing")
@Tag(name = "Calculate Taxes Controller", description = "APIs to calculate the taxes for the ProductOrder")
public class CalculateTaxesController {
	
	private static final Logger logger = LoggerFactory.getLogger(CalculateTaxesController.class);
	
	@RequestMapping(value = "/applyTaxes", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> calculateOrderPrice(@RequestBody String orderJson) throws Throwable {
		logger.info("Received request for applying taxes for the invoicing");
		Assert.state(!StringUtils.isBlank(orderJson), "Missing the instance of ProductOrder in the request body");
		
		try {
			// 1) parse request body to ProductOrder
			ProductOrder order = ProductOrder.fromJson(orderJson);
			// 2) calculate the invoicing
			//TODO: implement the invoicing process
			logger.debug("Apply taxes to the ProductOrder id: {}", order.getId());

			// 3) return updated ProductOrder
			return new ResponseEntity<String>(order.toJson(), HttpStatus.OK);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			// Java exception is converted into HTTP status code
			throw new Exception(e);
		}
	}

}
