package it.eng.dome.invoicing.engine.tmf;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import it.eng.dome.brokerage.billing.utils.UrlPathUtils;

@Component(value = "tmfApiFactory")
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
public final class TmfApiFactory implements InitializingBean {
	
	private static final Logger log = LoggerFactory.getLogger(TmfApiFactory.class);
	private static final String TMF_ENDPOINT_CONCAT_PATH = "-";
	
    @Value("${tmforumapi.tmf_endpoint}")
    public String tmfEndpoint;
    
    @Value("${tmforumapi.tmf_envoy}")
    public boolean tmfEnvoy;
    
    @Value("${tmforumapi.tmf_namespace}")
    public String tmfNamespace;
    
    @Value("${tmforumapi.tmf_postfix}")
    public String tmfPostfix;    
    
    @Value("${tmforumapi.tmf_port}")
    public String tmfPort;

	@Value( "${tmforumapi.tmf632_party_management_path}" )
	private String tmf632PartyManagementPath;


	public it.eng.dome.tmforum.tmf632.v4.ApiClient getTMF632PartyManagementApiClient() {
		final it.eng.dome.tmforum.tmf632.v4.ApiClient apiClient = it.eng.dome.tmforum.tmf632.v4.Configuration.getDefaultApiClient();
		
		String basePath = tmfEndpoint;
		if (!tmfEnvoy) { // no envoy specific path
			basePath += TMF_ENDPOINT_CONCAT_PATH + "party-catalog" + "." + tmfNamespace + "." + tmfPostfix + ":" + tmfPort;
		}
		
		apiClient.setBasePath(basePath + "/" + tmf632PartyManagementPath);
		log.debug("Invoke Catalog API at endpoint: " + apiClient.getBasePath());
		return apiClient;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		
		log.info("Billing Engine is using the following AccessNode endpoint: " + tmfEndpoint);	
		if (tmfEnvoy) {
			log.info("You set the apiProxy for TMForum endpoint. No tmf_port {} can be applied", tmfPort);	
		} else {
			log.info("No apiProxy set for TMForum APIs. You have to access on specific software via paths at tmf_port {}", tmfPort);	
		}
		
		Assert.state(!StringUtils.isBlank(tmfEndpoint), "Billing Engine not properly configured. tmf620_catalog_base property has no value.");
		Assert.state(!StringUtils.isBlank(tmf632PartyManagementPath), "Billing Engine not properly configured. tmf632_party_management_path property has no value.");

			
		if (tmfEndpoint.endsWith("/")) {
			tmfEndpoint = UrlPathUtils.removeFinalSlash(tmfEndpoint);		
		}
		
		if (tmf632PartyManagementPath.startsWith("/")) {
			tmf632PartyManagementPath = UrlPathUtils.removeInitialSlash(tmf632PartyManagementPath);
		}	

	}

}
