package it.eng.dome.invoicing.engine.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.eng.dome.brokerage.observability.health.Health;
import it.eng.dome.brokerage.observability.info.Info;
import it.eng.dome.invoicing.engine.service.HealthService;


@RestController
@RequestMapping("/invoicing")
@Tag(name = "Invoicing Service Controller", description = "APIs to manage the invoicing-service")
public class InfoInvoicingController {
	
	private static final Logger logger = LoggerFactory.getLogger(InfoInvoicingController.class);

    @Autowired
    private HealthService healthService;
    
    @GetMapping("/info")
    public ResponseEntity<Info> getInfo() {
    	logger.info("Request getInfo()");
    	
        try {
            Info info = this.healthService.getInfo();
           
            return ResponseEntity.ok(info);
            
        } catch (Exception e) {
        	logger.warn("Failed to serialize Info: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Health> getHealth() {
    	logger.info("Request getHealth()");
    	
        try {
            Health health = this.healthService.getHealth();
                                   
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
        	logger.warn("Failed to serialize Health: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
