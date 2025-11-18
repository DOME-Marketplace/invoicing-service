package it.eng.dome.invoicing.engine.service.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import it.eng.dome.invoicing.engine.model.InvoiceBom;
import it.eng.dome.invoicing.engine.service.PeppolPlaceholder;
import it.eng.dome.tmforum.tmf632.v4.model.Characteristic;
import it.eng.dome.tmforum.tmf632.v4.model.Organization;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBill;
import peppol.bis.invoice3.domain.*;

public class BomToPeppol {

    // replace Object with the proper PEPPOL type
    public Invoice render(InvoiceBom bom) {
        if (bom == null) {
            throw new IllegalArgumentException("InvoiceBom cannot be null");
        }

        // ======== Supplier ========
        Organization supplierOrg = bom.getOrganizationWithRole("Seller");
        AccountingSupplierParty supplier = this.createSupplierParty(supplierOrg);


        // ======== Customer ========
        Organization customerOrg = bom.getOrganizationWithRole("Buyer");
        AccountingCustomerParty customer = this.createCustomerParty(customerOrg);

        // ===== Tax Total =====
        TaxTotal taxTotal = new TaxTotal(
                new TaxAmount(String.valueOf(bom.getCustomerBill().getTaxIncludedAmount().getValue()), bom.getCustomerBill().getTaxIncludedAmount().getUnit())
        );
        if (bom.getCustomerBill().getTaxItem() != null && !bom.getCustomerBill().getTaxItem().isEmpty()) {
            bom.getCustomerBill().getTaxItem().forEach(ti -> {
                taxTotal.withTaxSubtotal(new TaxSubtotal(
                        new TaxableAmount(String.valueOf(bom.getCustomerBill().getTaxExcludedAmount().getValue()), bom.getCustomerBill().getTaxExcludedAmount().getUnit()),
                        new TaxAmount(String.valueOf(ti.getTaxAmount().getValue()), ti.getTaxAmount().getUnit()),
                        new TaxCategory("S", new TaxScheme(ti.getTaxCategory())).withPercent(String.valueOf(ti.getTaxRate() * 100))
                ));
            });
        }

        // ===== Legal Monetary Total =====
        LegalMonetaryTotal legalTotal = this.createLegalMonetaryTotal(bom.getCustomerBill());

        // ===== Invoice Lines =====
        List<InvoiceLine> lines = new ArrayList<>();
        for (AppliedCustomerBillingRate acbr : bom.getAppliedCustomerBillingRates()) {
            Product product = bom.getProduct(acbr.getProduct().getId());

            ClassifiedTaxCategory taxCategory = new ClassifiedTaxCategory(
                    "S", new TaxScheme(acbr.getAppliedTax() != null && !acbr.getAppliedTax().isEmpty()
                    ? acbr.getAppliedTax().get(0).getTaxCategory() : "VAT")
            ).withPercent(String.valueOf(acbr.getAppliedTax() != null && !acbr.getAppliedTax().isEmpty()
                    ? acbr.getAppliedTax().get(0).getTaxRate() * 100 : 0));

            Item item = new Item(
                    product != null ? product.getName() : "Prodotto",
                    taxCategory
            ).withSellersItemIdentification(new SellersItemIdentification(acbr.getId()));

            Price price = new Price(new PriceAmount(
                    String.valueOf(acbr.getTaxExcludedAmount().getValue()),
                    acbr.getTaxExcludedAmount().getUnit()
            ));

            InvoiceLine line = new InvoiceLine(
                    acbr.getId(),
                    new InvoicedQuantity("1", "EA"),
                    new LineExtensionAmount(String.valueOf(acbr.getTaxExcludedAmount().getValue()), acbr.getTaxExcludedAmount().getUnit()),
                    item,
                    price
            );

            lines.add(line);
        }

        // ===== Invoice =====
        Invoice invoice = new Invoice(
                bom.getCustomerBill().getId(),
                bom.getCustomerBill().getBillDate().toLocalDate().toString(),
                bom.getCustomerBill().getAmountDue().getUnit(),
                supplier,
                customer,
                taxTotal,
                legalTotal,
                lines
        ).withInvoiceTypeCode(380) // Standard invoice
                .withBuyerReference("n/a")
                .withDueDate(bom.getCustomerBill().getPaymentDueDate().toLocalDate().toString());

        return invoice;
    }

    // replace Object with the proper PEPPOL type
    public Collection<Invoice> render(Collection<InvoiceBom> boms) {
        Collection<Invoice> out = new ArrayList<>();
        for(InvoiceBom bom: boms) {
            out.add(this.render(bom));
        }
        return out;
    }

    private AccountingSupplierParty createSupplierParty(Organization org) {
        return new AccountingSupplierParty(
                new Party(
                        new EndpointID(org.getId()).withSchemeID("0192"),
                        new PostalAddress(
                                new Country(this.extractCountryCode(org)))
                                .withStreetName("streetName") // FIXME: from billingAccount
                                .withCityName("city")         // FIXME: from billingAccount
                                .withPostalZone("POSTALCODE"), // FIXME: from billingAccount
                        new PartyLegalEntity(org.getTradingName())
                                .withCompanyID(new CompanyID(org.getId()).withSchemeID("0192"))
                ).withPartyIdentification(new PartyIdentification(new ID(org.getId())))
                        .withPartyName(new PartyName(org.getTradingName()))
        );
    }

    private AccountingCustomerParty createCustomerParty(Organization org) {
        return new AccountingCustomerParty(
                new Party(
                        new EndpointID(org.getId()).withSchemeID("0192"),
                        new PostalAddress(new Country(this.extractCountryCode(org)))
                                .withStreetName("streetName") // FIXME: from billingAccount
                                .withCityName("city")         // FIXME: from billingAccount
                                .withPostalZone("POSTALCODE"), // FIXME: from billingAccount
                        new PartyLegalEntity(org.getTradingName())
                                .withCompanyID(new CompanyID(org.getId()).withSchemeID("0192"))
                ).withPartyIdentification(new PartyIdentification(new ID(org.getId())))
                        .withPartyName(new PartyName(org.getTradingName()))
        );
    }

    private LegalMonetaryTotal createLegalMonetaryTotal(CustomerBill cb) {
        return new LegalMonetaryTotal(
                new LineExtensionAmount(String.valueOf(cb.getTaxExcludedAmount().getValue()), cb.getTaxExcludedAmount().getUnit()),
                new TaxExclusiveAmount(String.valueOf(cb.getTaxExcludedAmount().getValue()), cb.getTaxExcludedAmount().getUnit()),
                new TaxInclusiveAmount(String.valueOf(cb.getTaxIncludedAmount().getValue()), cb.getTaxIncludedAmount().getUnit()),
                new PayableAmount(String.valueOf(cb.getAmountDue().getValue()), cb.getAmountDue().getUnit())
        );
    }

    private String extractCountryCode(Organization org) {
        if (org.getPartyCharacteristic() != null) {
            for (Characteristic c : org.getPartyCharacteristic()) {
                if ("country".equalsIgnoreCase(c.getName())) {
                    return c.getValue().toString();
                }
            }
        } else {
            throw new IllegalStateException("PartyCharacteristic is null for organization: " + org.getId());
        }
        return null;
    }

}
