package it.eng.dome.invoicing.tedb;

import java.io.IOException;
import java.util.Calendar;

import it.eng.dome.invoicing.tedb.model.Configurations;
import it.eng.dome.invoicing.tedb.model.SearchResult;
import it.eng.dome.invoicing.tedb.model.TaxRate;

public interface TEDB {

    public Configurations getConfigurations() throws IOException, InterruptedException;

    public SearchResult searchTaxes(String tedbCountryId, String taxType, Calendar date) throws Exception;

    public TaxRate getTaxRate(String taxId, String versionDate) throws IOException, InterruptedException;

}

