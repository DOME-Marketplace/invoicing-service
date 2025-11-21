package it.eng.dome.invoicing.engine.service.render;

import java.util.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;

import it.eng.dome.tmforum.tmf666.v4.model.BillingAccount;
import it.eng.dome.tmforum.tmf666.v4.model.Contact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.eng.dome.invoicing.engine.model.InvoiceBom;
import it.eng.dome.tmforum.tmf632.v4.model.Characteristic;
import it.eng.dome.tmforum.tmf632.v4.model.Organization;
import it.eng.dome.tmforum.tmf637.v4.model.Product;
import it.eng.dome.tmforum.tmf678.v4.model.AppliedCustomerBillingRate;
import it.eng.dome.tmforum.tmf678.v4.model.CustomerBill;
import peppol.bis.invoice3.domain.AccountingCustomerParty;
import peppol.bis.invoice3.domain.AccountingSupplierParty;
import peppol.bis.invoice3.domain.ClassifiedTaxCategory;
import peppol.bis.invoice3.domain.CompanyID;
import peppol.bis.invoice3.domain.Country;
import peppol.bis.invoice3.domain.EndpointID;
import peppol.bis.invoice3.domain.ID;
import peppol.bis.invoice3.domain.Invoice;
import peppol.bis.invoice3.domain.InvoiceLine;
import peppol.bis.invoice3.domain.InvoicedQuantity;
import peppol.bis.invoice3.domain.Item;
import peppol.bis.invoice3.domain.LegalMonetaryTotal;
import peppol.bis.invoice3.domain.LineExtensionAmount;
import peppol.bis.invoice3.domain.Party;
import peppol.bis.invoice3.domain.PartyIdentification;
import peppol.bis.invoice3.domain.PartyLegalEntity;
import peppol.bis.invoice3.domain.PartyName;
import peppol.bis.invoice3.domain.PartyTaxScheme;
import peppol.bis.invoice3.domain.PayableAmount;
import peppol.bis.invoice3.domain.PostalAddress;
import peppol.bis.invoice3.domain.Price;
import peppol.bis.invoice3.domain.PriceAmount;
import peppol.bis.invoice3.domain.SellersItemIdentification;
import peppol.bis.invoice3.domain.TaxAmount;
import peppol.bis.invoice3.domain.TaxCategory;
import peppol.bis.invoice3.domain.TaxExclusiveAmount;
import peppol.bis.invoice3.domain.TaxInclusiveAmount;
import peppol.bis.invoice3.domain.TaxScheme;
import peppol.bis.invoice3.domain.TaxSubtotal;
import peppol.bis.invoice3.domain.TaxTotal;
import peppol.bis.invoice3.domain.TaxableAmount;

/**
 * BomToPeppol
 *
 * Lightweight converter from an internal InvoiceBom to a PEPPOL BIS Invoice object.
 *
 * Simplified behavior:
 * - Extracts a usable organisation identifier (VAT / org id / external reference / email) from the TMF Organization.
 * - Chooses a CEF EAS scheme based on country/format (basic mapping).
 * - If an identifier is available it is used to build EndpointID and PartyLegalEntity CompanyID.
 *   If no identifier is found the EndpointID and PartyTaxScheme are omitted (safer than inserting URNs).
 * - If any invoice line is classified 'O' (not subject) then PartyTaxScheme (VAT CompanyID) is not added
 *   to supplier/customer to satisfy BR-O-02.
 * - Computes TaxSubtotals and TaxTotal, re-computes TaxInclusive.
 *
 * This class is intentionally compact and uses a small set of helpers. It includes Javadoc on public methods.
 */
public class BomToPeppol {

    private static final Logger LOG = LoggerFactory.getLogger(BomToPeppol.class);
    private static final int SCALE = 2;
    private static final String EXEMPTION_TEXT = "Not subject to VAT";

    public Collection<Invoice> render(Collection<InvoiceBom> boms) {
        Collection<Invoice> out = new ArrayList<>();
        for (InvoiceBom bom : boms) {
            out.add(this.render(bom));
        }
        return out;
    }

    /**
     * Convert InvoiceBom to PEPPOL Invoice object.
     *
     * @param bom input domain invoice
     * @return peppol.bis.invoice3.domain.Invoice ready to be serialized
     */
    public Invoice render(InvoiceBom bom) {
        if (bom == null) throw new IllegalArgumentException("InvoiceBom cannot be null");

        Organization supplierOrg = bom.getOrganizationWithRole("Seller");
        Organization customerOrg = bom.getOrganizationWithRole("Buyer");
        BillingAccount sellerBA = bom.getBillingAccountWithRole("Seller");
        BillingAccount buyerBA = bom.getBillingAccountWithRole("Buyer");

        // extract best candidate identifier and country
        String supplierId = extractIdentifier(supplierOrg);
        String customerId = extractIdentifier(customerOrg);
        String supplierCountry = extractCountryCode(supplierOrg);
        String customerCountry = extractCountryCode(customerOrg);

        // choose scheme only when we have an identifier
        String supplierScheme = supplierId != null ? schemeFor(supplierCountry, supplierId) : null;
        String customerScheme = customerId != null ? schemeFor(customerCountry, customerId) : null;

        // detect if any line will be classified as 'O' and supplier identifier is missing
        boolean hasO = containsZeroRatedLineWithoutSupplierId(bom, supplierId);
        boolean includePartyTaxScheme = !hasO;

        AccountingSupplierParty supplier = buildSupplierParty(supplierOrg, supplierId, supplierScheme, includePartyTaxScheme, sellerBA);
        AccountingCustomerParty customer = buildCustomerParty(customerOrg, customerId, customerScheme, includePartyTaxScheme, buyerBA);

        CustomerBill cb = bom.getCustomerBill();

        // Tax subtotals construction
        List<TaxSubtotal> taxSubtotals = new java.util.ArrayList<>();
        BigDecimal taxTotal = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);

        if (cb.getTaxItem() != null) {
            for (var ti : cb.getTaxItem()) {
                BigDecimal taxAmt = toBD(ti.getTaxAmount() != null ? ti.getTaxAmount().getValue() : null);
                BigDecimal taxableAmount = toBD(cb.getTaxExcludedAmount() != null ? cb.getTaxExcludedAmount().getValue() : null);
                String unit = (ti.getTaxAmount() != null && ti.getTaxAmount().getUnit() != null)
                        ? ti.getTaxAmount().getUnit()
                        : (cb.getTaxExcludedAmount() != null ? cb.getTaxExcludedAmount().getUnit() : null);

                TaxableAmount ta = new TaxableAmount(fmtMoney(taxableAmount), unit);
                TaxAmount tA = new TaxAmount(fmtMoney(taxAmt), unit);

                BigDecimal pct = ti.getTaxRate() != null ? toBD(ti.getTaxRate()).multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
                String cat = pct.compareTo(BigDecimal.ZERO) > 0 ? "S" : (supplierId != null ? "Z" : "O");

                TaxCategory taxCategory = new TaxCategory(cat, new TaxScheme(ti.getTaxCategory() != null ? ti.getTaxCategory() : "VAT"));
                if (!"O".equals(cat)) taxCategory.withPercent(fmtMoney(pct));
                else addTaxExemptionReasonIfSupported(taxCategory, EXEMPTION_TEXT);

                TaxSubtotal ts = new TaxSubtotal(ta, tA, taxCategory);
                taxSubtotals.add(ts);
                taxTotal = taxTotal.add(taxAmt);
            }
        }

        TaxTotal tt = new TaxTotal(new TaxAmount(fmtMoney(taxTotal),
                cb.getTaxIncludedAmount() != null ? cb.getTaxIncludedAmount().getUnit() : null));
        for (TaxSubtotal s : taxSubtotals) tt.withTaxSubtotal(s);

        // legal totals
        LegalMonetaryTotal legal = createLegalMonetaryTotal(cb, taxTotal);

        // invoice lines
        List<InvoiceLine> lines = new java.util.ArrayList<>();
        if (bom.getAppliedCustomerBillingRates() != null) {
            for (AppliedCustomerBillingRate acbr : bom.getAppliedCustomerBillingRates()) {
                Product product = bom.getProduct(acbr.getProduct().getId());
                BigDecimal ratePct = BigDecimal.ZERO;
                String taxSchemeId = "VAT";
                if (acbr.getAppliedTax() != null && !acbr.getAppliedTax().isEmpty()) {
                    if (acbr.getAppliedTax().get(0).getTaxRate() != null) {
                        ratePct = toBD(acbr.getAppliedTax().get(0).getTaxRate()).multiply(BigDecimal.valueOf(100));
                    }
                    if (acbr.getAppliedTax().get(0).getTaxCategory() != null) {
                        taxSchemeId = acbr.getAppliedTax().get(0).getTaxCategory();
                    }
                }

                String classified = ratePct.compareTo(BigDecimal.ZERO) > 0 ? "S" : (supplierId != null ? "Z" : "O");
                ClassifiedTaxCategory tc = new ClassifiedTaxCategory(classified, new TaxScheme(taxSchemeId));
                if (!"O".equals(classified)) tc.withPercent(fmtMoney(ratePct));
                else addTaxExemptionReasonIfSupported(tc, EXEMPTION_TEXT);

                Item item = new Item(product != null ? product.getName() : "Product", tc)
                        .withSellersItemIdentification(new SellersItemIdentification(acbr.getId()));

                Price price = new Price(new PriceAmount(fmtMoney(toBD(acbr.getTaxExcludedAmount().getValue())),
                        acbr.getTaxExcludedAmount().getUnit()));

                InvoiceLine line = new InvoiceLine(acbr.getId(),
                        new InvoicedQuantity("1", "EA"),
                        new LineExtensionAmount(fmtMoney(toBD(acbr.getTaxExcludedAmount().getValue())), acbr.getTaxExcludedAmount().getUnit()),
                        item, price);

                lines.add(line);
            }
        }

        return new Invoice(
                cb.getId(),
                cb.getBillDate().toLocalDate().toString(),
                cb.getAmountDue().getUnit(),
                supplier,
                customer,
                tt,
                legal,
                lines
        ).withInvoiceTypeCode(380)
         .withBuyerReference("n/a")
         .withDueDate(cb.getPaymentDueDate().toLocalDate().toString());
    }

    // -----------------------
    // Helpers (kept small)
    // -----------------------

    /** Build supplier party. If identifier is null the EndpointID and CompanyID are omitted. */
    private AccountingSupplierParty buildSupplierParty(Organization org, String id, String scheme, boolean includeTaxScheme, BillingAccount ba) {
        EndpointID endpoint = null;
        if (id != null && !id.isBlank() && scheme != null) {
            endpoint = new EndpointID(id).withSchemeID(scheme);
        }

        PartyLegalEntity ple = new PartyLegalEntity(org != null ? org.getTradingName() : null);
        if (id != null && !id.isBlank()) {
            ple.withCompanyID(new CompanyID(id).withSchemeID(scheme != null ? scheme : "9901"));
        }

        PostalAddress addr = new PostalAddress(new Country(extractCountryCode(org)))
                .withStreetName(this.getPostalAddressField(ba.getContact(), "street"))
                .withCityName(this.getPostalAddressField(ba.getContact(), "city"))
                .withPostalZone(this.getPostalAddressField(ba.getContact(), "postcode"));

        Party party = new Party(endpoint, addr, ple)
                .withPartyIdentification(new PartyIdentification(new ID(org != null ? org.getId() : null)))
                .withPartyName(new PartyName(org != null ? org.getTradingName() : null));

        if (includeTaxScheme && id != null && !id.isBlank()) {
            String companyId = (extractCountryCode(org) != null ? extractCountryCode(org).toUpperCase() : "") + id;
            party.withPartyTaxScheme(new PartyTaxScheme(companyId, new TaxScheme("VAT")));
        }

        return new AccountingSupplierParty(party);
    }

    /** Build customer party. Similar rules as for supplier. */
    private AccountingCustomerParty buildCustomerParty(Organization org, String id, String scheme, boolean includeTaxScheme, BillingAccount ba) {
        EndpointID endpoint = null;
        if (id != null && !id.isBlank() && scheme != null) {
            endpoint = new EndpointID(id).withSchemeID(scheme);
        }

        PartyLegalEntity ple = new PartyLegalEntity(org != null ? org.getTradingName() : null);
        if (id != null && !id.isBlank()) {
            ple.withCompanyID(new CompanyID(id).withSchemeID(scheme != null ? scheme : "9901"));
        }

        PostalAddress addr = new PostalAddress(new Country(extractCountryCode(org)))
                .withStreetName(this.getPostalAddressField(ba.getContact(), "street"))
                .withCityName(this.getPostalAddressField(ba.getContact(), "city"))
                .withPostalZone(this.getPostalAddressField(ba.getContact(), "postcode"));

        Party party = new Party(endpoint, addr, ple)
                .withPartyIdentification(new PartyIdentification(new ID(org != null ? org.getId() : null)))
                .withPartyName(new PartyName(org != null ? org.getTradingName() : null));

        if (includeTaxScheme && id != null && !id.isBlank()) {
            String companyId = (extractCountryCode(org) != null ? extractCountryCode(org).toUpperCase() : "") + id;
            party.withPartyTaxScheme(new PartyTaxScheme(companyId, new TaxScheme("VAT")));
        }

        return new AccountingCustomerParty(party);
    }

    /** Create LegalMonetaryTotal with recomputed tax inclusive. */
    private LegalMonetaryTotal createLegalMonetaryTotal(CustomerBill cb, BigDecimal taxTotalAmount) {
        BigDecimal taxExclusive = toBD(cb.getTaxExcludedAmount().getValue());
        BigDecimal inclusive = taxExclusive.add(taxTotalAmount).setScale(SCALE, RoundingMode.HALF_UP);

        return new LegalMonetaryTotal(
                new LineExtensionAmount(fmtMoney(taxExclusive), cb.getTaxExcludedAmount().getUnit()),
                new TaxExclusiveAmount(fmtMoney(taxExclusive), cb.getTaxExcludedAmount().getUnit()),
                new TaxInclusiveAmount(fmtMoney(inclusive), cb.getTaxExcludedAmount().getUnit()),
                new PayableAmount(fmtMoney(toBD(cb.getAmountDue().getValue())), cb.getAmountDue().getUnit())
        );
    }

    /**
     * Extract a best-effort identifier from Organization.
     * Searches partyCharacteristic, organizationIdentification (if present), externalReference, contactMedium email.
     * Returns cleaned identifier or null if none found.
     */
    private String extractIdentifier(Organization org) {
        if (org == null) return null;

        // 1) partyCharacteristic
        if (org.getPartyCharacteristic() != null) {
            for (Characteristic c : org.getPartyCharacteristic()) {
                if (c == null || c.getValue() == null) continue;
                String name = c.getName() != null ? c.getName().toLowerCase(Locale.ROOT) : "";
                String raw = c.getValue().toString().trim();
                if (raw.isEmpty()) continue;
                if (name.contains("vat") || name.contains("org") || name.contains("orgnr") || name.contains("id") || name.contains("company")) {
                    return raw.replaceAll("\\s+", "");
                }
            }
        }

        // 2) organizationIdentification (best-effort; depends on model)
        try {
            var orgIds = org.getOrganizationIdentification();
            if (orgIds != null) {
                for (var oi : orgIds) {
                    if (oi == null) continue;
                    Object idVal = null;
                    try { idVal = oi.getIdentificationId(); } catch (Throwable ignore) {}
 

                    if (idVal != null) {
                        String raw = idVal.toString().trim();
                        if (!raw.isEmpty()) {
                            String cleaned = raw.replaceFirst("^[a-zA-Z]+:[a-zA-Z0-9_-]+:", ""); // strip did: prefix
                            cleaned = cleaned.replaceAll("[^A-Za-z0-9@:\\.\\-_/+]", "");
                            if (!cleaned.isEmpty()) return cleaned;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOG.debug("organizationIdentification not accessible: {}", t.getMessage());
        }

        // 3) externalReference[].name
        try {
            var ext = org.getExternalReference();
            if (ext != null) {
                for (var er : ext) {
                    if (er == null) continue;
                    Object n = null;
                    try { n = er.getName(); } catch (Throwable ignore) {}
                    if (n != null) {
                        String raw = n.toString().trim();
                        if (!raw.isEmpty()) {
                            String cleaned = raw.replaceAll("[^A-Za-z0-9@:\\.\\-_/+]", "");
                            if (!cleaned.isEmpty()) return cleaned;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOG.debug("externalReference not accessible: {}", t.getMessage());
        }

        // 4) contactMedium email
        try {
            var cms = org.getContactMedium();
            if (cms != null) {
                for (var cm : cms) {
                    if (cm == null) continue;
                    try {
                        Object ch = cm.getCharacteristic();
                        if (ch != null) {
                            String email = null;
                            try { email = (String) ch.getClass().getMethod("getEmailAddress").invoke(ch); } catch (Throwable ignore) {}
                            if (email == null) {
                                try { email = (String) ch.getClass().getMethod("getEmail").invoke(ch); } catch (Throwable ignore) {}
                            }
                            if (email != null && !email.trim().isEmpty()) return email.trim();
                        }
                    } catch (Throwable ignore) {}
                }
            }
        } catch (Throwable t) {
            LOG.debug("contactMedium not accessible: {}", t.getMessage());
        }

        return null;
    }

    /** Choose a simple EAS scheme code based on country and identifier format. */
    private String schemeFor(String countryCode, String identifier) {
        if (identifier == null) return null;
        String cleaned = identifier.trim();

        // email
        if (cleaned.contains("@")) return "EM";

        // GLN (13 digits)
        if (cleaned.matches("\\d{13}")) return "0088";

        if (countryCode != null) {
            switch (countryCode.trim().toUpperCase()) {

                case "IT": return "0211";   
                case "ES": return "0060";   
                case "FR": return "0004";  
                case "DE": return "0088";  
                case "NL": return "0106";   
                case "SE": return "0007";   
                case "FI": return "0037";   
                case "NO": return "0192"; 
                case "DK": return "0195";   
                case "IS": return "0088";   
                case "BE": return "0208";  
                case "LU": return "0177";   
                case "PL": return "9920";
                case "CZ": return "9952";
                case "SK": return "9957";
                case "HU": return "9917";
                case "SI": return "9938";
                case "RO": return "9911";
                case "BG": return "9913";
                case "EE": return "9944";
                case "LT": return "9939";
                case "LV": return "9937";
                case "PT": return "9910";
                case "GR":
                case "EL": return "9912"; 
                case "MT": return "9915";  
                case "CY": return "9950";  
                case "HR": return "9940";  
                case "GB":
                case "UK": return "9932"; 
                case "CH": return "9925";  

                default:
                    return "9901"; 
            }
        }

        return "9901";
    }


    /**
     * If supplierId is missing and there is at least one zero-rated line, BR-O-02 applies.
     * Return true when invoice contains a zero-rated or missing-tax line AND supplierId is null.
     */
    private boolean containsZeroRatedLineWithoutSupplierId(InvoiceBom bom, String supplierId) {
        if (bom == null || bom.getAppliedCustomerBillingRates() == null) return false;
        if (supplierId != null && !supplierId.isBlank()) return false;
        for (AppliedCustomerBillingRate acbr : bom.getAppliedCustomerBillingRates()) {
            if (acbr.getAppliedTax() == null || acbr.getAppliedTax().isEmpty()) return true;
            Number r = acbr.getAppliedTax().get(0).getTaxRate();
            if (r == null || BigDecimal.valueOf(r.doubleValue()).compareTo(BigDecimal.ZERO) == 0) return true;
        }
        return false;
    }

    // Reflection helper: try to add TaxExemptionReason if supported by library
    private void addTaxExemptionReasonIfSupported(Object taxCategoryObj, String reason) {
        if (taxCategoryObj == null || reason == null) return;
        try {
            Method m = taxCategoryObj.getClass().getMethod("withTaxExemptionReason", String.class);
            m.invoke(taxCategoryObj, reason);
            return;
        } catch (Throwable ignored) {}
        try {
            Method m2 = taxCategoryObj.getClass().getMethod("withTaxExemptionReasonCode", String.class);
            m2.invoke(taxCategoryObj, reason);
        } catch (Throwable ignored) {}
    }

    private static BigDecimal toBD(Number n) {
        if (n == null) return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        return new BigDecimal(n.toString()).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private static String fmtMoney(BigDecimal bd) {
        return bd.setScale(SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    private String extractCountryCode(Organization org) {
        if (org == null || org.getPartyCharacteristic() == null) return null;
        for (Characteristic c : org.getPartyCharacteristic()) {
            if (c == null || c.getName() == null || c.getValue() == null) continue;
            if ("country".equalsIgnoreCase(c.getName())) return c.getValue().toString();
        }
        return null;
    }

    private String getPostalAddressField(List<Contact> contacts, String fieldName) {
        return contacts.stream()
                .flatMap(c -> c.getContactMedium().stream())
                .filter(cm -> "PostalAddress".equalsIgnoreCase(cm.getMediumType()))
                .map(cm -> {
                    switch (fieldName.toLowerCase()) {
                        case "street": return cm.getCharacteristic().getStreet1();
                        case "city": return cm.getCharacteristic().getCity();
                        case "postcode": return cm.getCharacteristic().getPostCode();
                        default: return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

}