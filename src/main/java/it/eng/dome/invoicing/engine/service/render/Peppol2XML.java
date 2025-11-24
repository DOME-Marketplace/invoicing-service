package it.eng.dome.invoicing.engine.service.render;

import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.eng.dome.invoicing.engine.exception.PeppolValidationException;
import peppol.bis.invoice3.api.PeppolBillingApi;
import peppol.bis.invoice3.domain.Invoice;
import peppol.bis.invoice3.validation.ValidationResult;

public class Peppol2XML {

    private static final Logger logger = LoggerFactory.getLogger(Peppol2XML.class);

    public Collection<Envelope<String>> render(Collection<Envelope<Invoice>> invoices) {
		Collection<Envelope<String>> out = new ArrayList<>();
		for (Envelope<Invoice> envInvoice : invoices) {
			out.add(this.render(envInvoice));
		}
		return out;
	}

    public Envelope<String> render(Envelope<Invoice> envInvoice) {
		Invoice invoice = envInvoice.getContent();
        PeppolBillingApi<Invoice> api = PeppolBillingApi.create(invoice);
        ValidationResult result = api.validate();

        if (!result.isValid()) {
            StringBuilder sb = new StringBuilder("Validation error:\n");
            result.errors().forEach(e -> sb.append(e).append("\n"));
            result.warns().forEach(w -> sb.append("WARN: ").append(w).append("\n"));

            logger.error("Validation errors for billId: {}", sb);
            throw new PeppolValidationException(sb.toString());
        }

        return new Envelope<String>(api.prettyPrint(), envInvoice.getName(), "xml");
    }
}
