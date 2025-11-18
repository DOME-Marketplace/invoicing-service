package it.eng.dome.invoicing.engine.controller;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import it.eng.dome.invoicing.engine.model.InvoiceBom;
import it.eng.dome.invoicing.engine.service.BomService;
import it.eng.dome.invoicing.engine.service.InvoicingService;
import it.eng.dome.invoicing.engine.service.PeppolPlaceholder;
import it.eng.dome.invoicing.engine.service.render.LocalResourceRef;
import jakarta.ws.rs.QueryParam;

@RestController
@RequestMapping("/invoicing")

public class InvoicingController {
	
	private static final Logger logger = LoggerFactory.getLogger(InvoicingController.class);

    @Autowired
    private BomService bomService;
    
    @Autowired
    private InvoicingService invoicingService;

    @Autowired
    private ObjectMapper jacksonObjectMapper;

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
    
    @GetMapping("invoices/{billId}")
    public ResponseEntity<String> getInvoice(@PathVariable String billId,  @QueryParam("format") String format) {
        try {
            if(format==null || format.isEmpty() || "peppol".equalsIgnoreCase(format)) {
                // TODO
                return ResponseEntity.ok("peppol");
            }
            else {
                // TODO
                return ResponseEntity.ok("non-peppol");
            }
        } catch (Exception e) {            
            logger.error("Error retrieving BOM for {}", billId);
            logger.error(e.getLocalizedMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("invoices")
    public ResponseEntity<InputStreamResource> getInvoices(@QueryParam("sellerId") String sellerId, @QueryParam("buyerId") String buyerId, @QueryParam("format") String format, @QueryParam("fromDate") OffsetDateTime fromDate, @QueryParam("toDate") OffsetDateTime toDate) {
        try {

            if(format==null || format.isEmpty() || "peppol".equalsIgnoreCase(format)) {
                Collection<PeppolPlaceholder> peppols = this.invoicingService.getPeppolInvoices(sellerId, buyerId, fromDate, toDate);
                InputStream in = new ByteArrayInputStream(jacksonObjectMapper.writeValueAsBytes(peppols));
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(new InputStreamResource(in));
            }
            else {
                LocalResourceRef ref = this.invoicingService.getPackagedInvoices(sellerId, buyerId, fromDate, toDate, format);
                InputStream in = getClass().getResourceAsStream(ref.getLocation());
                return ResponseEntity.ok().contentType(ref.getContentType()).body(new InputStreamResource(in));
            }

        } catch (Exception e) {            
            logger.error(e.getLocalizedMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }        

}
