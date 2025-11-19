package it.eng.dome.invoicing.engine.service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import it.eng.dome.invoicing.engine.exception.PeppolValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import it.eng.dome.invoicing.engine.exception.ExternalServiceException;
import it.eng.dome.invoicing.engine.model.InvoiceBom;
import it.eng.dome.invoicing.engine.service.render.BomToPeppol;
import it.eng.dome.invoicing.engine.service.render.LocalResourceRef;
import peppol.bis.invoice3.api.PeppolBillingApi;
import peppol.bis.invoice3.domain.Invoice;
import peppol.bis.invoice3.validation.ValidationResult;

@Service
public class InvoicingService {

	private final Logger logger = LoggerFactory.getLogger(InvoicingService.class);

    @Autowired
    BomService bomService;

	public InvoicingService(){}

    public LocalResourceRef getPackagedInvoices(String buyerId, String sellerId, OffsetDateTime fromDate, OffsetDateTime toDate, String format) {
        List<InvoiceBom> boms = bomService.getBomsFor(buyerId, sellerId, fromDate, toDate);
        // TODO: generate the needed local resources, probably by generating first some peppol objects...
        return new LocalResourceRef();
    }

    public Collection<Invoice> getPeppolInvoices(String buyerId, String sellerId, OffsetDateTime fromDate, OffsetDateTime toDate) {
        List<InvoiceBom> boms = bomService.getBomsFor(buyerId, sellerId, fromDate, toDate);
        return new BomToPeppol().render(boms);
    }

    public Invoice getPeppolInvoice(String billId) throws ExternalServiceException {
        InvoiceBom bom = bomService.getBomFor(billId);
        return new BomToPeppol().render(bom);
    }

    public String getXmlFromPeppol (String billId) throws ExternalServiceException {
        Invoice peppolInvoice = getPeppolInvoice(billId);

        PeppolBillingApi<Invoice> api = PeppolBillingApi.create(peppolInvoice);
        ValidationResult result = api.validate();

        if (result.isValid()) {
            return api.prettyPrint(); // XML
        }

        // errors: build exception message
        StringBuilder sb = new StringBuilder("Validation error:\n");
        result.errors().forEach(e -> sb.append(e).append("\n"));
        result.warns().forEach(w -> sb.append("WARN: ").append(w).append("\n"));

        logger.error("Peppol validation errors for billId {}: {}", billId, sb.toString());
        throw new PeppolValidationException(sb.toString());
    }

}
