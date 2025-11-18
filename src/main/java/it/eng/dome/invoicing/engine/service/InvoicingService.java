package it.eng.dome.invoicing.engine.service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import it.eng.dome.invoicing.engine.exception.ExternalServiceException;
import it.eng.dome.invoicing.engine.model.InvoiceBom;
import it.eng.dome.invoicing.engine.service.render.BomToPeppol;
import it.eng.dome.invoicing.engine.service.render.LocalResourceRef;

@Service
public class InvoicingService {

//	private final Logger logger = LoggerFactory.getLogger(InvoicingService.class);

    @Autowired
    BomService bomService;

	public InvoicingService(){}

    public LocalResourceRef getPackagedInvoices(String buyerId, String sellerId, OffsetDateTime fromDate, OffsetDateTime toDate, String format) {
        List<InvoiceBom> boms = bomService.getBomsFor(buyerId, sellerId, fromDate, toDate);
        // TODO: generate the needed local resources, probably by generating first some peppol objects...
        return new LocalResourceRef();
    }

    public Collection<PeppolPlaceholder> getPeppolInvoices(String buyerId, String sellerId, OffsetDateTime fromDate, OffsetDateTime toDate) {
        List<InvoiceBom> boms = bomService.getBomsFor(buyerId, sellerId, fromDate, toDate);
        return new BomToPeppol().render(boms);
    }

    public PeppolPlaceholder getPeppolInvoice(String billId) throws ExternalServiceException {
        InvoiceBom bom = bomService.getBomFor(billId);
        return new BomToPeppol().render(bom);
    }

}
