package it.eng.dome.invoicing.engine.service;

import it.eng.dome.brokerage.api.*;
import it.eng.dome.invoicing.engine.exception.ExternalServiceException;
import it.eng.dome.invoicing.engine.model.InvoiceBom;
import it.eng.dome.invoicing.engine.service.render.Envelope;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOffering;
import it.eng.dome.tmforum.tmf632.v4.model.Organization;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf666.v4.model.BillingAccount;
import it.eng.dome.tmforum.tmf678.v4.ApiException;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBill;
import it.eng.dome.tmforum.tmf678.v4.model.RelatedParty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BomService {

	private final Logger logger = LoggerFactory.getLogger(BomService.class);

	private final APIPartyApis partyAPI;
	private final ProductInventoryApis productInventoryAPI;
    private final CustomerBillApis customerBillAPI;
    private final AppliedCustomerBillRateApis appliedCustomerBillingRateAPI;
    private final ProductCatalogManagementApis productCatalogManagementAPI;
    private final AccountManagementApis accountManagementAPI;

	public BomService(APIPartyApis partyAPI, ProductInventoryApis productInventoryAPI, CustomerBillApis customerBillAPI, AppliedCustomerBillRateApis appliedCustomerBillingRateAPI, ProductCatalogManagementApis productCatalogManagementAPI, AccountManagementApis accountManagementAPI) {
        this.partyAPI = partyAPI;
        this.productInventoryAPI = productInventoryAPI;
        this.customerBillAPI = customerBillAPI;
        this.appliedCustomerBillingRateAPI = appliedCustomerBillingRateAPI;
        this.productCatalogManagementAPI = productCatalogManagementAPI;
        this.accountManagementAPI = accountManagementAPI;
	}

    public List<Envelope<InvoiceBom>> getBomsFor(String buyerId, String sellerId, OffsetDateTime fromDate, OffsetDateTime toDate) throws ExternalServiceException {
    	List<Envelope<InvoiceBom>> out = new ArrayList<>();

        Map<String, String> filter = new HashMap<>();
        if (buyerId != null) filter.put("relatedParty.id", buyerId); // only one filter per relatedParty.id is allowed
        if (sellerId != null) filter.put("relatedParty.id", sellerId); // so we need to filter manually later
        if (fromDate != null) filter.put("billDate>= ", fromDate.truncatedTo(ChronoUnit.SECONDS).toString());
        if (toDate != null) filter.put("billDate<= ", toDate.truncatedTo(ChronoUnit.SECONDS).toString());

        try {
            List<CustomerBill> bills = this.customerBillAPI.listCustomerBills(null, 0, 100, filter);
            logger.debug("Found {} Customer Bills between {} and {}", bills.size(), fromDate, toDate);

            for (CustomerBill cb : bills) {
                boolean include = true; // default: include the bill unless it fails a filter

                // filter by buyerId if provided
                if (buyerId != null) {
                    include = false; // reset inclusion, must match buyer
                    if (cb.getRelatedParty() != null) {
                        for (RelatedParty rp : cb.getRelatedParty()) {
                            if (buyerId.equals(rp.getId()) && "Buyer".equalsIgnoreCase(rp.getRole())) {
                                include = true; // matched buyer
                                break; // no need to continue checking other related parties
                            }
                        }
                    }
                }

                // filter by sellerId if provided
                if (include && sellerId != null) {
                    include = false; // reset inclusion, must match seller
                    if (cb.getRelatedParty() != null) {
                        for (RelatedParty rp : cb.getRelatedParty()) {
                            if (sellerId.equals(rp.getId()) && "Seller".equalsIgnoreCase(rp.getRole())) {
                                include = true; // matched seller
                                break; // no need to continue checking other related parties
                            }
                        }
                    }
                }

                // add to output if passed all applicable filters
                if (include) {
                    out.add(getBomFor(cb.getId()));
                }
            }
        } catch (ApiException e) {
            throw new ExternalServiceException(e.getMessage(), e);
        }

        logger.debug("Retrieved {} BOMs for buyerId={} and sellerId={}", out.size(), buyerId, sellerId);
        return out;
    }

    public Envelope<InvoiceBom> getBomFor(String customerBillId) throws ExternalServiceException {

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

        // Q: which billing accounts to retrieve for those organisations?
        try {
            // add billing account (for each organization referenced within the CB)
            HashMap<String, String> sellerFilter = new HashMap<>();
            String sellerId = bom.getOrganizationWithRole("Seller").getId();
            sellerFilter.put("relatedParty.id", sellerId);
            List<BillingAccount> sellerBAs = this.accountManagementAPI.listBillingAccounts(null, 0, 100, sellerFilter);
//            if (sellerBAs.size() == 1
            //FIXME: take the first one only for now
            bom.add(sellerBAs.get(0), "Seller");

            HashMap<String, String> buyerFilter = new HashMap<>();
            String buyerId = bom.getOrganizationWithRole("Buyer").getId();
            buyerFilter.put("relatedParty.id", buyerId);
            List<BillingAccount> buyerBAs = this.accountManagementAPI.listBillingAccounts(null, 0, 100, buyerFilter);
//            if (buyerBAs.size() == 1)
            //FIXME: take the first one only for now
            bom.add(buyerBAs.get(0), "Buyer");

           /*BillingAccount buyerBA = this.accountManagementAPI.getBillingAccount(bom.getCustomerBill().getBillingAccount().getId(), null);
           bom.add(buyerBA, "Buyer");*/
        } catch (it.eng.dome.tmforum.tmf666.v4.ApiException e) {
            throw new ExternalServiceException(e.getMessage(), e);
        }

        // TODO: add POPs
        // Q: are they needed?

        //FIXME: rename the invoiceId parameter to something meaningful
        String lastPart = customerBillId.substring(customerBillId.lastIndexOf(":") + 1);
        String invoiceId = "inv-" + lastPart.replaceAll("[^A-Za-z0-9]", "");
        return new Envelope<>(bom, invoiceId, "bom");
    }
}
