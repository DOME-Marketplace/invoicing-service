package it.eng.dome.invoicing.engine.controller;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.dome.brokerage.invoicing.dto.ApplyTaxesRequestDTO;
import it.eng.dome.invoicing.engine.rate.TaxService;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.JSON;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;

@RestController
@RequestMapping("/invoicing")
@Tag(name = "Calculate Taxes Controller", description = "APIs to calculate the taxes for the ProductOrder")
public class CalculateTaxesController {
	
	protected static final Logger logger = LoggerFactory.getLogger(CalculateTaxesController.class);

	@Autowired
	protected TaxService taxService;
	

	//TODO: remove
	/*@RequestMapping(value = "/applyTaxes", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> applyTaxes(@RequestBody String billsJson) throws Throwable {
		logger.info("Received request for applying taxes to a bill");
		Assert.state(!StringUtils.isBlank(billsJson), "Missing the instance of AppliedCustomerBillRate in the request body");
		try {
			// 1) parse request body to Bills
			// FIXME: replace the following with deserialization of the DTO, extract bills and product.
			AppliedCustomerBillingRate[] bills = JSON.getGson().fromJson(billsJson, AppliedCustomerBillingRate[].class);
			Product product = null;
			// 2) calculate the taxes
			AppliedCustomerBillingRate[] billsWithTaxes = taxService.applyTaxes(product, bills);
			// 3) return updated AppliedCustomerBillingRate
			return new ResponseEntity<String>(JSON.getGson().toJson(billsWithTaxes), HttpStatus.OK);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			// Java exception is converted into HTTP status code
			throw new Exception(e);
		}
		
	}*/
	
	@RequestMapping(value = "/applyTaxes", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> applyTaxes(@RequestBody ApplyTaxesRequestDTO dto) throws Throwable {
		logger.info("Received request for applying taxes to a bill");
		
		AppliedCustomerBillingRate[] bills;
		Product product;
		
		try {
			// 1) retrieve the Product and the AppliedCustomerBillingRate list from the ApplyTaxesRequestDTO
			product = dto.getProduct();
			Assert.state(!Objects.isNull(product),  "Missing the instance of Product in the ApplyTaxesRequestDTO");
			
			bills=(AppliedCustomerBillingRate[]) dto.getAppliedCustomerBillingRateList().toArray();
			Assert.state(!Objects.isNull(bills),  "Missing the list of AppliedCustomerBillingRate in the ApplyTaxesRequestDTO");
			
			// 2) calculate the taxes
			AppliedCustomerBillingRate[] billsWithTaxes = taxService.applyTaxes(product, bills);
			// 3) return updated AppliedCustomerBillingRate
			return new ResponseEntity<String>(JSON.getGson().toJson(billsWithTaxes), HttpStatus.OK);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			// Java exception is converted into HTTP status code
			throw new Exception(e);
		}
		
	}


	@RequestMapping(value = "/previewTaxes", method = RequestMethod.POST, produces = "application/json", consumes = "application/json")
    public ResponseEntity<String> previewTaxes(@RequestBody String orderJson) throws Throwable {
		logger.info("Received request for applying taxes for previewing taxes");
		Assert.state(!StringUtils.isBlank(orderJson), "Missing the instance of ProductOrder in the request body");
		
		try {
			// 1) parse request body to ProductOrder
			ProductOrder order = ProductOrder.fromJson(orderJson);
			// 2) calculate the invoicing
			ProductOrder orderWithTaxes = taxService.applyTaxes(order);
			// 3) return updated ProductOrder
			return new ResponseEntity<String>(orderWithTaxes.toJson(), HttpStatus.OK);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			// Java exception is converted into HTTP status code
			throw new Exception(e);
		}
	}

}
