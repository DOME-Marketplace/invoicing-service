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
import java.util.*;

@Service
public class BomService {

    private final Logger logger = LoggerFactory.getLogger(BomService.class);

    private final APIPartyApis partyAPI;
    private final ProductInventoryApis productInventoryAPI;
    private final CustomerBillApis customerBillAPI;
    private final AppliedCustomerBillRateApis appliedCustomerBillingRateAPI;
    private final ProductCatalogManagementApis productCatalogManagementAPI;
    private final AccountManagementApis accountManagementAPI;

    public BomService(APIPartyApis partyAPI,
                      ProductInventoryApis productInventoryAPI,
                      CustomerBillApis customerBillAPI,
                      AppliedCustomerBillRateApis appliedCustomerBillingRateAPI,
                      ProductCatalogManagementApis productCatalogManagementAPI,
                      AccountManagementApis accountManagementAPI) {
        this.partyAPI = partyAPI;
        this.productInventoryAPI = productInventoryAPI;
        this.customerBillAPI = customerBillAPI;
        this.appliedCustomerBillingRateAPI = appliedCustomerBillingRateAPI;
        this.productCatalogManagementAPI = productCatalogManagementAPI;
        this.accountManagementAPI = accountManagementAPI;
    }

    public List<Envelope<InvoiceBom>> getBomsFor(String buyerId,
                                                 String sellerId,
                                                 OffsetDateTime fromDate,
                                                 OffsetDateTime toDate) throws ExternalServiceException {

        List<Envelope<InvoiceBom>> out = new ArrayList<>();

        Map<String, String> filter = new HashMap<>();
        if (buyerId != null) filter.put("relatedParty.id", buyerId);
        if (sellerId != null) filter.put("relatedParty.id", sellerId);
        if (fromDate != null) filter.put("billDate>= ", fromDate.truncatedTo(ChronoUnit.SECONDS).toString());
        if (toDate != null) filter.put("billDate<= ", toDate.truncatedTo(ChronoUnit.SECONDS).toString());

        try {
            List<CustomerBill> bills = this.customerBillAPI.listCustomerBills(null, 0, 1000, filter);
            logger.debug("Found {} Customer Bills between {} and {}", bills.size(), fromDate, toDate);

            for (CustomerBill cb : bills) {
                boolean include = true;

                // buyer filter
                if (buyerId != null) {
                    include = false;
                    if (cb.getRelatedParty() != null) {
                        for (RelatedParty rp : cb.getRelatedParty()) {
                            if (buyerId.equals(rp.getId()) && ("Buyer".equalsIgnoreCase(rp.getRole()) ||
                            		"Customer".equalsIgnoreCase(rp.getRole()))) {
                                include = true;
                                break;
                            }
                        }
                    }
                }

                // seller filter
                if (include && sellerId != null) {
                    include = false;
                    if (cb.getRelatedParty() != null) {
                        for (RelatedParty rp : cb.getRelatedParty()) {
                            if (sellerId.equals(rp.getId()) && "Seller".equalsIgnoreCase(rp.getRole())) {
                                include = true;
                                break;
                            }
                        }
                    }
                }

                if (include) {
                    out.add(getBomFor(cb.getId()));
                }
            }

        } catch (ApiException e) {
            logger.error("Error retrieving Customer Bills: {}", e.getMessage());
            throw new ExternalServiceException(e.getMessage(), e);
        }

        logger.debug("Retrieved {} BOMs for buyerId={} and sellerId={}", out.size(), buyerId, sellerId);
        return out;
    }

    public Envelope<InvoiceBom> getBomFor(String customerBillId) throws ExternalServiceException {

        InvoiceBom bom;

        // 1. Customer Bill
        try {
            CustomerBill cb = this.customerBillAPI.getCustomerBill(customerBillId, null);
            bom = new InvoiceBom(cb);
        } catch (ApiException e) {
            logger.error("Error retrieving Customer Bill with id {}: {}", customerBillId, e.getMessage());
            throw new ExternalServiceException(e.getMessage(), e);
        }

        // 2. ACBR
        try {
            Map<String, String> filter = Map.of("bill.id", bom.getCustomerBill().getId());
            List<AppliedCustomerBillingRate> acbrs =
                    this.appliedCustomerBillingRateAPI.listAppliedCustomerBillingRates(null, 0, 1000, filter);

            for (AppliedCustomerBillingRate acbr : acbrs) {
                bom.add(acbr);
            }

        } catch (ApiException e) {
            throw new ExternalServiceException(e.getMessage(), e);
        }

        // 3. Products
        try {
            for (AppliedCustomerBillingRate acbr : bom.getAppliedCustomerBillingRates()) {
                if (acbr.getProduct() != null && acbr.getProduct().getId() != null) {
                    bom.add(this.productInventoryAPI.getProduct(acbr.getProduct().getId(), null));
                }
            }
        } catch (it.eng.dome.tmforum.tmf637.v4.ApiException e) {
            throw new ExternalServiceException(e.getMessage(), e);
        }

        // 4. Product Offerings
        try {
            for (Product product : bom.getProducts()) {
                if (product.getProductOffering() != null && product.getProductOffering().getId() != null) {
                    ProductOffering offering =
                            this.productCatalogManagementAPI.getProductOffering(product.getProductOffering().getId(), null);
                    bom.add(offering);
                }
            }
        } catch (it.eng.dome.tmforum.tmf620.v4.ApiException e) {
            throw new ExternalServiceException(e.getMessage(), e);
        }

        // 5. Organizations
        try {
            if (bom.getCustomerBill().getRelatedParty() != null) {
                for (RelatedParty party : bom.getCustomerBill().getRelatedParty()) {
                    if (party.getRole() != null && party.getId() != null) {
                        Organization organization = this.partyAPI.getOrganization(party.getId(), null);
                        bom.add(organization, party.getRole());
                    }
                }
            }
        } catch (it.eng.dome.tmforum.tmf632.v4.ApiException e) {
            throw new ExternalServiceException(e.getMessage(), e);
        }

        // fallback Buyer -> Customer
        Organization buyerOrg = getBuyerOrCustomer(bom);
        if (buyerOrg == null) {
            throw new ExternalServiceException("No Buyer or Customer found for bill " + bom.getCustomerBill().getId());
        }

        Organization sellerOrg = bom.getOrganizationWithRole("Seller");
        if (sellerOrg == null) {
            throw new ExternalServiceException("No Seller found for bill " + bom.getCustomerBill().getId());
        }

        // 6. Billing Accounts
        try {
            // Seller
            Map<String, String> sellerFilter = new HashMap<>();
            sellerFilter.put("relatedParty.id", sellerOrg.getId());

            List<BillingAccount> sellerBAs =
                    this.accountManagementAPI.listBillingAccounts(null, 0, 1000, sellerFilter);

            if (sellerBAs.isEmpty()) {
                logger.warn("No Billing Account found for Seller with id {}", sellerOrg.getId());
            } else {
                bom.add(sellerBAs.get(0), "Seller");
            }

            // Buyer/Customer
            Map<String, String> buyerFilter = new HashMap<>();
            buyerFilter.put("relatedParty.id", buyerOrg.getId());

            List<BillingAccount> buyerBAs =
                    this.accountManagementAPI.listBillingAccounts(null, 0, 1000, buyerFilter);

            if (buyerBAs.isEmpty()) {
                logger.warn("No Billing Account found for Buyer/Customer with id {}", buyerOrg.getId());
            } else {
                bom.add(buyerBAs.get(0), "Buyer");
            }

        } catch (it.eng.dome.tmforum.tmf666.v4.ApiException e) {
            logger.error("Error retrieving Billing Account: {}", e.getMessage());
            throw new ExternalServiceException(e.getMessage(), e);
        }

        // 7. Folder name
        String buyerName = buyerOrg.getTradingName() != null ? buyerOrg.getTradingName() : "UNKNOWN_BUYER";
        String sellerName = sellerOrg.getTradingName() != null ? sellerOrg.getTradingName() : "UNKNOWN_SELLER";
        String date = bom.getCustomerBill().getBillDate().toLocalDate().toString();

        String folderName = "Invoice from " + sellerName + " to " + buyerName + " on " + date;

        return new Envelope<>(bom, folderName, "bom");
    }

    private Organization getBuyerOrCustomer(InvoiceBom bom) {
        Organization org = bom.getOrganizationWithRole("Buyer");
        if (org == null) {
            org = bom.getOrganizationWithRole("Customer");
        }
        return org;
    }
}