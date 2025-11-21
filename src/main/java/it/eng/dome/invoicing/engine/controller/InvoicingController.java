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
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/invoicing")

public class InvoicingController {
	
	private static final Logger logger = LoggerFactory.getLogger(InvoicingController.class);

    @Autowired
    private InvoicingService invoicingService;

    @GetMapping("invoices/{billId}")
    public ResponseEntity<Resource> getInvoice(@PathVariable String billId,
            @RequestParam(name = "format", required = false, defaultValue = "peppol") String format) {
        try {
            String fmt = (format == null || format.isBlank()) ? "peppol" : format.toLowerCase().trim();
            switch (fmt) {
                case "peppol", "xml", "peppol-xml":
                    String xml = invoicingService.getPeppolXml(billId).getContent();
                    Resource xmlResource = new ByteArrayResource(xml.getBytes());
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_XML)
                            .body(xmlResource);
                case "html":
                    String html = invoicingService.getPeppolHTML(billId).getContent();
                    Resource htmlResource = new ByteArrayResource(html.getBytes());
                    return ResponseEntity.ok()
                            .contentType(MediaType.TEXT_HTML)
                            .body(htmlResource);
                case "pdf":
                    Envelope<ByteArrayOutputStream> pdf = invoicingService.getPeppolPdf(billId);
                    ByteArrayResource pdfResource = new ByteArrayResource(pdf.getContent().toByteArray());
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    ContentDisposition.attachment()
                                            .filename(pdf.getName() + ".pdf")
                                            .build().toString())
                            .body(pdfResource);

                default:
                    String msg = "BAD REQUEST: Unsupported output format: " + fmt;
                    ByteArrayResource errorResource = new ByteArrayResource(msg.getBytes(StandardCharsets.UTF_8));
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(errorResource);
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
            String fmt = (format == null || format.isBlank()) ? "peppol" : format.toLowerCase().trim();
            InputStreamResource resource;
            String fileName;
            MediaType mediaType;

            switch (fmt) {
                case "peppol", "xml", "peppol-xml":
                    resource = invoicingService.getInvoicesXml(buyerId, sellerId, fromDate, toDate);
                    fileName = "invoices-xml.zip";
                    mediaType = MediaType.APPLICATION_OCTET_STREAM;
                    break;

                case "html":
                    resource = invoicingService.getInvoicesHtml(buyerId, sellerId, fromDate, toDate);
                    fileName = "invoices-html.zip";
                    mediaType = MediaType.APPLICATION_OCTET_STREAM;
                    break;

                case "pdf":
                    resource = invoicingService.getInvoicesPdf(buyerId, sellerId, fromDate, toDate);
                    fileName = "invoices-pdf.zip";
                    mediaType = MediaType.APPLICATION_OCTET_STREAM;
                    break;

                default:
                    String msg = "BAD REQUEST: Unsupported output format: " + fmt;
                    resource = new InputStreamResource(new ByteArrayInputStream(msg.getBytes(StandardCharsets.UTF_8)));
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(resource);
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                    .contentType(mediaType)
                    .body(resource);
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
