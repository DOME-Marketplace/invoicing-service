package it.eng.dome.invoicing.engine.rate;

import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import it.eng.dome.invoicing.engine.tmf.TmfApiFactory;
import it.eng.dome.invoicing.tedb.TEDBCachedClient;
import it.eng.dome.invoicing.tedb.TEDBClient;
import it.eng.dome.invoicing.util.countryguesser.CountryGuesser;
import it.eng.dome.invoicing.util.countryguesser.GuessResult;
import it.eng.dome.tmforum.tmf622.v4.model.RelatedParty;
import it.eng.dome.tmforum.tmf632.v4.api.OrganizationApi;
import it.eng.dome.tmforum.tmf632.v4.model.Characteristic;
import it.eng.dome.tmforum.tmf632.v4.model.Organization;

@Component(value = "RateManager")
//@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RateManager implements InitializingBean {

    // A logger for this class
    private final Logger logger = LoggerFactory.getLogger(RateManager.class);

    // static so that we can reuse the cache across RateManager instances
    private static final TEDBClient tedbclient = new TEDBCachedClient();

    @Autowired
    // Factory for TMF APIss
    private TmfApiFactory tmfApiFactory;
    // TMForum API to retrieve organisations
    private OrganizationApi orgApi;

    // A CountryGuesser to be used when a country is not available in the organisation
    private CountryGuesser countryGuesser;

    @Override
    public void afterPropertiesSet() throws Exception {
        final it.eng.dome.tmforum.tmf632.v4.ApiClient orgApiClient = tmfApiFactory.getTMF632PartyManagementApiClient();
        orgApi = new OrganizationApi(orgApiClient);
    }

    public RateManager() {
        try {
            logger.info("Instantiating a CountryGuesser");
            this.countryGuesser = new CountryGuesser();
        } catch(Exception e) {
            logger.warn("Unable to instantiate a CountryGuesser. Relying on the 'country' characteristic, if any.");
        }
    }

    public Number getVATRateFor(RelatedParty buyer, RelatedParty seller, Calendar date) throws Exception {
        String sellerCountryCode = this.getCountryCodeFor(seller);
        String buyerCountryCode = this.getCountryCodeFor(buyer);
        return this.getVATRateFor(sellerCountryCode, buyerCountryCode, date);
    }

    private String getCountryCodeFor(RelatedParty party) throws Exception {
        Organization org = orgApi.retrieveOrganization(party.getId(), null);
        String countryCode = this.getCountryFromCharacteristic(org);
        if(countryCode==null) {
            logger.warn("unable to find a 'country' characteristic for organization " + org.getId());
            if(this.countryGuesser!=null) {
                List<GuessResult> guessResult = this.countryGuesser.guessCountry(org);
                if(!guessResult.isEmpty()) {
                    countryCode = guessResult.get(0).getCountryCode();
                    logger.warn("using GUESSED countryCode '"+countryCode+"' for organization " + org.getId());
                }
            }
        }
        return countryCode;
    }

    @SuppressWarnings("null")
    private String getCountryFromCharacteristic(Organization org) {
        if(org.getPartyCharacteristic()!=null)
            for(Characteristic c:org.getPartyCharacteristic()) {
                if("country".equalsIgnoreCase(c.getName()))
                    return c.getValue().toString();
            }
        return null;
    }

    private Number getVATRateFor(String sellerCountry, String buyerCountry, Calendar date)
        throws IOException, InterruptedException, IllegalArgumentException, ParseException {

        // check parameters
        if (sellerCountry == null || sellerCountry.trim() == "") {
            throw new IllegalArgumentException("Wrong or empty sellerCountry");
        }
        if (buyerCountry == null || buyerCountry.trim() == "") {
            throw new IllegalArgumentException("Wrong or empty buyerCountry");
        }
        if (date == null) {
            throw new IllegalArgumentException("Please provide a date");
        }
        // different countries => VAT not applicable
        if (!sellerCountry.toLowerCase().trim().equals(buyerCountry.toLowerCase().trim())) {
            String msg = String.format("VAT is not applicable in transactions between %s (seller) and %s (buyer). Applying 0%%.", sellerCountry, buyerCountry);
            logger.info(msg);
            return 0f;
        }
        // same countries => ask the VAT service for the VAT for the SELLER
        Number rate = tedbclient.getVATRateInCountryAtDate(sellerCountry, date);
        String msg = String.format("Applicable VAT in transactions between %s (seller) and %s (buyer) is %s%%", sellerCountry, buyerCountry, (rate.doubleValue()*100));
        logger.info(msg);
        return rate;
    }


}
