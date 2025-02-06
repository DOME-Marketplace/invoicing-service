package it.eng.dome.invoicing.tedb.model;

import java.io.Serializable;
import java.util.List;

public class TaxRate implements Serializable {

	private static final long serialVersionUID = 335055630574827362L;
	
	public List<FootNote> footnotes;
    public VatRateStructure vatRateStructure;

    public Object errors;
    public Object rateStructureTax;
    public Object annualIncome;
    public Object alcoholicBeverages;
    public Object rateEnergyStructure;
    public Object manufacturedTobacco;

    public VatRateStructure getVatRateStructure() {
        return this.vatRateStructure;
    }

}