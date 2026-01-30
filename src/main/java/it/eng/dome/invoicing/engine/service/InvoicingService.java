package it.eng.dome.invoicing.engine.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * Service for handling invoices and converting them to PEPPOL formats.
 * Provides methods to retrieve single invoices, collections of invoices,
 * or ZIP archives containing XML, HTML, and PDF formats.
 */
@Service
public class InvoicingService {

    private static final Logger logger = LoggerFactory.getLogger(InvoicingService.class);
    
    @Autowired
    BomService bomService;
    
    public InvoicingService() {
    }

    private Collection<Envelope<Invoice>> getPeppolInvoices(String buyerId, String sellerId, OffsetDateTime fromDate,
            OffsetDateTime toDate) throws ExternalServiceException {
        logger.debug("Fetching PEPPOL invoices for buyer: {}, seller: {}, from: {}, to: {}", 
                     buyerId, sellerId, fromDate, toDate);
        List<Envelope<InvoiceBom>> boms = bomService.getBomsFor(buyerId, sellerId, fromDate, toDate);
        Collection<Envelope<Invoice>> invoices = new BomToPeppol().render(boms);
        logger.info("Retrieved {} PEPPOL invoices for buyer: {}, seller: {}", 
                    invoices.size(), buyerId, sellerId);
        return invoices;
    }

    private Envelope<Invoice> getPeppolInvoice(String billId) throws ExternalServiceException {
        logger.debug("Fetching PEPPOL invoice for billId: {}", billId);
        Envelope<InvoiceBom> bom = bomService.getBomFor(billId);
        Envelope<Invoice> invoice = new BomToPeppol().render(bom);
        logger.info("Retrieved PEPPOL invoice for billId: {}", billId);
        return invoice;
    }

    public Envelope<String> getPeppolHTML(String billId) throws Exception {
        logger.debug("Rendering HTML invoice for billId: {}", billId);
        Envelope<Invoice> invoice = this.getPeppolInvoice(billId);
        Envelope<String> xml = new Peppol2XML().render(invoice);
        Envelope<String> html = new PeppolXML2Html().render(xml);
        logger.info("Rendered HTML invoice for billId: {}", billId);
        return html;
    }

    public Envelope<ByteArrayOutputStream> getPeppolPdf(String billId) throws Exception {
        logger.debug("Rendering PDF invoice for billId: {}", billId);
        Envelope<String> html = this.getPeppolHTML(billId);
        Envelope<ByteArrayOutputStream> pdf = new Html2Pdf().render(html);
        logger.info("Rendered PDF invoice for billId: {}, size: {} bytes", 
                    billId, pdf.getContent().size());
        return pdf;
    }

    public Envelope<String> getPeppolXml(String billId) throws ExternalServiceException {
        logger.debug("Rendering XML invoice for billId: {}", billId);
        Envelope<Invoice> inv = getPeppolInvoice(billId);
        Peppol2XML renderer = new Peppol2XML();
        Envelope<String> xml = renderer.render(inv);
        logger.info("Rendered XML invoice for billId: {}, size: {} bytes", 
                    billId, xml.getContent().getBytes().length);
        return xml;
    }

    public Collection<Envelope<String>> getPeppolsXml(
            String buyerId,
            String sellerId,
            OffsetDateTime fromDate,
            OffsetDateTime toDate) throws ExternalServiceException {

        logger.debug("Rendering XML invoices for buyer: {}, seller: {}", buyerId, sellerId);
        Collection<Envelope<Invoice>> invoices = getPeppolInvoices(buyerId, sellerId, fromDate, toDate);
        Peppol2XML renderer = new Peppol2XML();
        Collection<Envelope<String>> xmls = renderer.render(invoices);
        logger.info("Rendered {} XML invoices for buyer: {}, seller: {}", 
                    xmls.size(), buyerId, sellerId);
        return xmls;
    }

    public Collection<Envelope<String>> getPeppolsHTML(String buyerId,
            String sellerId,
            OffsetDateTime fromDate,
            OffsetDateTime toDate) throws Exception {

        logger.debug("Rendering HTML invoices for buyer: {}, seller: {}", buyerId, sellerId);
        Collection<Envelope<Invoice>> invoices = getPeppolInvoices(buyerId, sellerId, fromDate, toDate);
        Collection<Envelope<String>> xml = new Peppol2XML().render(invoices);
        Collection<Envelope<String>> htmls = new PeppolXML2Html().render(xml);
        logger.info("Rendered {} HTML invoices for buyer: {}, seller: {}", 
                    htmls.size(), buyerId, sellerId);
        return htmls;
    }

    public Collection<Envelope<ByteArrayOutputStream>> getPeppolsPdf(String buyerId,
            String sellerId,
            OffsetDateTime fromDate,
            OffsetDateTime toDate) throws Exception {
        logger.debug("Rendering PDF invoices for buyer: {}, seller: {}", buyerId, sellerId);
        Collection<Envelope<String>> htmls = this.getPeppolsHTML(buyerId, sellerId, fromDate, toDate);
        Collection<Envelope<ByteArrayOutputStream>> pdfs = new Html2Pdf().render(htmls);
        logger.info("Rendered {} PDF invoices for buyer: {}, seller: {}", 
                    pdfs.size(), buyerId, sellerId);
        return pdfs;
    }

    public byte[] getInvoicesXml(String buyerId, String sellerId, OffsetDateTime fromDate,
            OffsetDateTime toDate)
            throws ExternalServiceException, IOException {
        logger.debug("Creating XML ZIP for buyer: {}, seller: {}", buyerId, sellerId);
        byte[] zipBytes = ZipUtils.createZip(getPeppolsXml(buyerId, sellerId, fromDate, toDate));
        logger.info("Created XML ZIP for buyer: {}, seller: {}, size: {} bytes", 
                    buyerId, sellerId, zipBytes.length);
        return zipBytes;
    }

    public byte[] getInvoicesHtml(String buyerId, String sellerId, OffsetDateTime fromDate,
            OffsetDateTime toDate)
            throws Exception {
        logger.debug("Creating HTML ZIP for buyer: {}, seller: {}", buyerId, sellerId);
        byte[] zipBytes = ZipUtils.createZip(getPeppolsHTML(buyerId, sellerId, fromDate, toDate));
        logger.info("Created HTML ZIP for buyer: {}, seller: {}, size: {} bytes", 
                    buyerId, sellerId, zipBytes.length);
        return zipBytes;
    }

    public byte[] getInvoicesPdf(String buyerId, String sellerId, OffsetDateTime fromDate,
            OffsetDateTime toDate)
            throws Exception {
        logger.debug("Creating PDF ZIP for buyer: {}, seller: {}", buyerId, sellerId);
        byte[] zipBytes = ZipUtils.createZip(getPeppolsPdf(buyerId, sellerId, fromDate, toDate));
        logger.info("Created PDF ZIP for buyer: {}, seller: {}, size: {} bytes", 
                    buyerId, sellerId, zipBytes.length);
        return zipBytes;
    }

    public byte[] getInvoiceAllFormats(String billId) throws Exception {
        logger.debug("Creating all formats ZIP for billId: {}", billId);
        Envelope<String> xml = getPeppolXml(billId);
        Envelope<String> html = getPeppolHTML(billId);
        Envelope<ByteArrayOutputStream> pdf = getPeppolPdf(billId);
        Collection<Envelope<?>> all = List.of(xml, html, pdf);

        byte[] zipBytes = ZipUtils.createZip(all);
        logger.info("Created all formats ZIP for billId: {}, size: {} bytes", billId, zipBytes.length);
        return zipBytes;
    }

    public byte[] getInvoiceXmlAndHtmlFormats(String billId) {
        try {
            logger.debug("Creating XML and HTML ZIP for billId: {}", billId);
            Envelope<String> xml = getPeppolXml(billId);
            Envelope<String> html = getPeppolHTML(billId);
            Collection<Envelope<?>> all = List.of(xml, html);

            byte[] zipBytes = ZipUtils.createZip(all);
            logger.info("Created XML and HTML ZIP for billId: {}, size: {} bytes", billId, zipBytes.length);
            
            return zipBytes;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get invoice in XML and HTML formats for billId: " + billId, e);
        }
    }

    public byte[] getInvoicesAll(
            String buyerId,
            String sellerId,
            OffsetDateTime fromDate,
            OffsetDateTime toDate) throws Exception {

        logger.debug("Creating all formats ZIP for buyer: {}, seller: {}", buyerId, sellerId);
        Collection<Envelope<String>> xmls = getPeppolsXml(buyerId, sellerId, fromDate, toDate);
        Collection<Envelope<String>> htmls = getPeppolsHTML(buyerId, sellerId, fromDate, toDate);
        Collection<Envelope<ByteArrayOutputStream>> pdfs = getPeppolsPdf(buyerId, sellerId, fromDate, toDate);

        byte[] zipBytes = ZipUtils.zipPerInvoice(xmls, htmls, pdfs);
        logger.info("Created all formats ZIP for buyer: {}, seller: {}, size: {} bytes", 
                    buyerId, sellerId, zipBytes.length);
        return zipBytes;
    }

}