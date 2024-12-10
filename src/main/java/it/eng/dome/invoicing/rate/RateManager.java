package it.eng.dome.invoicing.rate;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;

import it.eng.dome.invoicing.tedb.TEDBCachedClient;
import it.eng.dome.invoicing.tedb.TEDBClient;

public class RateManager {

    private static final RateManager instance = new RateManager();

    private TEDBClient tedbclient;

    private RateManager() {
        System.out.println("instantiating the tedbclient");
        this.tedbclient = new TEDBCachedClient();
    }

    public static RateManager getInstance() {
        return RateManager.instance;
    }

    public Number getVATRateFor(String sellerCountry, String buyerCountry, Calendar date) throws IOException, InterruptedException, IllegalArgumentException, ParseException{
        // check parameters
        if(sellerCountry==null || sellerCountry.trim()=="") {
            throw new IllegalArgumentException("Wrong or empty sellerCountry");
        }
        if(buyerCountry==null || buyerCountry.trim()=="") {
            throw new IllegalArgumentException("Wrong or empty buyerCountry");
        }
        if(date==null) {
            throw new IllegalArgumentException("Please provide a date");
        }
        // different countries => VAT not applicable
        if(!sellerCountry.toLowerCase().trim().equals(buyerCountry.toLowerCase().trim())) {
            return 0f;
        }
        // same countries => ask the VAT service for the VAT for the SELLER
        return this.tedbclient.getVATRateInCountryAtDate(sellerCountry, date);
    }

    public static void main(String[] args) throws Exception {
        Number rate = new RateManager().getVATRateFor("ATS", "ATS", Calendar.getInstance());
        System.out.println(rate);
    }

}
