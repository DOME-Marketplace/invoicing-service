package it.eng.dome.invoicing.engine.model;

import java.util.Objects;

public class TaxItemKey {

	private final Float taxRate;
    private final String taxCategory;

    public TaxItemKey(Float taxRate, String taxCategory) {
        this.taxRate = taxRate;
        this.taxCategory = taxCategory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TaxItemKey)) return false;
        TaxItemKey taxKey = (TaxItemKey) o;
        return Float.compare(taxKey.taxRate, taxRate) == 0 &&
               Objects.equals(taxCategory, taxKey.taxCategory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taxRate, taxCategory);
    }

	public Float getTaxRate() {
		return taxRate;
	}

	public String getTaxCategory() {
		return taxCategory;
	}
}
