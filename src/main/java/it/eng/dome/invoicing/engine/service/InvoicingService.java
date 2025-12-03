package it.eng.dome.invoicing.engine.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import it.eng.dome.invoicing.engine.exception.ExternalServiceException;
import it.eng.dome.invoicing.engine.model.InvoiceBom;
import it.eng.dome.invoicing.engine.service.render.BomToPeppol;
import it.eng.dome.invoicing.engine.service.render.Envelope;
import it.eng.dome.invoicing.engine.service.render.Html2Pdf;
import it.eng.dome.invoicing.engine.service.render.Peppol2XML;
import it.eng.dome.invoicing.engine.service.render.PeppolXML2Html;
import it.eng.dome.invoicing.engine.service.utils.NamingUtils;
import it.eng.dome.invoicing.engine.service.utils.ZipUtils;
import peppol.bis.invoice3.domain.Invoice;

/**
 * Service for handling invoices and converting them to PEPPOL formats.
 * Provides methods to retrieve single invoices, collections of invoices,
 * or ZIP archives containing XML, HTML, and PDF formats.
 */
@Service
public class InvoicingService {

    @Autowired
    BomService bomService;

    public InvoicingService() {
    }

    /**
     * Retrieves all invoices for a given buyer and seller id within a date range.
     *
     * @param buyerId  Buyer identifier
     * @param sellerId Seller identifier
     * @param fromDate Start date
     * @param toDate   End date
     * @return Collection of Invoice envelopes
     * @throws ExternalServiceException if fetching data fails
     */
    private Collection<Envelope<Invoice>> getPeppolInvoices(String buyerId, String sellerId, OffsetDateTime fromDate,
            OffsetDateTime toDate) throws ExternalServiceException {
        List<Envelope<InvoiceBom>> boms = bomService.getBomsFor(buyerId, sellerId, fromDate, toDate);
        return new BomToPeppol().render(boms);
    }

    /**
     * Retrieves a single Peppol Invoice by Customer Bill ID.
     *
     * @param billId Customer Bill identifier
     * @return Envelope containing the invoice
     * @throws ExternalServiceException if fetching data fails
     */
    private Envelope<Invoice> getPeppolInvoice(String billId) throws ExternalServiceException {
        Envelope<InvoiceBom> bom = bomService.getBomFor(billId);
        return new BomToPeppol().render(bom);
    }

    /**
     * Retrieves a single HTML Invoice by Customer Bill ID.
     *
     * @param billId Customer Bill identifier
     * @return Envelope containing HTML representation
     * @throws Exception if rendering fails
     */
    public Envelope<String> getPeppolHTML(String billId) throws Exception {
        Envelope<Invoice> invoice = this.getPeppolInvoice(billId);
        Envelope<String> xml = new Peppol2XML().render(invoice);
        Envelope<String> html = new PeppolXML2Html().render(xml);
        return html;
    }

    /**
     * Retrieves a single PDF Invoice by Customer Bill ID.
     *
     * @param billId Customer Bill identifier
     * @return Envelope containing PDF representation
     * @throws Exception if rendering fails
     */
    public Envelope<ByteArrayOutputStream> getPeppolPdf(String billId) throws Exception {
        Envelope<String> html = this.getPeppolHTML(billId);
        Envelope<ByteArrayOutputStream> pdf = new Html2Pdf().render(html);
        return pdf;
    }

    /**
     * Returns the PEPPOL XML representation of a single invoice.
     *
     * @param billId the CustomerBill identifier
     * @return Envelope containing the invoice XML
     * @throws ExternalServiceException if the invoice is not found or an external error occurs
     */
    public Envelope<String> getPeppolXml(String billId) throws ExternalServiceException {
        Envelope<Invoice> inv = getPeppolInvoice(billId);

        Peppol2XML renderer = new Peppol2XML();
        return renderer.render(inv);
    }

    /**
     * Retrieves all invoices as XML between a given buyer and seller ID within a date range.
     *
     * @param buyerId  Buyer identifier
     * @param sellerId Seller identifier
     * @param fromDate Start date
     * @param toDate   End date
     * @return Collection of XML envelopes
     * @throws ExternalServiceException if fetching data fails
     */
    public Collection<Envelope<String>> getPeppolsXml(
            String buyerId,
            String sellerId,
            OffsetDateTime fromDate,
            OffsetDateTime toDate) throws ExternalServiceException {

        Collection<Envelope<Invoice>> invoices = getPeppolInvoices(buyerId, sellerId, fromDate, toDate);

        Peppol2XML renderer = new Peppol2XML();
        return renderer.render(invoices);
    }

    /**
     * Retrieves all invoices as HTML between a given buyer and seller ID within a date range.
     *
     * @param buyerId  Buyer identifier
     * @param sellerId Seller identifier
     * @param fromDate Start date
     * @param toDate   End date
     * @return Collection of HTML envelopes
     * @throws ExternalServiceException if fetching data fails
     */
    public Collection<Envelope<String>> getPeppolsHTML(String buyerId,
            String sellerId,
            OffsetDateTime fromDate,
            OffsetDateTime toDate) throws Exception {

        Collection<Envelope<Invoice>> invoices = getPeppolInvoices(buyerId, sellerId, fromDate, toDate);
        Collection<Envelope<String>> xml = new Peppol2XML().render(invoices);
        Collection<Envelope<String>> htmls = new PeppolXML2Html().render(xml);
        return htmls;
    }

    /**
     * Retrieves all invoices as PDF between a given buyer and seller ID within a date range.
     *
     * @param buyerId  Buyer identifier
     * @param sellerId Seller identifier
     * @param fromDate Start date
     * @param toDate   End date
     * @return Collection of PDF envelopes
     * @throws ExternalServiceException if fetching data fails
     */
    public Collection<Envelope<ByteArrayOutputStream>> getPeppolsPdf(String buyerId,
            String sellerId,
            OffsetDateTime fromDate,
            OffsetDateTime toDate) throws Exception {
        Collection<Envelope<String>> htmls = this.getPeppolsHTML(buyerId, sellerId, fromDate, toDate);
        Collection<Envelope<ByteArrayOutputStream>> pdfs = new Html2Pdf().render(htmls);
        return pdfs;
    }

    /**
     * Returns a ZIP of invoices in XML format between seller and buyer id within a date range.
     *
     * @param buyerId  Buyer identifier
     * @param sellerId Seller identifier
     * @param fromDate Start date
     * @param toDate   End date
     * @return InputStreamResource containing the ZIP
     * @throws ExternalServiceException if fetching fails
     * @throws IOException              if ZIP creation fails
     */
    public InputStreamResource getInvoicesXml(String buyerId, String sellerId, OffsetDateTime fromDate,
            OffsetDateTime toDate)
            throws ExternalServiceException, IOException {
        return createZipResource(getPeppolsXml(buyerId, sellerId, fromDate, toDate));
    }

    /**
     * Returns a ZIP of invoices in HTML format between seller and buyer id within a date range.
     *
     * @param buyerId  Buyer identifier
     * @param sellerId Seller identifier
     * @param fromDate Start date
     * @param toDate   End date
     * @return InputStreamResource containing the ZIP
     * @throws ExternalServiceException if fetching fails
     * @throws IOException              if ZIP creation fails
     */
    public InputStreamResource getInvoicesHtml(String buyerId, String sellerId, OffsetDateTime fromDate,
            OffsetDateTime toDate)
            throws Exception {
        return createZipResource(getPeppolsHTML(buyerId, sellerId, fromDate, toDate));
    }

    /**
     * Returns a ZIP of invoices in PDF format between seller and buyer id within a date range.
     *
     * @param buyerId  Buyer identifier
     * @param sellerId Seller identifier
     * @param fromDate Start date
     * @param toDate   End date
     * @return InputStreamResource containing the ZIP
     * @throws ExternalServiceException if fetching fails
     * @throws IOException              if ZIP creation fails
     */
    public InputStreamResource getInvoicesPdf(String buyerId, String sellerId, OffsetDateTime fromDate,
            OffsetDateTime toDate)
            throws Exception {
        return createZipResource(getPeppolsPdf(buyerId, sellerId, fromDate, toDate));
    }

    /**
     * Creates a ZIP resource from a collection of envelopes.
     *
     * @param envelopes Collection of envelopes to zip
     * @param <T>       Type of envelope content
     * @return InputStreamResource containing the ZIP
     * @throws IOException if ZIP creation fails
     */
    private <T> InputStreamResource createZipResource(Collection<Envelope<T>> envelopes) throws IOException {
        byte[] zipBytes = ZipUtils.createZip(envelopes);
        return new InputStreamResource(new ByteArrayInputStream(zipBytes), NamingUtils.sanitizeFilename(NamingUtils.extractFileNameFromEnvelopes(envelopes)));
    }

    /**
     * Returns a ZIP containing a single invoice in all formats (XML, HTML, PDF).
     *
     * @param billId Customer Bill identifier
     * @return InputStreamResource containing the ZIP
     * @throws Exception if rendering fails
     */
    public InputStreamResource getInvoiceAllFormats(String billId) throws Exception {
        Envelope<String> xml = getPeppolXml(billId);
        Envelope<String> html = getPeppolHTML(billId);
        Envelope<ByteArrayOutputStream> pdf = getPeppolPdf(billId);
        // Generic Envelope collection
        Collection<Envelope<?>> all = List.of(xml, html, pdf);

        byte[] zipBytes = ZipUtils.createZip(all);
        return new InputStreamResource(new ByteArrayInputStream(zipBytes), xml.getName());
    }

    /**
     * Returns a ZIP containing all invoices for a buyer and seller, with each invoice in XML, HTML, PDF.
     *
     * @param buyerId  Buyer identifier
     * @param sellerId Seller identifier
     * @param fromDate Start date
     * @param toDate   End date
     * @return InputStreamResource containing the ZIP
     * @throws Exception if rendering fails
     */
    public InputStreamResource getInvoicesAll(
            String buyerId,
            String sellerId,
            OffsetDateTime fromDate,
            OffsetDateTime toDate) throws Exception {

        Collection<Envelope<String>> xmls = getPeppolsXml(buyerId, sellerId, fromDate, toDate);
        Collection<Envelope<String>> htmls = getPeppolsHTML(buyerId, sellerId, fromDate, toDate);
        Collection<Envelope<ByteArrayOutputStream>> pdfs = getPeppolsPdf(buyerId, sellerId, fromDate, toDate);

        byte[] zipBytes = ZipUtils.zipPerInvoice(xmls, htmls, pdfs);
        return new InputStreamResource(new ByteArrayInputStream(zipBytes), NamingUtils.sanitizeFilename(NamingUtils.extractFileNameFromEnvelopes(pdfs)));
    }
}
