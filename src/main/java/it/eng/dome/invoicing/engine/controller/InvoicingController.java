package it.eng.dome.invoicing.engine.controller;

import it.eng.dome.invoicing.engine.exception.ExternalServiceException;
import it.eng.dome.invoicing.engine.exception.PeppolValidationException;
import it.eng.dome.invoicing.engine.service.InvoicingService;
import it.eng.dome.invoicing.engine.service.render.Envelope;
import it.eng.dome.invoicing.engine.service.utils.NamingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/invoicing")
@Tag(name = "Get Invoices Controller", description = "Get Invoices in XML and HTML formats")
public class InvoicingController {

    private static final Logger logger = LoggerFactory.getLogger(InvoicingController.class);

    private static final MediaType APPLICATION_ZIP = MediaType.parseMediaType("application/zip");

    @Autowired
    private InvoicingService invoicingService;

    /**
     * Helper method to build a binary ZIP response.
     * Uses InputStreamResource to ensure binary stream is preserved through proxies.
     */
    private ResponseEntity<InputStreamResource> buildZipResponse(byte[] zipBytes, String fileName) {
        InputStreamResource resource = new InputStreamResource(new ByteArrayInputStream(zipBytes));

        return ResponseEntity.ok()
                .contentType(APPLICATION_ZIP)
                .contentLength(zipBytes.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName)
                                .build().toString())
                .header("Content-Transfer-Encoding", "binary")
                .header("X-Content-Type-Options", "nosniff")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .body(resource);
    }

    @GetMapping("invoices/{billId}")
    public ResponseEntity<?> getInvoice(@PathVariable String billId,
            @RequestParam(name = "format", required = false, defaultValue = "peppol") String format) {
        try {
            String fmt = (format == null || format.isBlank()) ? "peppol" : format.toLowerCase().trim();

            switch (fmt) {
                case "peppol", "xml", "peppol-xml": {
                    Envelope<String> peppolXmlEnvelope = invoicingService.getPeppolXml(billId);
                    String xml = peppolXmlEnvelope.getContent();
                    Resource xmlResource = new ByteArrayResource(xml.getBytes(StandardCharsets.UTF_8));
                    String fileName = NamingUtils.sanitizeFilename(peppolXmlEnvelope.getName()) + ".xml";
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_XML)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    ContentDisposition.attachment()
                                            .filename(fileName)
                                            .build().toString())
                            .body(xmlResource);
                }

                case "html": {
                    Envelope<String> peppolHtmlEnvelope = invoicingService.getPeppolHTML(billId);
                    String html = peppolHtmlEnvelope.getContent();
                    Resource htmlResource = new ByteArrayResource(html.getBytes(StandardCharsets.UTF_8));
                    String fileName = NamingUtils.sanitizeFilename(peppolHtmlEnvelope.getName()) + ".html";
                    return ResponseEntity.ok()
                            .contentType(MediaType.TEXT_HTML)
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    ContentDisposition.attachment()
                                            .filename(fileName)
                                            .build().toString())
                            .body(htmlResource);
                }

                case "pdf": {
                    Envelope<ByteArrayOutputStream> pdf = invoicingService.getPeppolPdf(billId);
                    InputStreamResource pdfResource = new InputStreamResource(
                            new ByteArrayInputStream(pdf.getContent().toByteArray()));
                    String fileName = NamingUtils.sanitizeFilename(pdf.getName()) + ".pdf";
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_PDF)
                            .contentLength(pdf.getContent().size())
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    ContentDisposition.attachment()
                                            .filename(fileName)
                                            .build().toString())
                            .header("Content-Transfer-Encoding", "binary")
                            .body(pdfResource);
                }

                case "xml-html": {
                    Envelope<String> xmlEnvelope = invoicingService.getPeppolXml(billId);
                    String baseName = NamingUtils.sanitizeFilename(xmlEnvelope.getName());

                    byte[] zipBytes = invoicingService.getInvoiceXmlAndHtmlFormats(billId);
                    String fileName = baseName + "-xml-html.zip";

                    logger.info("Returning xml-html ZIP for billId {}: {} bytes", billId, zipBytes.length);

                    return buildZipResponse(zipBytes, fileName);
                }

                case "all": {
                    Envelope<String> xmlEnvelope = invoicingService.getPeppolXml(billId);
                    String baseName = NamingUtils.sanitizeFilename(xmlEnvelope.getName());

                    byte[] zipBytes = invoicingService.getInvoiceAllFormats(billId);
                    String fileName = baseName + "-all.zip";

                    logger.info("Returning all formats ZIP for billId {}: {} bytes", billId, zipBytes.length);

                    return buildZipResponse(zipBytes, fileName);
                }

                default: {
                    String msg = "BAD REQUEST: Unsupported output format: " + fmt;
                    ByteArrayResource errorResource = new ByteArrayResource(msg.getBytes(StandardCharsets.UTF_8));
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(errorResource);
                }
            }
        } catch (PeppolValidationException e) {
            logger.error("PEPPOL validation problem for billId {}: {}", billId, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .build();
        } catch (ExternalServiceException e) {
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
    public ResponseEntity<?> getInvoices(
            @RequestParam(name = "sellerId", required = false) String sellerId,
            @RequestParam(name = "buyerId", required = false) String buyerId,
            @RequestParam(name = "format", required = false, defaultValue = "peppol") String format,
            @RequestParam(name = "fromDate", required = false) OffsetDateTime fromDate,
            @RequestParam(name = "toDate", required = false) OffsetDateTime toDate) {
        try {
            String fmt = (format == null || format.isBlank()) ? "peppol" : format.toLowerCase().trim();
            byte[] zipBytes;
            String fileName;

            switch (fmt) {
                case "peppol":
                case "xml":
                case "peppol-xml": {
                    zipBytes = invoicingService.getInvoicesXml(buyerId, sellerId, fromDate, toDate);
                    var xmls = invoicingService.getPeppolsXml(buyerId, sellerId, fromDate, toDate);
                    String baseName = NamingUtils.sanitizeFilename(
                            NamingUtils.extractFileNameFromEnvelopes(xmls));
                    fileName = baseName + "-xml.zip";
                    logger.info("Returning XML ZIP: {} bytes", zipBytes.length);
                    break;
                }

                case "html": {
                    zipBytes = invoicingService.getInvoicesHtml(buyerId, sellerId, fromDate, toDate);
                    var htmls = invoicingService.getPeppolsHTML(buyerId, sellerId, fromDate, toDate);
                    String baseName = NamingUtils.sanitizeFilename(
                            NamingUtils.extractFileNameFromEnvelopes(htmls));
                    fileName = baseName + "-html.zip";
                    logger.info("Returning HTML ZIP: {} bytes", zipBytes.length);
                    break;
                }

                case "pdf": {
                    zipBytes = invoicingService.getInvoicesPdf(buyerId, sellerId, fromDate, toDate);
                    var pdfs = invoicingService.getPeppolsPdf(buyerId, sellerId, fromDate, toDate);
                    String baseName = NamingUtils.sanitizeFilename(
                            NamingUtils.extractFileNameFromEnvelopes(pdfs));
                    fileName = baseName + "-pdf.zip";
                    logger.info("Returning PDF ZIP: {} bytes", zipBytes.length);
                    break;
                }

                case "all": {
                    zipBytes = invoicingService.getInvoicesAll(buyerId, sellerId, fromDate, toDate);
                    var pdfs = invoicingService.getPeppolsPdf(buyerId, sellerId, fromDate, toDate);
                    String baseName = NamingUtils.sanitizeFilename(
                            NamingUtils.extractFileNameFromEnvelopes(pdfs));
                    fileName = baseName + "-all.zip";
                    logger.info("Returning all formats ZIP: {} bytes", zipBytes.length);
                    break;
                }

                default: {
                    String msg = "BAD REQUEST: Unsupported output format: " + fmt;
                    byte[] errorBytes = msg.getBytes(StandardCharsets.UTF_8);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(errorBytes);
                }
            }

            return buildZipResponse(zipBytes, fileName);

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