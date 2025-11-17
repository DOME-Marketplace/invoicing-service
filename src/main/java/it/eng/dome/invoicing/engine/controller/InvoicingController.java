package it.eng.dome.invoicing.engine.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.eng.dome.invoicing.engine.model.InvoiceBom;
import it.eng.dome.invoicing.engine.service.BomService;

@RestController
@RequestMapping("/invoicing")

public class InvoicingController {
	
	private static final Logger logger = LoggerFactory.getLogger(InvoicingController.class);

    @Autowired
    private BomService bomService;
    

    @GetMapping("invoices/{billId}/dev/bom")
    public ResponseEntity<InvoiceBom> getInvoiceBom(@PathVariable String billId) {
        try {
            InvoiceBom bom = bomService.getBomFor(billId);
            return ResponseEntity.ok(bom);
        } catch (Exception e) {            
            logger.error("Error retrieving BOM for {}", billId);
            logger.error(e.getLocalizedMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }    
     

}
