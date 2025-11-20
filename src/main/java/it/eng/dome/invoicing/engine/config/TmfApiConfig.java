package it.eng.dome.invoicing.engine.config;

import it.eng.dome.brokerage.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.eng.dome.invoicing.engine.tmf.TmfApiFactory;


@Configuration
public class TmfApiConfig {
	
	private final Logger logger = LoggerFactory.getLogger(TmfApiConfig.class);
	
	@Autowired
	private TmfApiFactory tmfApiFactory;

	
	@Bean
    public ProductInventoryApis productInventoryApis() {
		logger.info("Initializing of ProductInventoryApis");
		
		return new ProductInventoryApis(tmfApiFactory.getTMF637ProductInventoryApiClient());
	}
	
	@Bean
    public APIPartyApis apiPartyApis() {
		logger.info("Initializing of APIPartyApis");
		
		return new APIPartyApis(tmfApiFactory.getTMF632PartyManagementApiClient());
	}

	@Bean
	public CustomerBillApis customerBillApis() {
		logger.info("Initializing of CustomerBillApis");

		return new CustomerBillApis(tmfApiFactory.getTMF678CustomerBillApiClient());
	}
	
	@Bean
	public AppliedCustomerBillRateApis appliedCustomerBillRateApis() {
		logger.info("Initializing of AppliedCustomerBillRateApis");

		return new AppliedCustomerBillRateApis(tmfApiFactory.getTMF678CustomerBillApiClient());
	}

	@Bean
	public ProductCatalogManagementApis productCatalogManagementApis() {
		logger.info("Initializing of ProductCatalogManagementApis");

		return new ProductCatalogManagementApis(tmfApiFactory.getTMF620ProductCatalogManagementApiClient());
	}

	@Bean
	public AccountManagementApis accountManagementApis() {
		logger.info("Initializing of AccountManagementApis");

		return new AccountManagementApis(tmfApiFactory.getTMF666AccountManagementApiClient());
	}
	

}
