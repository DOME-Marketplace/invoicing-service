package it.eng.dome.invoicing.engine.controller;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
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
import it.eng.dome.brokerage.api.ProductApis;
import it.eng.dome.invoicing.engine.service.TaxService;
import it.eng.dome.invoicing.engine.tmf.TmfApiFactory;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.JSON;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import jakarta.ws.rs.core.MediaType;

@JsonInclude(JsonInclude.Include.NON_NULL)
class LocalApplyTaxesRequestDTO {
	
	@JsonProperty("product")
	private Product product;
   
	@JsonProperty("appliedCustomerBillingRate")
	private List<AppliedCustomerBillingRate> appliedCustomerBillingRate;

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public List<AppliedCustomerBillingRate> getAppliedCustomerBillingRate() {
		return appliedCustomerBillingRate;
	}

	public void setAppliedCustomerBillingRate(List<AppliedCustomerBillingRate> appliedCustomerBillingRate) {
		this.appliedCustomerBillingRate = appliedCustomerBillingRate;
	}
}

@RestController
@RequestMapping("/invoicing")
@Tag(name = "Calculate Taxes Controller", description = "APIs to calculate the taxes for the ProductOrder")
public class CalculateTaxesController implements InitializingBean{
	
	protected static final Logger logger = LoggerFactory.getLogger(CalculateTaxesController.class);

	@Autowired
	protected TaxService taxService;
	
	@Autowired
	private TmfApiFactory tmfApiFactory;
	
	private ProductApis producApis;

	@Override
	public void afterPropertiesSet() throws Exception {
		this.producApis = new ProductApis(tmfApiFactory.getTMF637ProductInventoryApiClient());
	}

    @PostMapping(value="/applyTaxes", consumes=MediaType.APPLICATION_JSON)
	public ResponseEntity<List<AppliedCustomerBillingRate>> applyTaxes(@RequestBody LocalApplyTaxesRequestDTO dto) {
		try {
//			LocalApplyTaxesRequestDTO dto = JSON.deserialize(applyTaxesDTO, LocalApplyTaxesRequestDTO.class);

			// 1) retrieve the Product and the AppliedCustomerBillingRate list from the ApplyTaxesRequestDTO
			Product product = producApis.getProduct(dto.getProduct().getId(), null);			
			Assert.state(!Objects.isNull(product), "Missing the instance of Product in the ApplyTaxesRequestDTO");

			List<AppliedCustomerBillingRate> bills = dto.getAppliedCustomerBillingRate();
			Assert.state(!Objects.isNull(bills), "Missing the list of AppliedCustomerBillingRate in the ApplyTaxesRequestDTO");
			
	        // 2) calculate the taxes
			List<AppliedCustomerBillingRate> billsWithTaxes = taxService.applyTaxes(product, bills);
            return ResponseEntity.ok(billsWithTaxes);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
    @PostMapping(value="/previewTaxes", consumes=MediaType.APPLICATION_JSON)
    public ResponseEntity<ProductOrder> previewTaxes(@RequestBody ProductOrder order) throws IOException {
        try {
			ProductOrder orderWithTaxes = taxService.applyTaxes(order);
            return ResponseEntity.ok(orderWithTaxes);
        } catch (Exception e) {
			logger.error("Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
	
	/*
	@RequestMapping(value = "/applyTaxes", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> applyTaxes(@RequestBody String applyTaxesRequestDTO) throws Throwable {
		logger.info("Received request for applying taxes to a bill");

		ApplyTaxesRequestDTO dto = JSON.deserialize(toLowerCaseStatus(applyTaxesRequestDTO), ApplyTaxesRequestDTO.class);

		AppliedCustomerBillingRate[] bills;
		Product product;

		try {
			// 1) retrieve the Product and the AppliedCustomerBillingRate list from the ApplyTaxesRequestDTO
			//product = dto.getProduct();	
			logger.debug("Trying to get product with Id: {}",  dto.getProduct().getId());
			product = producApis.getProduct(dto.getProduct().getId(), null);			
			Assert.state(!Objects.isNull(product), "Missing the instance of Product in the ApplyTaxesRequestDTO");

			bills = dto.getAppliedCustomerBillingRate().toArray(new AppliedCustomerBillingRate[0]);
			Assert.state(!Objects.isNull(bills), "Missing the list of AppliedCustomerBillingRate in the ApplyTaxesRequestDTO");
			
	        // 2) calculate the taxes
			AppliedCustomerBillingRate[] billsWithTaxes = taxService.applyTaxes(product, bills);
			
			// 3) return updated AppliedCustomerBillingRate
			return new ResponseEntity<String>(JSON.getGson().toJson(billsWithTaxes), HttpStatus.OK);
		} catch (Exception e) {
			logger.error("Error: {}", e.getMessage());
			// Java exception is converted into HTTP status code
			throw new Exception(e);
		}
		
	}

	@RequestMapping(value = "/previewTaxes", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
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
			logger.error("Error: {}", e.getMessage());
			// Java exception is converted into HTTP status code
			throw new Exception(e);
		}
	}

	//TODO workaround to set the status value in lowercase
	private String toLowerCaseStatus(String json) {
		ObjectMapper objectMapper = new ObjectMapper();
		String lower = json;
		 try {
			 ObjectNode rootNode = (ObjectNode) objectMapper.readTree(json);
			 JsonNode statusNode = rootNode.at("/product/status");
			 if (!statusNode.isMissingNode()) {
				 String status = statusNode.asText();
				 ((ObjectNode) rootNode.at("/product")).put("status", status.toLowerCase());
			 }
			 return objectMapper.writeValueAsString(rootNode);

		} catch (Exception e) {			
			return lower;
		}
	}
	*/

}
