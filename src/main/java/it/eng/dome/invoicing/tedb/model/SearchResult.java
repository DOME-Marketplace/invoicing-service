package it.eng.dome.invoicing.tedb.model;

import java.util.ArrayList;
import java.util.List;

public class SearchResult {

    public String errors;
    public InitialSearch initialSearch;
    public List<TaxVersion> result;

    public SearchResult() {
        this.result = new ArrayList<>();
    }

    public List<TaxVersion> getTaxVersions() {
        return this.result;
    }

}
