package it.eng.dome.invoicing.tedb.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

public class SearchResult implements Serializable {

	private static final long serialVersionUID = 5288412543565262347L;
	
	//public String errors;
	public JsonObject errors;
    public InitialSearch initialSearch;
    public List<TaxVersion> result;

    public SearchResult() {
        this.result = new ArrayList<>();
    }

    public List<TaxVersion> getTaxVersions() {
        return this.result;
    }

}
