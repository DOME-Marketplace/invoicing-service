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

    public Collection<String> render(Collection<Invoice> invoices) throws Exception {
        Collection<String> out = new ArrayList<>();
        for (Invoice invoice : invoices) {
            out.add(this.render(invoice));
        }
        return out;
    }

    public  String render(Invoice invoice) {
        PeppolBillingApi<Invoice> api = PeppolBillingApi.create(invoice);
        ValidationResult result = api.validate();

        if (!result.isValid()) {
            StringBuilder sb = new StringBuilder("Validation error:\n");
            result.errors().forEach(e -> sb.append(e).append("\n"));
            result.warns().forEach(w -> sb.append("WARN: ").append(w).append("\n"));

            logger.error("Validation errors for billId: {}", sb);
            throw new PeppolValidationException(sb.toString());
        }

        return api.prettyPrint();
    }


}
