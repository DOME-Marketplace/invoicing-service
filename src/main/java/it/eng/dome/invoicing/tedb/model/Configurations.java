package it.eng.dome.invoicing.tedb.model;

import java.io.Serializable;
import java.util.List;

public class Configurations implements Serializable {

	private static final long serialVersionUID = -3649978020730136553L;

	public String contactMailAddress;
    public String trackerUrl;
    public List<Country> nonCompliantCountries;
    public List<Country> countries;
    public String updateDate;
    public String europeanCommissionUrl;
    public String contactMailSubject;
    public String disclaimerURL;
    public String siteId;
    public String privacyPolicyUrl;
    public String legacyUrl;
    public String tedbEuropaWebtoolsURL;
    public String version;
    public TaxTypes taxTypes;
    public TaxParentType taxParentType;
    public String taxationAndCustomsUrl;
    public Boolean asyncFacetsCount;
    public Integer nbDaysForPublicNotificationExpirationDate;

    public List<Country> getCountries() {
        return this.countries;
    }

}
