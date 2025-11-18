package it.eng.dome.invoicing.engine.service.render;

import java.util.ArrayList;
import java.util.Collection;

import it.eng.dome.invoicing.engine.model.InvoiceBom;
import it.eng.dome.invoicing.engine.service.PeppolPlaceholder;

public class BomToPeppol {

    // replace Object with the proper PEPPOL type
    public PeppolPlaceholder render(InvoiceBom bom) {        
        // TODO: translate bom into a peppol invoice


        
        return new PeppolPlaceholder();
    }

    // replace Object with the proper PEPPOL type
    public Collection<PeppolPlaceholder> render(Collection<InvoiceBom> boms) {
        Collection<PeppolPlaceholder> out = new ArrayList<>();
        for(InvoiceBom bom: boms) {
            out.add(this.render(bom));
        }
        return out;
    }

}
