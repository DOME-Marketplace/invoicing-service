package it.eng.dome.invoicing.engine.controller;

import it.eng.dome.invoicing.engine.exception.ExternalServiceException;
import it.eng.dome.invoicing.engine.exception.PeppolValidationException;
import it.eng.dome.invoicing.engine.service.InvoicingService;
import it.eng.dome.invoicing.engine.service.render.Envelope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/invoicing")

public class InvoicingController {
	
	private static final Logger logger = LoggerFactory.getLogger(InvoicingController.class);

//    @Autowired
//    private BomService bomService;
    
    @Autowired
    private InvoicingService invoicingService;

    @Autowired
//    private ObjectMapper jacksonObjectMapper;

    @GetMapping("invoices/{billId}")
    public ResponseEntity<Resource> getInvoice(@PathVariable String billId,
            @RequestParam(name = "format", required = false, defaultValue = "peppol") String format) {
        try {
            if (format == null || format.isEmpty() || "peppol".equalsIgnoreCase(format)
                    || "peppol-xml".equalsIgnoreCase(format) || "xml".equalsIgnoreCase(format)) {
                String xml = invoicingService.getPeppolXml(billId);
                Resource resource = new ByteArrayResource(xml.getBytes());
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_XML)
                        .body(resource);
            }
            else if("html".equalsIgnoreCase(format)) {
                String html = this.invoicingService.getPeppolHTML(billId).getContent();
                Resource resource = new ByteArrayResource(html.getBytes());
                return ResponseEntity.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(resource);
            }
            else if("pdf".equalsIgnoreCase(format)) {
                Envelope<ByteArrayOutputStream> pdf = this.invoicingService.getPeppolPdf(billId);
                ByteArrayResource resource = new ByteArrayResource(pdf.getContent().toByteArray());
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                ContentDisposition.attachment()
                                    .filename(pdf.getName()+".pdf")
                                    .build().toString())
                        .body(resource);
            }
            else {
                String msg = "BAD REQUEST: Unsupported output format: " + format;
                ByteArrayResource resource = new ByteArrayResource(msg.getBytes());
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(resource);
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
            }
            else { 
                String msg = "BAD REQUEST: Unsupported output format: " + format;
                InputStreamResource res = new InputStreamResource(new ByteArrayInputStream(msg.getBytes(StandardCharsets.UTF_8)));
                return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(res);
            }
            /*
            } else {
                // Probably no longer neeeded. To check.
                LocalResourceRef ref = this.invoicingService.getPackagedInvoices(sellerId, buyerId, fromDate, toDate, format);
                InputStream in = getClass().getResourceAsStream(ref.getLocation());
                return ResponseEntity.ok()
                        .contentType(ref.getContentType()).body(new InputStreamResource(in));
            }
            */
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
