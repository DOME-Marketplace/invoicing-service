package it.eng.dome.invoicing.engine.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import it.eng.dome.invoicing.engine.model.InfoInvoicing;

@Component
public class StartupListener {

	private static final Logger logger = LoggerFactory.getLogger(StartupListener.class);

	private final String INFO_PATH = "/invoicing/info";
	private final RestTemplate restTemplate;

	@Value("${server.port}")
	private int serverPort;

	@Value("${server.servlet.context-path}")
	private String contextPath;

	public StartupListener(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onApplicationReady() {

		String path = contextPath + INFO_PATH;
		String url = "http://localhost:" + serverPort + path.replaceAll("//+", "/");

		logger.info("Listener GET call to {}", url);
		try {
			InfoInvoicing response = restTemplate.getForObject(url, InfoInvoicing.class);
			logger.info("Started the {} version: {} ", response.getName(), response.getVersion());

		} catch (Exception e) {
			logger.error("Error calling {}: {}", url, e.getMessage());
		}
	}
}
