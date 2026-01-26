package it.eng.dome.invoicing.engine. service.render;

import java.util.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

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
import peppol.bis.invoice3.domain.*;


public class BomToPeppol {

    private static final Logger LOG = LoggerFactory.getLogger(BomToPeppol. class);
    
    private static final int SCALE = 2;
    
    private static final String EXEMPTION_TEXT = "Not subject to VAT";
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$");
    
    private static final Pattern GLN_PATTERN = Pattern.compile("^\\d{13}$");
    
    private static final Pattern DUNS_PATTERN = Pattern.compile("^\\d{9}$");

    private static final Set<String> EU_COUNTRIES = new HashSet<>(Arrays.asList(
            "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "EL",
            "HU", "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE"
    ));

    private static final Set<String> EFTA_COUNTRIES = new HashSet<>(Arrays.asList("NO", "IS", "LI", "CH"));

    // ==========================================
    // MAIN RENDERING
    // ==========================================

    /**
     * Renders multiple invoice BOMs to PEPPOL invoices. 
     *
     * @param envBoms collection of invoice BOM envelopes
     * @return collection of PEPPOL invoice envelopes
     * @throws RuntimeException if rendering fails for any invoice
     */
    public Collection<Envelope<Invoice>> render(Collection<Envelope<InvoiceBom>> envBoms) {
        Collection<Envelope<Invoice>> out = new ArrayList<>();
        if (envBoms != null) {
            for (Envelope<InvoiceBom> e : envBoms) {
                try {
                    out.add(this.render(e));
                } catch (Exception ex) {
                    LOG.error("Failed to render invoice: {}", e != null ? e.getName() : "null", ex);
                    throw ex;
                }
            }
        }
        return out;
    }

    /**
	 * Renders a single invoice BOM to a PEPPOL invoice.
     * @param envBom invoice BOM envelope containing source data
     * @return PEPPOL invoice envelope
     * @throws IllegalArgumentException if envelope or content is null
     * @throws IllegalStateException if required organizations are missing
     */
	public Envelope<Invoice> render(Envelope<InvoiceBom> envBom) {
	    if (envBom == null) {
	        throw new IllegalArgumentException("InvoiceBom envelope cannot be null");
	    }
	
	    InvoiceBom bom = envBom.getContent();
	    if (bom == null) {
	        throw new IllegalArgumentException("InvoiceBom content cannot be null");
	    }
	
	    LOG.info("Rendering EU invoice: {}", envBom.getName());
	
	    // Extract organizations and billing accounts
	    Organization supplierOrg = bom.getOrganizationWithRole("Seller");
	    Organization customerOrg = bom.getOrganizationWithRole("Buyer");
	    BillingAccount sellerBA = bom.getBillingAccountWithRole("Seller");
	    BillingAccount buyerBA = bom.getBillingAccountWithRole("Buyer");
	
	    if (supplierOrg == null || customerOrg == null) {
	        throw new IllegalStateException("Supplier and Customer organizations are mandatory");
	    }
	
	    // Extract countries
	    String supplierCountry = extractCountryCode(supplierOrg);
	    String customerCountry = extractCountryCode(customerOrg);
	    LOG.debug("Countries - Supplier: {}, Customer: {}", supplierCountry, customerCountry);
	
	    // Validate EU membership (warning only)
	    validateEUCountry(supplierCountry, "Supplier");
	    validateEUCountry(customerCountry, "Customer");
	
	    // Extract identifiers
	    String supplierId = extractIdentifier(supplierOrg, supplierCountry);
	    String customerId = extractIdentifier(customerOrg, customerCountry);
	    LOG.info("Identifiers - Supplier: '{}', Customer: '{}'", supplierId, customerId);
	
	    // Select validator-safe schemes
	    String supplierScheme = selectEUScheme(supplierId, supplierCountry);
	    String customerScheme = selectEUScheme(customerId, customerCountry);
	    LOG.info("Schemes - Supplier: {}, Customer: {}", supplierScheme, customerScheme);
	
	    // Check for tax exemption scenarios
	    boolean hasExemption = containsZeroRatedLineWithoutSupplierId(bom, supplierId);
	    boolean includePartyTaxScheme = !hasExemption;
	
	    if (hasExemption) {
	        LOG.info("Tax exemption detected - PartyTaxScheme will be omitted (BR-O-02)");
	    }
	
	    // Build parties
	    AccountingSupplierParty supplier = buildSupplierParty(supplierOrg, supplierId, supplierScheme, includePartyTaxScheme, sellerBA);
	    AccountingCustomerParty customer = buildCustomerParty(customerOrg, customerId, customerScheme, includePartyTaxScheme, buyerBA);
	
	    CustomerBill cb = bom.getCustomerBill();
	    if (cb == null) {
	        throw new IllegalStateException("CustomerBill cannot be null");
	    }
	
	    // Build tax totals (calculate from lines, not from CustomerBill)
	    TaxCalculationResult taxResult = calculateTaxTotals(cb, supplierId, bom);
	
	    // Build legal monetary total
	    LegalMonetaryTotal legal = createLegalMonetaryTotal(cb, taxResult.getTaxTotal(), bom);
	
	    // Build invoice lines
	    List<InvoiceLine> lines = createInvoiceLines(bom, supplierId);
	
	    String currency = getCurrency(cb);
	
	    // Build invoice with REQUIRED fields
	    Invoice invoice = new Invoice(
	            cb.getId(),
	            cb.getBillDate().toLocalDate().toString(),
	            currency,
	            supplier,
	            customer,
	            taxResult.getTaxTotalObject(),
	            legal,
	            lines
	    ).withInvoiceTypeCode(380);
	
	    // OPTIONAL: DueDate - only if present
	    if (cb.getPaymentDueDate() != null) {
	        invoice.withDueDate(cb.getPaymentDueDate().toLocalDate().toString());
	    }else {
	    	invoice.withDueDate(cb.getBillDate().toLocalDate().toString());
	        LOG.info("DueDate not present - used BillDate as fallback");
	    }
	
	    // OPTIONAL: BuyerReference - only if present
	    if (customerOrg.getTradingName() != null && !customerOrg.getTradingName().isBlank()) {
	        invoice.withBuyerReference(customerOrg.getTradingName());
	    }else {
	    	invoice.withBuyerReference("N/A");
	        LOG.info("BuyerReference not present - omitted");
	    }
	
	    // Add invoice notes
	    addInvoiceNotes(invoice, supplierCountry, bom, supplierId);
	
	    LOG.info("EU invoice rendered successfully: {}", cb.getId());
	    return new Envelope<>(invoice, envBom.getName(), envBom.getFormat());
	}

	/**
	 * Extracts currency from CustomerBill with fallback to EUR.
	 * Currency is REQUIRED in PEPPOL, so fallback to EUR is acceptable.
	 */
	private String getCurrency(CustomerBill cb) {
	    if (cb.getTaxIncludedAmount() != null && cb.getTaxIncludedAmount().getUnit() != null) {
	        return cb.getTaxIncludedAmount().getUnit();
	    }
	    if (cb.getTaxExcludedAmount() != null && cb.getTaxExcludedAmount().getUnit() != null) {
	        return cb.getTaxExcludedAmount().getUnit();
	    }
	    LOG.warn("No currency found in CustomerBill - using EUR as default");
	    return "EUR";
	}
    // ==========================================
    // EU COUNTRY VALIDATION
    // ==========================================

    /**
     * Validates if a country code belongs to EU/EFTA/UK.
     * Logs warnings for missing or unsupported countries.
     *
     * @param countryCode ISO 3166-1 alpha-2 country code
     * @param role party role ("Supplier" or "Customer")
     */
    private void validateEUCountry(String countryCode, String role) {
        if (countryCode == null || countryCode.isBlank()) {
            LOG.warn("{} country code is missing", role);
            return;
        }

        String cc = countryCode.toUpperCase();
        boolean isEU = EU_COUNTRIES. contains(cc);
        boolean isEFTA = EFTA_COUNTRIES.contains(cc);
        boolean isUK = "GB".equals(cc) || "UK".equals(cc);

        if (!isEU && !isEFTA && !isUK) {
            LOG.warn("{} country '{}' is not EU/EFTA/UK - may have limited support", role, cc);
        } else {
            LOG.debug("{} country '{}' is valid (EU: {}, EFTA:  {}, UK: {})", role, cc, isEU, isEFTA, isUK);
        }
    }

    // ==========================================
    // IDENTIFIER EXTRACTION (Multi-source)
    // ==========================================

    /**
     * Extracts and normalizes organization identifier from multiple sources.
     *
     * @param org organization entity
     * @param countryCode ISO 3166-1 alpha-2 country code for normalization
     * @return normalized identifier or null if not found
     */
    private String extractIdentifier(Organization org, String countryCode) {
        if (org == null) {
            LOG.warn("Organization is null - cannot extract identifier");
            return null;
        }

        // Source 1: externalReference
        String id = extractFromExternalReference(org);
        if (id != null) {
            return normalizeIdentifier(id, countryCode);
        }

        // Source 2: contactMedium (email)
        id = extractEmailFromContactMedium(org);
        if (id != null) {
            return id; // Email doesn't need normalization
        }

        LOG.warn("No identifier found for organization: {}", org.getId());
        return null;
    }

    /**
     * Extracts identifier from organization's externalReference field.
     *
     * @param org organization entity
     * @return raw identifier or null if not found
     */
    private String extractFromExternalReference(Organization org) {
        try {
            var ext = org.getExternalReference();
            if (ext != null) {
                for (var er : ext) {
                    if (er == null) continue;
                    Object n = null;
                    try {
                        n = er. getName();
                    } catch (Throwable ignore) {}
                    if (n != null) {
                        String raw = n.toString().trim();
                        if (!raw.isEmpty()) {
                            return raw;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOG.debug("externalReference not accessible:  {}", t.getMessage());
        }
        return null;
    }

    /**
     * Extracts email address from organization's contactMedium field.
     *
     * @param org organization entity
     * @return email address or null if not found or invalid
     */
    private String extractEmailFromContactMedium(Organization org) {
        try {
            var contacts = org.getContactMedium();
            if (contacts != null) {
                for (var contact : contacts) {
                    if ("Email".equalsIgnoreCase(contact.getMediumType())) {
                        var chars = contact.getCharacteristic();
                        if (chars != null && chars.getEmailAddress() != null) {
                            String email = chars.getEmailAddress().trim();
                            if (EMAIL_PATTERN.matcher(email).matches()) {
                                return email;
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOG.debug("contactMedium not accessible: {}", t.getMessage());
        }
        return null;
    }

    /**
     * Normalizes identifier by removing common EU VAT prefixes and separators.
     *
     * @param raw raw identifier string
     * @param countryCode ISO 3166-1 alpha-2 country code
     * @return normalized identifier or null if empty after normalization
     */
    private String normalizeIdentifier(String raw, String countryCode) {
        if (raw == null || raw.isBlank()) return null;

        String cleaned = raw.trim().toUpperCase();
        LOG.debug("Normalizing identifier: '{}' (country: {})", cleaned, countryCode);

        // Remove "VAT" prefix (common in EU)
        if (cleaned.startsWith("VAT")) {
            cleaned = cleaned. substring(3);
        }

        // Remove country code prefix if present
        if (countryCode != null && ! countryCode.isBlank()) {
            String cc = countryCode.toUpperCase();
            if (cleaned. startsWith(cc)) {
                cleaned = cleaned.substring(cc. length());
            }
        }

        // Remove common EU separators
        cleaned = cleaned.replaceAll("[-\\s_.]", "");

        // Keep only alphanumeric (and @ for email)
        cleaned = cleaned.replaceAll("[^A-Za-z0-9@]", "");

        if (cleaned.isEmpty()) {
            LOG.warn("Normalized identifier is empty!");
            return null;
        }

        LOG.debug("Normalized result: '{}'", cleaned);
        return cleaned;
    }

    // ==========================================
    // EU SCHEME SELECTION (VALIDATOR-SAFE)
    // ==========================================

    /**
     * Selects PEPPOL participant identifier scheme using validator-safe strategy.
     * @param identifier normalized identifier
     * @param countryCode ISO 3166-1 alpha-2 country code
     * @return PEPPOL scheme code or null if identifier is null/blank
     */
    private String selectEUScheme(String identifier, String countryCode) {
        if (identifier == null || identifier. isBlank()) {
            LOG.debug("No identifier - no scheme selected");
            return null;
        }

        String cleaned = identifier.trim();

        // Universal schemes (work everywhere in EU)
        if (EMAIL_PATTERN.matcher(cleaned).matches()) {
            LOG.debug("Using email scheme (EM)");
            return "EM";
        }

        if (GLN_PATTERN.matcher(cleaned).matches()) {
            LOG.debug("Using GLN scheme (0088)");
            return "0088";
        }

        if (DUNS_PATTERN.matcher(cleaned).matches()) {
            LOG.debug("Using DUNS scheme (0060)");
            return "0060";
        }

        String countryScheme = getEUCountryScheme(countryCode);
        if (countryScheme != null) {
            LOG.debug("Using EU country scheme:  {} for {}", countryScheme, countryCode);
            return countryScheme;
        }

        LOG.warn("Using DUNS (0060) as universal fallback for country: {}", countryCode);
        return "0060";
    }

    /**
     * Maps EU country codes to PEPPOL participant identifier schemes.
     *
     * @param countryCode ISO 3166-1 alpha-2 country code
     * @return PEPPOL scheme code or null for DUNS fallback
     */
    private String getEUCountryScheme(String countryCode) {
        if (countryCode == null) return null;

        switch (countryCode.trim().toUpperCase()) {
            // === Countries with VALIDATED 0xxx schemes (<= 0240) ===
            
            case "DK": return "0184"; 
            case "FI": return "0216"; 
            case "SE": return "0007";
            case "NO":  return "0192"; 
            case "IS":  return "0196"; 

            case "EE": return "0191"; 
            case "LT": return "0200"; 
            case "LV": return "0218"; 

            case "NL": return "0190"; 
            case "BE": return "0208"; 
            case "LU": return "0060"; 

            case "CH": return "0183"; 
            case "DE": return "0204";
            case "AT": return "0060"; 

            case "IT": return "0210";
            case "FR": return "0009"; 
            case "ES": return "0060"; 
            case "PT": return "0060"; 
            case "GR": 
            case "EL": return "0060"; 
            case "MT": return "0060"; 
            case "CY": return "0060"; 

            case "PL": return "0060"; 
            case "CZ": return "0060"; 
            case "SK": return "0060"; 
            case "HU": return "0060"; 
            case "SI": return "0060"; 
            case "RO": return "0060"; 
            case "BG": return "0060"; 
            case "HR": return "0060"; 

            case "IE": return "0060"; 

            case "GB": 
            case "UK": return "0060";

            case "LI": return "0060"; 

            default: 
                LOG.warn("Unknown country {} - will use DUNS fallback", countryCode);
                return "0060"; // Safe universal fallback
        }
    }

    // ==========================================
    // PARTY BUILDING
    // ==========================================

    /**
     * Builds PEPPOL AccountingSupplierParty from organization data.
     *
     * @param org supplier organization
     * @param id normalized identifier
     * @param scheme PEPPOL scheme code
     * @param includeTaxScheme whether to include PartyTaxScheme (BR-O-02)
     * @param ba billing account for address
     * @return PEPPOL AccountingSupplierParty
     */
    private AccountingSupplierParty buildSupplierParty(Organization org, String id, String scheme,
                                                       boolean includeTaxScheme, BillingAccount ba) {
        LOG.debug("Building supplier party: id='{}', scheme='{}'", id, scheme);

        EndpointID endpoint = null;
        if (id != null && ! id.isBlank() && scheme != null && !scheme. isBlank()) {
            endpoint = new EndpointID(id).withSchemeID(scheme);
            LOG.debug("Supplier EndpointID: {}:{}", scheme, id);
        } else {
            LOG.warn("Supplier EndpointID omitted");
        }

        PartyLegalEntity ple = new PartyLegalEntity(org.getTradingName());
        if (id != null && !id.isBlank()) {
            String finalScheme = (scheme != null && !scheme.isBlank()) ? scheme : "0060";
            ple.withCompanyID(new CompanyID(id).withSchemeID(finalScheme));
            LOG.debug("Supplier CompanyID: {}:{}", finalScheme, id);
        }

        PostalAddress addr = buildPostalAddress(ba, extractCountryCode(org));

        Party party = new Party(endpoint, addr, ple)
                .withPartyIdentification(new PartyIdentification(new ID(org.getId())))
                .withPartyName(new PartyName(org.getTradingName()));

        if (includeTaxScheme && id != null && !id.isBlank()) {
            String countryCode = extractCountryCode(org);
            String taxId = (countryCode != null ?  countryCode. toUpperCase() : "") + id;
            party.withPartyTaxScheme(new PartyTaxScheme(taxId, new TaxScheme("VAT")));
            LOG.debug("Supplier PartyTaxScheme: {}", taxId);
        }

        return new AccountingSupplierParty(party);
    }

    /**
     * Builds PEPPOL AccountingCustomerParty from organization data.
     *
     * @param org customer organization
     * @param id normalized identifier
     * @param scheme PEPPOL scheme code
     * @param includeTaxScheme whether to include PartyTaxScheme (BR-O-02)
     * @param ba billing account for address
     * @return PEPPOL AccountingCustomerParty
     */
    private AccountingCustomerParty buildCustomerParty(Organization org, String id, String scheme,
                                                       boolean includeTaxScheme, BillingAccount ba) {
        LOG.debug("Building customer party: id='{}', scheme='{}'", id, scheme);

        EndpointID endpoint = null;
        if (id != null && !id.isBlank() && scheme != null && !scheme.isBlank()) {
            endpoint = new EndpointID(id).withSchemeID(scheme);
            LOG.debug("Customer EndpointID: {}:{}", scheme, id);
        } else {
            LOG.warn("Customer EndpointID omitted");
        }

        PartyLegalEntity ple = new PartyLegalEntity(org.getTradingName());
        if (id != null && !id.isBlank()) {
            String finalScheme = (scheme != null && !scheme.isBlank()) ? scheme : "0060";
            ple.withCompanyID(new CompanyID(id).withSchemeID(finalScheme));
            LOG.debug("Customer CompanyID: {}:{}", finalScheme, id);
        }

        PostalAddress addr = buildPostalAddress(ba, extractCountryCode(org));

        Party party = new Party(endpoint, addr, ple)
                .withPartyIdentification(new PartyIdentification(new ID(org.getId())))
                .withPartyName(new PartyName(org.getTradingName()));

        if (includeTaxScheme && id != null && !id.isBlank()) {
            String countryCode = extractCountryCode(org);
            String taxId = (countryCode != null ? countryCode.toUpperCase() : "") + id;
            party. withPartyTaxScheme(new PartyTaxScheme(taxId, new TaxScheme("VAT")));
            LOG.debug("Customer PartyTaxScheme: {}", taxId);
        }

        return new AccountingCustomerParty(party);
    }

    /**
     * Builds PEPPOL PostalAddress from billing account contact information.
     *
     * @param ba billing account
     * @param countryCode ISO 3166-1 alpha-2 country code
     * @return PEPPOL PostalAddress with defaults for missing fields
     */
    private PostalAddress buildPostalAddress(BillingAccount ba, String countryCode) {
        String street = getPostalAddressField(ba != null ? ba.getContact() : null, "street");
        String city = getPostalAddressField(ba != null ? ba. getContact() : null, "city");
        String postcode = getPostalAddressField(ba != null ? ba.getContact() : null, "postcode");

        return new PostalAddress(new Country(countryCode != null ? countryCode : "XX"))
                .withStreetName(street != null ? street : "N/A")
                .withCityName(city != null ? city : "N/A")
                .withPostalZone(postcode != null ? postcode : "00000");
    }

    // ==========================================
    // TAX CALCULATION (Per-Category)
    // ==========================================

    /**
     * Container for tax calculation results.
     */
    private static class TaxCalculationResult {
        private final BigDecimal taxTotal;
        private final TaxTotal taxTotalObject;

        TaxCalculationResult(BigDecimal taxTotal, TaxTotal taxTotalObject) {
            this. taxTotal = taxTotal;
            this.taxTotalObject = taxTotalObject;
        }

        BigDecimal getTaxTotal() {
            return taxTotal;
        }

        TaxTotal getTaxTotalObject() {
            return taxTotalObject;
        }
    }

    /**
     * Calculates tax totals by grouping invoice lines by tax category.
     * @param cb customer bill
     * @param supplierId normalized supplier identifier
     * @param bom invoice BOM containing billing rates
     * @return tax calculation result with totals per category
     */
    private TaxCalculationResult calculateTaxTotals(CustomerBill cb, String supplierId, InvoiceBom bom) {
        List<TaxSubtotal> taxSubtotals = new ArrayList<>();
        BigDecimal taxTotal = BigDecimal.ZERO. setScale(SCALE, RoundingMode.HALF_UP);
        
        // Group lines by tax category
        Map<String, BigDecimal> taxableByCategory = new LinkedHashMap<>();
        Map<String, BigDecimal> taxAmountByCategory = new LinkedHashMap<>();
        Map<String, BigDecimal> taxRateByCategory = new LinkedHashMap<>();
        
        if (bom. getAppliedCustomerBillingRates() != null) {
            for (AppliedCustomerBillingRate acbr : bom.getAppliedCustomerBillingRates()) {
                BigDecimal lineAmount = toBD(acbr.getTaxExcludedAmount().getValue());
                BigDecimal ratePct = BigDecimal.ZERO;
                BigDecimal lineTaxAmount = BigDecimal.ZERO;
                
                if (acbr. getAppliedTax() != null && ! acbr.getAppliedTax().isEmpty()) {
                    if (acbr.getAppliedTax().get(0).getTaxRate() != null) {
                        ratePct = toBD(acbr.getAppliedTax().get(0).getTaxRate());
                        lineTaxAmount = lineAmount. multiply(ratePct).setScale(SCALE, RoundingMode.HALF_UP);
                    }
                }
                
                String cat = determineTaxCategory(ratePct. multiply(BigDecimal.valueOf(100)), supplierId);
                
                taxableByCategory.merge(cat, lineAmount, BigDecimal::add);
                taxAmountByCategory.merge(cat, lineTaxAmount, BigDecimal::add);
                taxRateByCategory.putIfAbsent(cat, ratePct. multiply(BigDecimal.valueOf(100)));
            }
        }

        String unit = cb.getTaxExcludedAmount() != null ? cb.getTaxExcludedAmount().getUnit() : "EUR";
        
        // Create TaxSubtotal for each category
        for (Map. Entry<String, BigDecimal> entry : taxableByCategory.entrySet()) {
            String cat = entry.getKey();
            BigDecimal taxableAmount = entry.getValue();
            BigDecimal taxAmt = taxAmountByCategory.getOrDefault(cat, BigDecimal.ZERO);
            BigDecimal pct = taxRateByCategory. getOrDefault(cat, BigDecimal.ZERO);
            
            TaxableAmount ta = new TaxableAmount(fmtMoney(taxableAmount), unit);
            TaxAmount tA = new TaxAmount(fmtMoney(taxAmt), unit);
            
            TaxCategory taxCategory = new TaxCategory(cat, new TaxScheme("VAT"));
            if (!"O".equals(cat)) {
                taxCategory.withPercent(fmtMoney(pct));
            } else {
                addTaxExemptionReasonIfSupported(taxCategory, EXEMPTION_TEXT);
            }

            TaxSubtotal ts = new TaxSubtotal(ta, tA, taxCategory);
            taxSubtotals.add(ts);
            taxTotal = taxTotal.add(taxAmt);
            
            LOG.debug("TaxSubtotal: category={}, taxable={}, tax={}, rate={}%", 
                     cat, fmtMoney(taxableAmount), fmtMoney(taxAmt), fmtMoney(pct));
        }

        TaxTotal tt = new TaxTotal(new TaxAmount(fmtMoney(taxTotal), unit));
        for (TaxSubtotal s : taxSubtotals) {
            tt.withTaxSubtotal(s);
        }

        LOG.info("Tax calculation complete: {} categories, total tax: {}", taxSubtotals.size(), fmtMoney(taxTotal));
        return new TaxCalculationResult(taxTotal, tt);
    }

    /**
     * Determines VAT category code based on tax rate and supplier identifier.
     *
     * @param pct tax rate percentage
     * @param supplierId normalized supplier identifier
     * @return VAT category code:  "S" (standard), "Z" (zero-rated), or "O" (exempt)
     */
    private String determineTaxCategory(BigDecimal pct, String supplierId) {
        if (pct. compareTo(BigDecimal. ZERO) > 0) {
            return "S"; // Standard rated
        } else if (supplierId != null && ! supplierId.isBlank()) {
            return "Z"; // Zero rated
        } else {
            return "O"; // Not subject to VAT
        }
    }

    // ==========================================
    // INVOICE LINES
    // ==========================================

    /**
     * Creates PEPPOL invoice lines from billing rates.
     * Handles both normal lines and discount lines (negative amounts).
     *
     * @param bom invoice BOM containing billing rates
     * @param supplierId normalized supplier identifier
     * @return list of PEPPOL invoice lines
     */
    private List<InvoiceLine> createInvoiceLines(InvoiceBom bom, String supplierId) {
        List<InvoiceLine> lines = new ArrayList<>();
        int lineCounter = 1;

        if (bom.getAppliedCustomerBillingRates() != null) {
            for (AppliedCustomerBillingRate acbr : bom. getAppliedCustomerBillingRates()) {
                BigDecimal amount = toBD(acbr. getTaxExcludedAmount().getValue());

                if (amount.compareTo(BigDecimal.ZERO) < 0) {
                    lines.add(createDiscountLine(acbr, bom, supplierId, lineCounter++));
                } else {
                    lines.add(createNormalLine(acbr, bom, supplierId, lineCounter++));
                }
            }
        }

        if (lines.isEmpty()) {
            LOG.warn("No invoice lines created");
        } else {
            LOG.debug("Created {} invoice lines", lines. size());
        }

        return lines;
    }

    /**
     * Creates a normal (positive amount) invoice line.
     *
     * @param acbr applied customer billing rate
     * @param bom invoice BOM
     * @param supplierId normalized supplier identifier
     * @param lineNum line number
     * @return PEPPOL invoice line
     */
    private InvoiceLine createNormalLine(AppliedCustomerBillingRate acbr, InvoiceBom bom, String supplierId, int lineNum) {
        Product product = bom.getProduct(acbr.getProduct().getId());
        BigDecimal ratePct = BigDecimal.ZERO;
        String taxSchemeId = "VAT";

        if (acbr.getAppliedTax() != null && !acbr.getAppliedTax().isEmpty()) {
            if (acbr.getAppliedTax().get(0).getTaxRate() != null) {
                ratePct = toBD(acbr.getAppliedTax().get(0).getTaxRate()).multiply(BigDecimal.valueOf(100));
            }
            if (acbr.getAppliedTax().get(0).getTaxCategory() != null) {
                taxSchemeId = acbr. getAppliedTax().get(0).getTaxCategory();
            }
        }

        String classified = determineTaxCategory(ratePct, supplierId);
        ClassifiedTaxCategory tc = new ClassifiedTaxCategory(classified, new TaxScheme(taxSchemeId));

        if (!"O".equals(classified)) {
            tc.withPercent(fmtMoney(ratePct));
        } else {
            addTaxExemptionReasonIfSupported(tc, EXEMPTION_TEXT);
        }

        String productName = product != null ? product.getName() : "Product";
        Item item = new Item(productName, tc)
                .withSellersItemIdentification(new SellersItemIdentification(acbr.getId()));

        BigDecimal amount = toBD(acbr. getTaxExcludedAmount().getValue());
        String unit = acbr.getTaxExcludedAmount().getUnit() != null ? acbr.getTaxExcludedAmount().getUnit() : "EUR";

        Price price = new Price(new PriceAmount(fmtMoney(amount), unit));

        return new InvoiceLine(
                String.valueOf(lineNum),
                new InvoicedQuantity("1", "EA"),
                new LineExtensionAmount(fmtMoney(amount), unit),
                item,
                price
        );
    }

    /**
     * Creates a discount (negative amount) invoice line with negative quantity.
     *
     * @param acbr applied customer billing rate
     * @param bom invoice BOM
     * @param supplierId normalized supplier identifier
     * @param lineNum line number
     * @return PEPPOL invoice line with negative quantity
     */
    private InvoiceLine createDiscountLine(AppliedCustomerBillingRate acbr, InvoiceBom bom, String supplierId, int lineNum) {
        Product product = bom. getProduct(acbr.getProduct().getId());
        BigDecimal ratePct = BigDecimal.ZERO;
        String taxSchemeId = "VAT";

        if (acbr. getAppliedTax() != null && !acbr.getAppliedTax().isEmpty()) {
            if (acbr.getAppliedTax().get(0).getTaxRate() != null) {
                ratePct = toBD(acbr.getAppliedTax().get(0).getTaxRate()).multiply(BigDecimal.valueOf(100));
            }
            if (acbr.getAppliedTax().get(0).getTaxCategory() != null) {
                taxSchemeId = acbr.getAppliedTax().get(0).getTaxCategory();
            }
        }

        String classified = determineTaxCategory(ratePct, supplierId);
        ClassifiedTaxCategory tc = new ClassifiedTaxCategory(classified, new TaxScheme(taxSchemeId));

        if (!"O".equals(classified)) {
            tc.withPercent(fmtMoney(ratePct));
        } else {
            addTaxExemptionReasonIfSupported(tc, EXEMPTION_TEXT);
        }

        BigDecimal discountAmount = toBD(acbr.getTaxExcludedAmount().getValue()).abs();
        String productName = product != null ? product. getName() : "Product";
        String discountDescription = "Discount:  " + productName;
        String unit = acbr.getTaxExcludedAmount().getUnit() != null ? acbr.getTaxExcludedAmount().getUnit() : "EUR";

        Item item = new Item(discountDescription, tc)
                .withSellersItemIdentification(new SellersItemIdentification(acbr.getId()));

        Price price = new Price(new PriceAmount(fmtMoney(discountAmount), unit));

        return new InvoiceLine(
                String.valueOf(lineNum),
                new InvoicedQuantity("-1", "EA"),
                new LineExtensionAmount(fmtMoney(discountAmount. negate()), unit),
                item,
                price
        );
    }

    // ==========================================
    // LEGAL MONETARY TOTAL
    // ==========================================

    /**
     * Creates PEPPOL LegalMonetaryTotal by summing actual invoice lines.
     *
     * @param cb customer bill
     * @param taxTotalAmount calculated tax total
     * @param bom invoice BOM containing billing rates
     * @return PEPPOL LegalMonetaryTotal
     */
    private LegalMonetaryTotal createLegalMonetaryTotal(CustomerBill cb, BigDecimal taxTotalAmount, InvoiceBom bom) {
        // Calculate LineExtensionAmount from actual lines
        BigDecimal lineExtensionSum = BigDecimal.ZERO. setScale(SCALE, RoundingMode.HALF_UP);
        
        if (bom.getAppliedCustomerBillingRates() != null) {
            for (AppliedCustomerBillingRate acbr : bom. getAppliedCustomerBillingRates()) {
                BigDecimal amount = toBD(acbr.getTaxExcludedAmount().getValue());
                lineExtensionSum = lineExtensionSum.add(amount);
            }
        }
        
        BigDecimal taxExclusive = lineExtensionSum;
        BigDecimal taxInclusive = taxExclusive.add(taxTotalAmount).setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal payableAmount = taxInclusive;
        
        String unit = cb.getTaxExcludedAmount() != null ? cb.getTaxExcludedAmount().getUnit() : "EUR";

        LOG.info("LegalMonetaryTotal:  LineExt={}, TaxExcl={}, TaxIncl={}, Payable={}", 
                 fmtMoney(lineExtensionSum), fmtMoney(taxExclusive), 
                 fmtMoney(taxInclusive), fmtMoney(payableAmount));

        return new LegalMonetaryTotal(
                new LineExtensionAmount(fmtMoney(lineExtensionSum), unit),
                new TaxExclusiveAmount(fmtMoney(taxExclusive), unit),
                new TaxInclusiveAmount(fmtMoney(taxInclusive), unit),
                new PayableAmount(fmtMoney(payableAmount), unit)
        );
    }

    // ==========================================
    // INVOICE NOTES
    // ==========================================

    /**
     * Adds descriptive notes to the invoice.
     *
     * @param invoice PEPPOL invoice
     * @param supplierCountry supplier country code
     * @param bom invoice BOM
     * @param supplierId normalized supplier identifier
     */
    private void addInvoiceNotes(Invoice invoice, String supplierCountry, InvoiceBom bom, String supplierId) {
        List<String> notes = new ArrayList<>();

        if (supplierCountry != null && !supplierCountry.isBlank()) {
            notes.add("Invoice issued in " + supplierCountry);
        }

        notes.add(determineTaxRegime(bom, supplierId));
        notes.add("Electronic invoice compatible with PEPPOL BIS 3.0");

        invoice.withNote(String.join("; ", notes));
    }

    /**
     * Determines the overall tax regime of the invoice.
     *
     * @param bom invoice BOM
     * @param supplierId normalized supplier identifier
     * @return human-readable tax regime description
     */
    private String determineTaxRegime(InvoiceBom bom, String supplierId) {
        String invoiceCat = "O";

        if (bom.getAppliedCustomerBillingRates() != null) {
            for (AppliedCustomerBillingRate acbr : bom.getAppliedCustomerBillingRates()) {
                BigDecimal ratePct = BigDecimal.ZERO;
                if (acbr.getAppliedTax() != null && !acbr.getAppliedTax().isEmpty() &&
                        acbr.getAppliedTax().get(0).getTaxRate() != null) {
                    ratePct = toBD(acbr.getAppliedTax().get(0).getTaxRate()).multiply(BigDecimal.valueOf(100));
                }

                String lineCat = determineTaxCategory(ratePct, supplierId);
                if ("S".equals(lineCat)) {
                    invoiceCat = "S";
                    break;
                }
                if ("Z".equals(lineCat) && !"S".equals(invoiceCat)) {
                    invoiceCat = "Z";
                }
            }
        }

        switch (invoiceCat) {
            case "S":
                return "VAT applies - normal regime";
            case "Z": 
                return "VAT not charged - zero-rated transaction";
            case "O": 
                return "VAT not applicable - exempted";
            default:
                return "VAT status unknown";
        }
    }

    // ==========================================
    // UTILITY METHODS
    // ==========================================

    /**
     * Checks if invoice contains zero-rated lines without a supplier identifier.
     * Used to determine if PartyTaxScheme should be omitted (BR-O-02).
     *
     * @param bom invoice BOM
     * @param supplierId normalized supplier identifier
     * @return true if exemption applies
     */
    private boolean containsZeroRatedLineWithoutSupplierId(InvoiceBom bom, String supplierId) {
        if (bom == null || bom.getAppliedCustomerBillingRates() == null) return false;
        if (supplierId != null && !supplierId.isBlank()) return false;

        for (AppliedCustomerBillingRate acbr : bom.getAppliedCustomerBillingRates()) {
            if (acbr.getAppliedTax() == null || acbr.getAppliedTax().isEmpty()) return true;
            Number r = acbr.getAppliedTax().get(0).getTaxRate();
            if (r == null || BigDecimal.valueOf(r. doubleValue()).compareTo(BigDecimal.ZERO) == 0) return true;
        }
        return false;
    }

    /**
     * Adds tax exemption reason to tax category if supported by the object.
     * Uses reflection to handle different PEPPOL library versions.
     *
     * @param taxCategoryObj tax category object
     * @param reason exemption reason text
     */
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

    /**
     * Extracts ISO 3166-1 alpha-2 country code from organization characteristics.
     *
     * @param org organization entity
     * @return country code or null if not found
     */
    private String extractCountryCode(Organization org) {
        if (org == null || org.getPartyCharacteristic() == null) return null;
        for (Characteristic c : org.getPartyCharacteristic()) {
            if (c == null || c.getName() == null || c.getValue() == null) continue;
            if ("country".equalsIgnoreCase(c.getName())) {
                String country = c.getValue().toString().trim().toUpperCase();
                if (country.length() == 2) {
                    return country;
                }
            }
        }
        return null;
    }

    /**
     * Extracts postal address field from contact list.
     *
     * @param contacts list of contacts
     * @param fieldName field name ("street", "city", "postcode")
     * @return field value or null if not found
     */
    private String getPostalAddressField(List<Contact> contacts, String fieldName) {
        if (contacts == null || contacts.isEmpty()) {
            return null;
        }
        return contacts.stream()
                .flatMap(c -> c.getContactMedium().stream())
                .filter(cm -> "PostalAddress".equalsIgnoreCase(cm.getMediumType()))
                .map(cm -> {
                    switch (fieldName. toLowerCase()) {
                        case "street":
                            return cm. getCharacteristic().getStreet1();
                        case "city": 
                            return cm.getCharacteristic().getCity();
                        case "postcode":
                            return cm.getCharacteristic().getPostCode();
                        default:
                            return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Converts Number to BigDecimal with proper scale. 
     *
     * @param n number to convert
     * @return BigDecimal with scale 2, or zero if input is null
     */
    private static BigDecimal toBD(Number n) {
        if (n == null) return BigDecimal.ZERO. setScale(SCALE, RoundingMode.HALF_UP);
        return new BigDecimal(n.toString()).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Formats BigDecimal as monetary string with scale 2.
     *
     * @param bd BigDecimal to format
     * @return formatted string (e.g., "1234.56")
     */
    private static String fmtMoney(BigDecimal bd) {
        return bd.setScale(SCALE, RoundingMode. HALF_UP).toPlainString();
    }
}