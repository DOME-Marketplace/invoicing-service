package it.eng.dome.invoicing.engine.controller;

import java.io.IOException;
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
import it.eng.dome.brokerage.invoicing.dto.ApplyTaxesRequestDTO;
import it.eng.dome.invoicing.engine.service.TaxService;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBill;
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
     * The POST /invoicing/applyTaxes REST API is invoked to calculate the taxes that must be applied to the bill 
     * 
     * @param dto An {@link ApplyTaxesRequestDTO} with the {@link CustomerBill} and {@link AppliedCustomerBillingRate}(s) of a {@link Product} for which the taxes must be applied
     * @return a {@link BillingResponseDTO} containing the CustomerBill and the AppliedcustomerBillingRate updated with taxes
     */
    @PostMapping(value="/applyTaxes", consumes=MediaType.APPLICATION_JSON)
   	public ResponseEntity<BillingResponseDTO> applyTaxes(@RequestBody ApplyTaxesRequestDTO dto) {
   		try {

   			// 1) retrieve the Product, the CustomerBill and the AppliedCustomerBillingRate list from the ApplyTaxesRequestDTO
   			Product product = dto.getProduct();
   			Assert.state(!Objects.isNull(product), "Missing the instance of Product in the ApplyTaxesRequestDTO");

   			CustomerBill cb=dto.getCustomerBill();
   			Assert.state(!Objects.isNull(cb), "Missing the CustomerBill in the ApplyTaxesRequestDTO");
   			
   			List<AppliedCustomerBillingRate> bills = dto.getAppliedCustomerBillingRate();
   			Assert.state(!Objects.isNull(bills), "Missing the list of AppliedCustomerBillingRate in the ApplyTaxesRequestDTO");
   			
   	        // 2) calculate the taxes
   			//ApplyTaxesResponseDTO billsWithTaxes = taxService.applyTaxes(product, cb, bills);
   			BillingResponseDTO billsWithTaxes=taxService.applyTaxes(product, cb, bills);
            return ResponseEntity.ok(billsWithTaxes);
   		}
   		catch(Exception e) {
   			logger.error(e.getMessage());
   			logger.error(e.getStackTrace().toString());
   			
   			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
   		}
   	}
	
    /**
     * The POST /invoicing/previewTaxes REST API is invoked to calculate the price preview of a {@link ProductOrder} with taxes
     * 
     * @param order the {@link ProductOrder} to which the taxes must be applied
     * @return The ProductOrder updated with applied taxes
     */
    @PostMapping(value="/previewTaxes", consumes=MediaType.APPLICATION_JSON)
    public ResponseEntity<ProductOrder> previewTaxes(@RequestBody ProductOrder order){
        try {
			ProductOrder orderWithTaxes = taxService.applyTaxes(order);
            return ResponseEntity.ok(orderWithTaxes);
        } catch (Exception e) {
			logger.error("Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
	
}
