package it.eng.dome.invoicing.engine.rate;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.invoicing.engine.tmf.TmfApiFactory;
import it.eng.dome.tmforum.tmf622.v4.model.Money;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;
import it.eng.dome.tmforum.tmf622.v4.model.ProductPrice;
import it.eng.dome.tmforum.tmf622.v4.model.RelatedParty;
import it.eng.dome.tmforum.tmf637.v4.api.ProductApi;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedBillingTaxRate;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.ProductRef;
import it.eng.dome.tmforum.tmf678.v4.model.TimePeriod;

@Component(value = "taxService")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TaxService implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(TaxService.class);

    @Autowired
    // Factory for TMF APIss
    private TmfApiFactory tmfApiFactory;
    // TMForum API to retrieve organisations
    private ProductApi productApi;

    @Autowired
    private RateManager rateManager;

    @Override
    public void afterPropertiesSet() throws Exception {
        final it.eng.dome.tmforum.tmf637.v4.ApiClient apiClient = tmfApiFactory.getTMF637ProductInventoryManagementApiClient();
        productApi = new ProductApi(apiClient);
    }

    public TaxService() {
    }

    public ProductOrder applyTaxes(ProductOrder order) throws Exception {

        // retrieve the rate
        float rate = this.rateManager.getVATRateFor(this.getBuyer(order), this.getSeller(order), Calendar.getInstance()).floatValue();
        logger.debug("retrieved rate is " + rate);

        // total price
        List<OrderPrice> orderPrices = order.getOrderTotalPrice();
        if(orderPrices!=null) {
            for(OrderPrice op:orderPrices) {
                this.applyTax(op, rate);
            }
        }

        // productOrderItems
        List<ProductOrderItem> productOrderItems = order.getProductOrderItem();
        if(productOrderItems!=null) {
            for(ProductOrderItem item:productOrderItems) {
                this.applyTax(item, rate);
            }
        }
        return order;        
    }

    public AppliedCustomerBillingRate[] applyTaxes(AppliedCustomerBillingRate[] bills) throws Exception {
        for(AppliedCustomerBillingRate bill: bills) {
            this.applyTaxes(bill);
        }
        return bills;
    }

    private AppliedCustomerBillingRate applyTaxes(AppliedCustomerBillingRate bill) throws Exception {

        // retrieve the involved parties
        List<RelatedParty> involvedParties = this.retrieveRelatedParties(bill);
        RelatedParty buyer = this.getBuyer(involvedParties);
        RelatedParty seller = this.getSeller(involvedParties);

        // retrieve the date of the bill
        Calendar billDate = this.extractDateForVAT(bill);

        // retrieve the VAT rate
        float rate = this.rateManager.getVATRateFor(buyer, seller, billDate).floatValue();
        logger.info("retrieved rate is " + rate);
        
        // retrieving the taxExcludedAmount
        it.eng.dome.tmforum.tmf678.v4.model.Money taxExcluded = bill.getTaxExcludedAmount();

        // applying taxes (VAT only, as of now)
        List<AppliedBillingTaxRate> appliedTaxes = new ArrayList<>();
        appliedTaxes.add(this.applyVatTax(taxExcluded, rate));
        bill.setAppliedTax(appliedTaxes);

        // and this should be the sum of the two above.
        bill.setTaxIncludedAmount(this.addTaxes(bill.getTaxExcludedAmount(), appliedTaxes));
        
        return bill;
    }

    private it.eng.dome.tmforum.tmf678.v4.model.Money addTaxes(it.eng.dome.tmforum.tmf678.v4.model.Money inMoney, List<AppliedBillingTaxRate> taxes) {
        it.eng.dome.tmforum.tmf678.v4.model.Money outMoney = new it.eng.dome.tmforum.tmf678.v4.model.Money();
        outMoney.setUnit(inMoney.getUnit());
        outMoney.setValue(inMoney.getValue());
        for(AppliedBillingTaxRate tax: taxes) {
            outMoney.setValue(outMoney.getValue()+tax.getTaxAmount().getValue());
        }
        return outMoney;
    }

    // TODO: what's the correct date to consider? period.start? period.end? Maybe we should make sure there's no VAT change in between.
    // and in case split the calculation.
    // TODO: in the case of a one-off purchase, is there any periodCoverage at all? 
    private Calendar extractDateForVAT(AppliedCustomerBillingRate bill) {
        TimePeriod pc = bill.getPeriodCoverage();
        if(pc!=null) {
            OffsetDateTime end = pc.getEndDateTime();
            if(end!=null)
                return toCalendar(end);
            OffsetDateTime start = pc.getStartDateTime();
            if(start!=null)
                return toCalendar(start);
        }
        logger.error("No PeriodCoverage found in bill; attempting with BillDate.");
        OffsetDateTime billDate = bill.getDate();
        if(billDate!=null) {
            return toCalendar(billDate);
        }
        logger.error("No BillDate found in bill; using current time.");
        return Calendar.getInstance();
    }

    private Calendar toCalendar(OffsetDateTime odt) {
        Calendar c = Calendar.getInstance();
        c.set(odt.getYear(), odt.getMonth().getValue(), odt.getDayOfMonth());
        return c;
    }

    private List<RelatedParty> retrieveRelatedParties(AppliedCustomerBillingRate bill) throws Exception {
        // retrieve the product
        ProductRef pref = bill.getProduct();
        if(pref!=null) {
            // retrieve the product
            String productId = pref.getId();
            Product product = productApi.retrieveProduct(productId, null);
            List<it.eng.dome.tmforum.tmf637.v4.model.RelatedParty> parties = product.getRelatedParty();
            if(parties!=null)
                return this.convert(parties);
            return new ArrayList<RelatedParty>();
        } else {
            throw new NoSuchElementException("No product referenced in the AppliedCustomerBillingRate " + bill.getId());            
        }
    }

    private RelatedParty convert(it.eng.dome.tmforum.tmf637.v4.model.RelatedParty inParty) throws IOException {
        return RelatedParty.fromJson(inParty.toJson());
    }

    private List<RelatedParty> convert(List<it.eng.dome.tmforum.tmf637.v4.model.RelatedParty> inList) throws IOException {
        List<RelatedParty> out = new ArrayList<>();
        for(it.eng.dome.tmforum.tmf637.v4.model.RelatedParty rp: inList) {
            out.add(this.convert(rp));
        }
        return out;
    }



    public ProductOrderItem applyTax(ProductOrderItem item, float taxRate) {
        List<OrderPrice> itemPrices = item.getItemPrice();
        if(itemPrices!=null) {
            for(OrderPrice op:itemPrices) {
                this.applyTax(op, taxRate);
            }
        }
        List<OrderPrice> itemTotalPrices = item.getItemTotalPrice();
        if(itemTotalPrices!=null) {
            for(OrderPrice op:itemTotalPrices) {
                this.applyTax(op, taxRate);
            }
        }
        return item;
    }

    public OrderPrice applyTax(OrderPrice op, float taxRate) {
        this.applyTax(op.getPrice(), taxRate);
        // and corresponding alterations
        List<PriceAlteration> alterations = op.getPriceAlteration();
        if(alterations!=null) {
            for(PriceAlteration pa:alterations) {
                this.applyTax(pa, taxRate);
            }
        }
        return op;
    }

    public PriceAlteration applyTax(PriceAlteration pa, float taxRate) {
        this.applyTax(pa.getPrice(), taxRate);
        return pa;
    }

    public ProductPrice applyTax(ProductPrice price, float taxRate) {
        this.applyTax(price.getPrice(), taxRate);
        // and corresponding alterations
        List<PriceAlteration> alterations = price.getProductPriceAlteration();
        if(alterations!=null) {
            for(PriceAlteration pa:alterations) {
                this.applyTax(pa, taxRate);
            }
        }
        return price;
    }

    public Price applyTax(Price inPrice, float taxRate) {
        // set the tax rate
        inPrice.setTaxRate(taxRate);

        // build the taxIncluded amonut
        Money taxIncluedeAmount = new Money();
        Money dutyFreeAmount = inPrice.getDutyFreeAmount();
        if(dutyFreeAmount!=null) {
            if(dutyFreeAmount.getUnit()!=null) {
                taxIncluedeAmount.setUnit(dutyFreeAmount.getUnit());
            }
            Float dutyFreeValue = dutyFreeAmount.getValue();
            if(dutyFreeValue!=null) {
                taxIncluedeAmount.setValue(dutyFreeValue*(1+taxRate));
            }
        }

        // update the price
        inPrice.setTaxIncludedAmount(taxIncluedeAmount);

        return inPrice;
    }

    private AppliedBillingTaxRate applyVatTax(it.eng.dome.tmforum.tmf678.v4.model.Money money, float taxRate) {
        AppliedBillingTaxRate out = new AppliedBillingTaxRate();
        out.setTaxCategory("VAT");
        out.setTaxRate(taxRate);
        it.eng.dome.tmforum.tmf678.v4.model.Money taxAmount = new it.eng.dome.tmforum.tmf678.v4.model.Money();
        taxAmount.setUnit(money.getUnit());
        taxAmount.setValue(money.getValue()*taxRate);
        out.setTaxAmount(taxAmount);
        return out;
    }

    private RelatedParty getSeller(ProductOrder order) {
        return this.extractRelatedPartyIdByRole(order, "seller");
    }

    private RelatedParty getSeller(List<RelatedParty> parties) {
        return this.extractRelatedPartyIdByRole(parties, "seller");
    }

    private RelatedParty getBuyer(ProductOrder order) {
        // try with both roles 'buyer' and 'customer'
        RelatedParty out = this.extractRelatedPartyIdByRole(order, "customer");
        if(out==null)
            out = this.extractRelatedPartyIdByRole(order, "buyer");
        return out;
    }

    private RelatedParty getBuyer(List<RelatedParty> parties) {
        // try with both roles 'buyer' and 'customer'
        RelatedParty out = this.extractRelatedPartyIdByRole(parties, "customer");
        if(out==null)
            out = this.extractRelatedPartyIdByRole(parties, "buyer");
        return out;
    }

    private RelatedParty extractRelatedPartyIdByRole(ProductOrder order, String role) {
        List<RelatedParty> parties = order.getRelatedParty();
        if(parties!=null) {
            for(RelatedParty party:parties) {
                if(role.equalsIgnoreCase(party.getRole())) {
                    return party;
                }
            }
        }
        return null;
    }

    private RelatedParty extractRelatedPartyIdByRole(List<RelatedParty> parties, String role) {
        if(parties!=null) {
            for(RelatedParty party:parties) {
                if(role.equalsIgnoreCase(party.getRole())) {
                    return party;
                }
            }
        }
        return null;
    }

}
