package it.eng.dome.invoicing.tedb.model;

import java.util.List;
import java.util.Vector;

public class SearchResult {

    public String errors;
    public InitialSearch initialSearch;
    public List<TaxVersion> result;

    public SearchResult() {
        this.result = new Vector<>();
    }

    public List<TaxVersion> getTaxVersions() {
        return this.result;
    }

}
