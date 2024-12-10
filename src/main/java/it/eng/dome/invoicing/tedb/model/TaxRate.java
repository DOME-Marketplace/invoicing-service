package it.eng.dome.invoicing.tedb.model;

import java.util.List;

public class TaxRate {

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