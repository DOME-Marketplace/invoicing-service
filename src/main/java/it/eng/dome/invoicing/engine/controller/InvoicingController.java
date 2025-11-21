package it.eng.dome.invoicing.engine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.eng.dome.invoicing.engine.exception.ExternalServiceException;
import it.eng.dome.invoicing.engine.exception.PeppolValidationException;
import it.eng.dome.invoicing.engine.model.InvoiceBom;
import it.eng.dome.invoicing.engine.service.BomService;
import it.eng.dome.invoicing.engine.service.InvoicingService;
import it.eng.dome.invoicing.engine.service.render.LocalResourceRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.time.OffsetDateTime;

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
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
    
    @GetMapping("invoices/{billId}")
    public ResponseEntity<String> getInvoice(
            @PathVariable String billId,
            @RequestParam(name = "format", required = false, defaultValue = "peppol") String format) {
        try {
            if (format == null || format.isEmpty() || "peppol".equalsIgnoreCase(format)
                    || "peppol-xml".equalsIgnoreCase(format) || "xml".equalsIgnoreCase(format)) {
                String xml = invoicingService.getPeppolXml(billId);
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_XML)
                        .body(xml);
            }
            else if("html".equalsIgnoreCase(format)) {
                String html = this.invoicingService.getPeppolHTML(billId);
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(html);
            }
            else {
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("BAD REQUEST: Unsupported output format: " + format);
            }
        } catch (PeppolValidationException e) {
            logger.error("PEPPOL validation problem for billId {}: {}", billId, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .build();
        } catch (ExternalServiceException e) {
            // External service failed, status 503
            logger.error("External service error for billId {}: {}", billId, e.getMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.SERVICE_UNAVAILABLE)
                    .build();
        } catch (Exception e) {            
            logger.error("Error retrieving BOM for {}", billId);
            logger.error(e.getLocalizedMessage(), e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }

    @GetMapping("invoices")
    public ResponseEntity<InputStreamResource> getInvoices(
            @RequestParam(name = "sellerId", required = false) String sellerId,
            @RequestParam(name = "buyerId", required = false) String buyerId,
            @RequestParam(name = "format", required = false, defaultValue = "peppol") String format,
            @RequestParam(name = "fromDate", required = false) OffsetDateTime fromDate,
            @RequestParam(name = "toDate", required = false) OffsetDateTime toDate) {
        try {

            if (format == null || format.isEmpty() || "peppol".equalsIgnoreCase(format)) {
                InputStreamResource resource = invoicingService.getInvoicesZip(buyerId, sellerId, fromDate, toDate);

                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoices.zip")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .body(resource);
            } else {
                // Probably no longer neeeded. To check.
                LocalResourceRef ref = this.invoicingService.getPackagedInvoices(sellerId, buyerId, fromDate, toDate, format);
                InputStream in = getClass().getResourceAsStream(ref.getLocation());
                return ResponseEntity.ok()
                        .contentType(ref.getContentType()).body(new InputStreamResource(in));
            }
        } catch (ExternalServiceException e) {
            logger.error("External service error for invoices {}-{}: {}", buyerId, sellerId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .build();
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
}
