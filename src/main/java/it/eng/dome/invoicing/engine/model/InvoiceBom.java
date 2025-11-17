package it.eng.dome.invoicing.engine.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import it.eng.dome.tmforum.tmf620.v4.model.ProductOffering;
import it.eng.dome.tmforum.tmf620.v4.model.ProductOfferingPrice;
import it.eng.dome.tmforum.tmf632.v4.model.Organization;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf666.v4.model.BillingAccount;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBill;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceBom {

    private CustomerBill customerBill;

    private Map<String, AppliedCustomerBillingRate> acbrs;

    private Map<String, Product> products;

    private Map<String, ProductOffering> productOfferings;

    private Map<String, ProductOfferingPrice> productOfferingPrices;

    private Map<String, Organization> organizations;

    private Map<String, BillingAccount> billingAccounts;

    private InvoiceBom() {
        this.acbrs = new HashMap<>();
        this.products = new HashMap<>();
        this.productOfferings = new HashMap<>();
        this.productOfferingPrices = new HashMap<>();
        this.organizations = new HashMap<>();
        this.billingAccounts = new HashMap<>();
    }

    public InvoiceBom(CustomerBill customerBill) {
        this();
        this.customerBill = customerBill;
    }

    public CustomerBill getCustomerBill() {
        return this.customerBill;
    }

    public Collection<AppliedCustomerBillingRate> getAppliedCustomerBillingRates() {
        return this.acbrs.values();
    }

    public AppliedCustomerBillingRate getAppliedCustomerBillingRate(String id) {
        return this.acbrs.get(id);
    }

    public Collection<Product> getProducts() {
        return this.products.values();
    }

    public Product getProduct(String id) {
        return this.products.get(id);
    }

    public Collection<ProductOffering> getProductOfferings() {
        return this.productOfferings.values();
    }

    public ProductOffering getProductOffering(String id) {
        return this.productOfferings.get(id);
    }

    public Collection<ProductOfferingPrice> getProductOfferingPrice() {
        return this.productOfferingPrices.values();
    }

    public ProductOfferingPrice getProductOfferingPrice(String id) {
        return this.productOfferingPrices.get(id);
    }

    public Collection<Organization> getOrganizations() {
        return this.organizations.values();
    }

    public Organization getOrganization(String id) {
        for(Organization o: this.organizations.values())
            if(o.getId().equals(id))
                return o;
        return null;
    }

    public Organization getOrganizationWithRole(String role) {
        return this.organizations.get(role);
    }

    public Collection<BillingAccount> getBillingAccounts() {
        return this.billingAccounts.values();
    }

    public BillingAccount getBillingAccounts(String id) {
        return this.billingAccounts.get(id);
    }

    public void add(Product product) {
        if(product!=null && product.getId()!=null)
            this.products.put(product.getId(), product);
    }

    public void add(ProductOffering offering) {
        if(offering!=null && offering.getId()!=null)
            this.productOfferings.put(offering.getId(), offering);
    }

    public void add(ProductOfferingPrice price) {
        if(price!=null && price.getId()!=null)
            this.productOfferingPrices.put(price.getId(), price);
    }

    public void add(Organization organization, String role) {
        if(organization!=null && organization.getId()!=null && role!=null)
            this.organizations.put(role, organization);
    }

    public void add(BillingAccount account) {
        if(account!=null && account.getId()!=null)
            this.billingAccounts.put(account.getId(), account);
    }

    public void add(AppliedCustomerBillingRate acbr) {
        if(acbr!=null && acbr.getId()!=null)
            this.acbrs.put(acbr.getId(), acbr);
    }

}
