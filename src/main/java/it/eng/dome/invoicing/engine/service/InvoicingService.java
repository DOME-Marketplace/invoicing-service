package it.eng.dome.invoicing.engine.service;

import it.eng.dome.invoicing.engine.exception.ExternalServiceException;
import it.eng.dome.invoicing.engine.exception.PeppolValidationException;
import it.eng.dome.invoicing.engine.model.InvoiceBom;
import it.eng.dome.invoicing.engine.service.render.BomToPeppol;
import it.eng.dome.invoicing.engine.service.render.Envelope;
import it.eng.dome.invoicing.engine.service.render.Html2Pdf;
import it.eng.dome.invoicing.engine.service.render.Peppol2XML;
import it.eng.dome.invoicing.engine.service.render.PeppolXML2Html;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import peppol.bis.invoice3.api.PeppolBillingApi;
import peppol.bis.invoice3.domain.Invoice;
import peppol.bis.invoice3.validation.ValidationResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class InvoicingService {

	private final Logger logger = LoggerFactory.getLogger(InvoicingService.class);

    @Autowired
    BomService bomService;

	public InvoicingService(){}

    /*
    public LocalResourceRef getPackagedInvoices(String buyerId, String sellerId, OffsetDateTime fromDate, OffsetDateTime toDate, String format) throws ExternalServiceException {
        List<InvoiceBom> boms = bomService.getBomsFor(buyerId, sellerId, fromDate, toDate);
        // TODO: generate the needed local resources, probably by generating first some peppol objects...
        return new LocalResourceRef();
    }
    */

    private Collection<Invoice> getPeppolInvoices(String buyerId, String sellerId, OffsetDateTime fromDate, OffsetDateTime toDate) throws ExternalServiceException {
        List<InvoiceBom> boms = bomService.getBomsFor(buyerId, sellerId, fromDate, toDate);
        return new BomToPeppol().render(boms);
    }

    private Invoice getPeppolInvoice(String billId) throws ExternalServiceException {
        InvoiceBom bom = bomService.getBomFor(billId);
        return new BomToPeppol().render(bom);
    }

    public Envelope<String> getPeppolHTML(String billId) throws Exception {
        Invoice invoice = this.getPeppolInvoice(billId);
        Envelope<String> xml = new Peppol2XML().render(invoice);
        Envelope<String> html = new PeppolXML2Html().render(xml);
        return html;
    }

    public Envelope<ByteArrayOutputStream> getPeppolPdf(String billId) throws Exception {
        Envelope<String> html = this.getPeppolHTML(billId);
        Envelope<ByteArrayOutputStream> pdf = new Html2Pdf().render(html);
        return pdf;
    }

    public String getPeppolXml(String billId) throws ExternalServiceException {
        Invoice peppolInvoice = getPeppolInvoice(billId);

        PeppolBillingApi<Invoice> api = PeppolBillingApi.create(peppolInvoice);
        ValidationResult result = api.validate();

        if (!result.isValid()) {
            StringBuilder sb = new StringBuilder("Validation error:\n");
            result.errors().forEach(e -> sb.append(e).append("\n"));
            result.warns().forEach(w -> sb.append("WARN: ").append(w).append("\n"));

            logger.error("Validation errors for billId {}: {}", billId, sb);
            throw new PeppolValidationException(sb.toString());
        }

        return api.prettyPrint();
    }

    public Collection<String> getPeppolsXml (String buyerId, String sellerId, OffsetDateTime fromDate, OffsetDateTime toDate) throws ExternalServiceException {
        Collection<Invoice> peppolInvoices = getPeppolInvoices(buyerId, sellerId, fromDate, toDate);
        PeppolBillingApi<Invoice> api;
        ValidationResult result;
        Collection<String> out = new java.util.ArrayList<>();
        for(Invoice inv : peppolInvoices) {
            api = PeppolBillingApi.create(inv);
            result = api.validate();
            if (result.isValid()) {
                out.add(api.prettyPrint());
            } else {
                // errors
                StringBuilder sb = new StringBuilder("Validation error:\n");
                result.errors().forEach(e -> sb.append(e).append("\n"));
                result.warns().forEach(w -> sb.append("WARN: ").append(w).append("\n"));
                logger.error("Peppol validation errors for invoice {}: {}", inv.getProfileID(), sb.toString());
            }
        }

        return out;
    }

    // retrieves invoices as XML, creates a ZIP archive, and returns it as an InputStreamResource
    public InputStreamResource getInvoicesZip(String buyerId, String sellerId, OffsetDateTime fromDate, OffsetDateTime toDate) throws ExternalServiceException, IOException {
        Collection<String> peppolsXml = getPeppolsXml(buyerId, sellerId, fromDate, toDate);
        byte[] zipBytes = createInvoicesZip(peppolsXml);
        return new InputStreamResource(new ByteArrayInputStream(zipBytes));
    }

    // to create a zip file containing multiple invoices in XML format
    public byte[] createInvoicesZip(Collection<String> invoicesXml) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        int index = 1;
        for (String xml : invoicesXml) {
            ZipEntry entry = new ZipEntry("invoice_" + index + ".xml");
            zos.putNextEntry(entry);
            zos.write(xml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            index++;
        }

        zos.close();
        return baos.toByteArray();
    }


}
