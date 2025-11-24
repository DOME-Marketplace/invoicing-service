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
import it.eng.dome.invoicing.engine.service.utils.ZipUtils;
import peppol.bis.invoice3.domain.Invoice;

@Service
public class InvoicingService {

    @Autowired
    BomService bomService;

    public InvoicingService() {
    }

    private Collection<Envelope<Invoice>> getPeppolInvoices(String buyerId, String sellerId, OffsetDateTime fromDate,
            OffsetDateTime toDate) throws ExternalServiceException {
        List<Envelope<InvoiceBom>> boms = bomService.getBomsFor(buyerId, sellerId, fromDate, toDate);
        return new BomToPeppol().render(boms);
    }

    private Envelope<Invoice> getPeppolInvoice(String billId) throws ExternalServiceException {
        Envelope<InvoiceBom> bom = bomService.getBomFor(billId);
        return new BomToPeppol().render(bom);
    }

    public Envelope<String> getPeppolHTML(String billId) throws Exception {
        Envelope<Invoice> invoice = this.getPeppolInvoice(billId);
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
        Envelope<Invoice> inv = getPeppolInvoice(billId);

        Peppol2XML renderer = new Peppol2XML();
        return renderer.render(inv);
    }

    public Collection<Envelope<String>> getPeppolsXml(
            String buyerId,
            String sellerId,
            OffsetDateTime fromDate,
            OffsetDateTime toDate) throws ExternalServiceException {

        Collection<Envelope<Invoice>> invoices = getPeppolInvoices(buyerId, sellerId, fromDate, toDate);

        Peppol2XML renderer = new Peppol2XML();
        return renderer.render(invoices);
    }

    public Collection<Envelope<String>> getPeppolsHTML(String buyerId,
            String sellerId,
            OffsetDateTime fromDate,
            OffsetDateTime toDate) throws Exception {

        Collection<Envelope<Invoice>> invoices = getPeppolInvoices(buyerId, sellerId, fromDate, toDate);
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

    public InputStreamResource getInvoicesXml(String buyerId, String sellerId, OffsetDateTime fromDate,
            OffsetDateTime toDate)
            throws ExternalServiceException, IOException {
        return createZipResource(getPeppolsXml(buyerId, sellerId, fromDate, toDate));
    }

    public InputStreamResource getInvoicesHtml(String buyerId, String sellerId, OffsetDateTime fromDate,
            OffsetDateTime toDate)
            throws Exception {
        return createZipResource(getPeppolsHTML(buyerId, sellerId, fromDate, toDate));
    }

    public InputStreamResource getInvoicesPdf(String buyerId, String sellerId, OffsetDateTime fromDate,
            OffsetDateTime toDate)
            throws Exception {
        return createZipResource(getPeppolsPdf(buyerId, sellerId, fromDate, toDate));
    }

    private <T> InputStreamResource createZipResource(Collection<Envelope<T>> envelopes) throws IOException {
        byte[] zipBytes = ZipUtils.createZip(envelopes);
        return new InputStreamResource(new ByteArrayInputStream(zipBytes));
    }

    public InputStreamResource getInvoiceAllFormats(String billId) throws Exception {
        Envelope<String> xml = getPeppolXml(billId);
        Envelope<String> html = getPeppolHTML(billId);
        Envelope<ByteArrayOutputStream> pdf = getPeppolPdf(billId);
        // Generic Envelope collection
        Collection<Envelope<?>> all = List.of(xml, html, pdf);

        byte[] zipBytes = ZipUtils.createZip(all);
        return new InputStreamResource(new ByteArrayInputStream(zipBytes));
    }

    public InputStreamResource getInvoicesAll(
            String buyerId,
            String sellerId,
            OffsetDateTime fromDate,
            OffsetDateTime toDate) throws Exception {

        Collection<Envelope<String>> xmls = getPeppolsXml(buyerId, sellerId, fromDate, toDate);
        Collection<Envelope<String>> htmls = getPeppolsHTML(buyerId, sellerId, fromDate, toDate);
        Collection<Envelope<ByteArrayOutputStream>> pdfs = getPeppolsPdf(buyerId, sellerId, fromDate, toDate);

        byte[] zipBytes = ZipUtils.zipPerInvoice(xmls, htmls, pdfs);

        return new InputStreamResource(new ByteArrayInputStream(zipBytes));
    }

}
