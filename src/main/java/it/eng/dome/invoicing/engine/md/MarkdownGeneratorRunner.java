package it.eng.dome.invoicing.engine.md;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import it.eng.dome.invoicing.engine.listener.StartupListener;
import it.eng.dome.invoicing.engine.model.InfoInvoicing;

import java.nio.charset.StandardCharsets;

//@Component
public class MarkdownGeneratorRunner {

	private static final Logger logger = LoggerFactory.getLogger(MarkdownGeneratorRunner.class);
	
    private final OpenApiMarkdownGenerator generator;
    
	private final String API_DOCS_PATH = "/v3/api-docs";
	private final RestTemplate restTemplate;	

	@Value("${server.port}")
	private int serverPort;
	
	@Value("${server.servlet.context-path}")
	private String contextPath;

    public MarkdownGeneratorRunner(OpenApiMarkdownGenerator generator, RestTemplate restTemplate) {
        this.generator = generator;
        this.restTemplate = restTemplate;
    }

    //@PostConstruct
    public void run() throws Exception {
    	
    	String path = contextPath + API_DOCS_PATH;
		String url = "http://localhost:" + serverPort + path.replaceAll("//+", "/");
		
		logger.info("GET call to {}", url);
		
		String json = restTemplate.getForObject(url, String.class);
    	
		logger.info(json);
        
        //generator.generateMarkdownFromJson(json, "APIs.md");
    }
}