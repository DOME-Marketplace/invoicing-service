package it.eng.dome.invoicing.engine.service;

import it.eng.dome.invoicing.engine.exception.ExternalServiceException;
import it.eng.dome.invoicing.engine.exception.PeppolValidationException;
import it.eng.dome.invoicing.engine.model.InvoiceBom;
import it.eng.dome.invoicing.engine.service.render.BomToPeppol;
import it.eng.dome.invoicing.engine.service.render.Envelope;
import it.eng.dome.invoicing.engine.service.render.Html2Pdf;
import it.eng.dome.invoicing.engine.service.render.Peppol2XML;
import it.eng.dome.invoicing.engine.service.render.PeppolXML2Html;

import it.eng.dome.invoicing.engine.service.utils.ZipUtils;
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

    public Envelope<String> getPeppolXml(String billId) throws ExternalServiceException {
        Invoice inv = getPeppolInvoice(billId);

        Peppol2XML renderer = new Peppol2XML();
        return renderer.render(inv);
    }

    public Collection<Envelope<String>> getPeppolsXml(
            String buyerId,
            String sellerId,
            OffsetDateTime fromDate,
            OffsetDateTime toDate
    ) throws ExternalServiceException {

        Collection<Invoice> invoices = invoices = getPeppolInvoices(buyerId, sellerId, fromDate, toDate);

        Peppol2XML renderer = new Peppol2XML();
        return renderer.render(invoices);
    }

    public Collection<Envelope<String>> getPeppolsHTML(String buyerId,
                                           String sellerId,
                                           OffsetDateTime fromDate,
                                           OffsetDateTime toDate) throws Exception {

        Collection<Invoice> invoices = getPeppolInvoices(buyerId, sellerId, fromDate, toDate);
        Collection<Envelope<String>> xml = new Peppol2XML().render(invoices);
        Collection<Envelope<String>> htmls = new PeppolXML2Html().render(xml);
        return htmls;
    }

    public Collection<Envelope<ByteArrayOutputStream>> getPeppolsPdf(String buyerId,
                                                         String sellerId,
                                                         OffsetDateTime fromDate,
                                                         OffsetDateTime toDate) throws Exception {
        Collection<Envelope<String>> htmls = this.getPeppolsHTML(buyerId, sellerId, fromDate, toDate);
        Collection<Envelope<ByteArrayOutputStream>> pdfs = new Html2Pdf().render(htmls);
        return pdfs;
    }

    // retrieves invoices as XML, creates a ZIP archive, and returns it as an InputStreamResource
    public InputStreamResource getInvoicesXml(String buyerId, String sellerId, OffsetDateTime fromDate, OffsetDateTime toDate)
            throws ExternalServiceException, IOException {
        return createZipResource(getPeppolsXml(buyerId, sellerId, fromDate, toDate));
    }

    public InputStreamResource getInvoicesHtml(String buyerId, String sellerId, OffsetDateTime fromDate, OffsetDateTime toDate)
            throws Exception {
        return createZipResource(getPeppolsHTML(buyerId, sellerId, fromDate, toDate));
    }

    public InputStreamResource getInvoicesPdf(String buyerId, String sellerId, OffsetDateTime fromDate, OffsetDateTime toDate)
            throws Exception {
        return createZipResource(getPeppolsPdf(buyerId, sellerId, fromDate, toDate));
    }

    private <T> InputStreamResource createZipResource(Collection<Envelope<T>> envelopes) throws IOException {
        byte[] zipBytes = ZipUtils.createZip(envelopes);
        return new InputStreamResource(new ByteArrayInputStream(zipBytes));
    }

}
