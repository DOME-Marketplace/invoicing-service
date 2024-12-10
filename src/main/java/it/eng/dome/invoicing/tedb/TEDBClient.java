package it.eng.dome.invoicing.tedb;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.util.Calendar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import it.eng.dome.invoicing.tedb.model.Configurations;
import it.eng.dome.invoicing.tedb.model.Country;
import it.eng.dome.invoicing.tedb.model.SearchResult;
import it.eng.dome.invoicing.tedb.model.TaxRate;
import it.eng.dome.invoicing.tedb.model.TaxVersion;

public class TEDBClient implements TEDB {

    private static final String DEFAULT_URL = "https://ec.europa.eu/taxation_customs/tedb/rest-api";

    private String url;

    public TEDBClient() {
        this.url = DEFAULT_URL;
    }

    public TEDBClient(String url) {
        this.url = url;
    }

    public Configurations getConfigurations() throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.url+"/configurations"))
                .GET()
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Gson gson = new GsonBuilder().create();
        Configurations configs = gson.fromJson(response.body(), Configurations.class);
//        verifyAsConfiguratinos(response.body());

        return configs;
    }


    /**
     * A second call, needed to get the TEDB taxId of a given taxType in a give
     * country
     * 
     * @param tedbCountryId
     * @param taxType
     * @return
     * @throws Exception
     */
    public SearchResult searchTaxes(String tedbCountryId, String taxType, Calendar date) throws InterruptedException, IOException, IllegalArgumentException{

        HttpClient client = HttpClient.newHttpClient();

        String situationOn = String.format("%s/%s/%s", date.get(Calendar.YEAR), date.get(Calendar.MONTH)+1, date.get(Calendar.DAY_OF_MONTH));
        System.out.println(situationOn);

        String body = "{\"searchForm\":{\"selectedTaxTypes\":[\"" + taxType + "\"],\"selectedMemberStates\":["
                + tedbCountryId + "],\"situationOn\":\""+situationOn+"\",\"historized\":\"" + false
                + "\",\"keywords\":\"\"},\"availableFacets\":null,\"selectedFacets\":null,\"sort\":null}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(this.url+"/simpleSearch"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//        verifyAsSearchResult(response.body());

        try {
                Gson gson = new GsonBuilder().create();
                SearchResult searchResult = gson.fromJson(response.body(), SearchResult.class);
                return searchResult;
        } catch (JsonSyntaxException e) {
                String msg = String.format("Bad request. Unable to search taxes for country '%s', tax type '%s' and date '%s'", tedbCountryId, taxType, date);
                throw new IllegalArgumentException(msg);
        }

}

    /**
     * A third call, to get rates for a given tax (the country is already encoded
     * within the taxid)
     * 
     * @param tv
     * @return
     */
    public TaxRate getTaxRate(String taxId, String versionDate) throws IOException, InterruptedException, IllegalArgumentException {

        HttpClient client = HttpClient.newHttpClient();

        String url = String.format(this.url + "/tax/rate?taxId=%s&versionDate=%s&isEuro=true", taxId, versionDate);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//        verifyAsTaxRate(response.body());

        if(response.statusCode()==400) {
                String msg = String.format("Bad request. Unable to retrive tax rate for tax '%s', version '%s'", taxId, versionDate);
                throw new IllegalArgumentException(msg);
        }

        try {
                Gson gson = new GsonBuilder().create();
                TaxRate taxRate = gson.fromJson(response.body(), TaxRate.class);
                if(taxRate!=null && taxRate.getVatRateStructure()==null) {
                        String msg = String.format("Bad request. Unable to retrive tax rate for tax '%s', version '%s'", taxId, versionDate);
                        throw new IllegalArgumentException(msg);
                }
                return taxRate;
        } catch (JsonSyntaxException e) {
                String msg = String.format("Bad request. Unable to retrive tax rate for tax '%s', version '%s'", taxId, versionDate);
                throw new IllegalArgumentException(msg);
        }

    }

    public String getCountryIdFor(String countryCode) throws IOException, InterruptedException, IllegalArgumentException {
        for (Country country : this.getConfigurations().getCountries()) {
                String code = country.getDefaultCountryCOde();
                if (code.toLowerCase().equals(countryCode.toLowerCase())) {
                        return country.getId();
                }
        }
        throw new IllegalArgumentException("Unable to map country code '" + countryCode + "'' to a TEDB id. TEDB probably doesn't know about " + countryCode);
    }


    public Number getVATRateInCountryAtDate(String countryCode, Calendar date) throws IOException, InterruptedException, IllegalArgumentException, ParseException {
        String tedbCountryId = this.getCountryIdFor(countryCode);
        SearchResult sr = this.searchTaxes(tedbCountryId, "VAT", date);
        if(sr.getTaxVersions().size()>1) {
                String msg = String.format("WARNING: found %d different VAT taxes for %s at date %s", sr.getTaxVersions().size(), countryCode, date);
                System.out.println(msg);
        }
        if(sr.getTaxVersions().size()==0) {
                String msg = String.format("ERROR: unable to find VAT taxes for %s at date %s", sr.getTaxVersions().size(), countryCode, date);
                System.out.println(msg);
        }
        TaxVersion tv = sr.getTaxVersions().get(0);
        return this.getTaxRate(tv.taxId, tv.versionDate).getVatRateStructure().getStandardRate().getRate().getValue();
    }


}