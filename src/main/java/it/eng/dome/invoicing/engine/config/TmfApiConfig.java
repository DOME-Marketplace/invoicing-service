package it.eng.dome.invoicing.engine.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import it.eng.dome.brokerage.api.APIPartyApis;
import it.eng.dome.brokerage.api.ProductInventoryApis;
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
	
}
