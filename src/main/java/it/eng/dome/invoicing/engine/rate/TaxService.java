package it.eng.dome.invoicing.engine.rate;

import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.eng.dome.tmforum.tmf622.v4.model.Money;
import it.eng.dome.tmforum.tmf622.v4.model.OrderPrice;
import it.eng.dome.tmforum.tmf622.v4.model.Price;
import it.eng.dome.tmforum.tmf622.v4.model.PriceAlteration;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrder;
import it.eng.dome.tmforum.tmf622.v4.model.ProductOrderItem;
import it.eng.dome.tmforum.tmf622.v4.model.ProductPrice;
import it.eng.dome.tmforum.tmf622.v4.model.RelatedParty;

@Component(value = "taxService")
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TaxService implements InitializingBean {

    private final Logger logger = LoggerFactory.getLogger(TaxService.class);

    @Autowired
    private RateManager rateManager;

    @Override
    public void afterPropertiesSet() throws Exception {
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

    private RelatedParty getSeller(ProductOrder order) {
        return this.extractRelatedPartyIdByRole(order, "seller");
    }

    private RelatedParty getBuyer(ProductOrder order) {
        return this.extractRelatedPartyIdByRole(order, "buyer");
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

}
