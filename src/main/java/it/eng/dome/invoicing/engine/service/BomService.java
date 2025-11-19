package it.eng.dome.invoicing.engine.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import it.eng.dome.brokerage.api.APIPartyApis;
import it.eng.dome.brokerage.api.AppliedCustomerBillRateApis;
import it.eng.dome.brokerage.api.CustomerBillApis;
import it.eng.dome.brokerage.api.ProductCatalogManagementApis;
import it.eng.dome.brokerage.api.ProductInventoryApis;
import it.eng.dome.invoicing.engine.exception.ExternalServiceException;
import it.eng.dome.invoicing.engine.model.InvoiceBom;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOffering;
import it.eng.dome.tmforum.tmf632.v4.model.Organization;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.ApiException;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBill;
import it.eng.dome.tmforum.tmf678.v4.model.RelatedParty;
import peppol.bis.invoice3.domain.Invoice;

@Service
public class BomService {

//	private final Logger logger = LoggerFactory.getLogger(BomService.class);

	private final APIPartyApis partyAPI;
	private final ProductInventoryApis productInventoryAPI;
    private final CustomerBillApis customerBillAPI;
    private final AppliedCustomerBillRateApis appliedCustomerBillingRateAPI;
    private final ProductCatalogManagementApis productCatalogManagementAPI;

	public BomService(APIPartyApis partyAPI, ProductInventoryApis productInventoryAPI, CustomerBillApis customerBillAPI, AppliedCustomerBillRateApis appliedCustomerBillingRateAPI, ProductCatalogManagementApis productCatalogManagementAPI) {		
        this.partyAPI = partyAPI;
        this.productInventoryAPI = productInventoryAPI;
        this.customerBillAPI = customerBillAPI;
        this.appliedCustomerBillingRateAPI = appliedCustomerBillingRateAPI;
        this.productCatalogManagementAPI = productCatalogManagementAPI;
	}

    public List<InvoiceBom> getBomsFor(String buyerId, String sellerId, OffsetDateTime fromDate, OffsetDateTime toDate) {
        List<InvoiceBom> out = new ArrayList<>();
        // TODO: implement me
        out.add(new InvoiceBom(null));
        out.add(new InvoiceBom(null));
        out.add(new InvoiceBom(null));
        return out;
    }

    public InvoiceBom getBomFor(String customerBillId) throws ExternalServiceException {

        // retrieve the customer bill...
        InvoiceBom bom = null;
        try {
            CustomerBill cb;
            cb = this.customerBillAPI.getCustomerBill(customerBillId, null);
            // ...and create the bom
            bom = new InvoiceBom(cb);
        } catch (ApiException e) {
            throw new ExternalServiceException(e.getMessage(), e);
        }

        // FIXME: commented out, as tmf returns internal server error as of 17 nov 2025
        // add included acbrs (if any)
        try {
            List<AppliedCustomerBillingRate> acbrs;
            Map<String, String> filter = Map.of("bill.id", bom.getCustomerBill().getId());
            acbrs = this.appliedCustomerBillingRateAPI.listAppliedCustomerBillingRates(null, 0, 5, filter);
            for(AppliedCustomerBillingRate acbr: acbrs) {
                bom.add(acbr);
            }
        } catch (ApiException e) {
            throw new ExternalServiceException(e.getMessage(), e);
        }

        try {
            // now add products (where referenced inside acbrs)
            for(AppliedCustomerBillingRate acbr: bom.getAppliedCustomerBillingRates()) {
                if(acbr.getProduct()!=null && acbr.getProduct().getId()!=null)
                        bom.add(this.productInventoryAPI.getProduct(acbr.getProduct().getId(), null));
            }
        } catch (it.eng.dome.tmforum.tmf637.v4.ApiException e) {
            throw new ExternalServiceException(e.getMessage(), e);
        }

        try {
            // add product offerings (where referenced inside acbrs)
            for(Product product: bom.getProducts()) {
                if(product.getProductOffering()!=null && product.getProductOffering().getId()!=null) {
                    ProductOffering offering;
                        offering = this.productCatalogManagementAPI.getProductOffering(product.getProductOffering().getId(), null);
                    bom.add(offering);
                }
            }
        } catch (it.eng.dome.tmforum.tmf620.v4.ApiException e) {
            throw new ExternalServiceException(e.getMessage(), e);
        }

        try {
            // add organizations (as referenced within the CB)
            for(RelatedParty party: bom.getCustomerBill().getRelatedParty()) {
                if(party.getRole()!=null && party.getId()!=null) {
                    Organization organization;
                        organization = this.partyAPI.getOrganization(party.getId(), null);
                    bom.add(organization, party.getRole());
                }
            }
        } catch (it.eng.dome.tmforum.tmf632.v4.ApiException e) {
            throw new ExternalServiceException(e.getMessage(), e);
        }

        // TODO: add billing accounts
        // Q: which billing accounts to retrieve for those organisations?


        // TODO: add POPs
        // Q: are they needed?

        return bom;
    }
}
