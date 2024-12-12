package it.eng.dome.invoicing.tedb.model;

import java.util.List;

public class VatRateStructure {

    public StandardRate standardRate;
    public ReducedRate reducedRate;
    public List<SpecificReducedRatesSection> specificReducedRatesSections;

    public StandardRate getStandardRate() {
        return this.standardRate;
    }
}
