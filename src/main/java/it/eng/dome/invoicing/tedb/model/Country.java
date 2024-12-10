package it.eng.dome.invoicing.tedb.model;

public class Country {

    public Integer id;
    public String name;
    public String defaultCountryCode;
    public String alternativeCountryCode;
    public String email;
    public Currency defaultCurrency;
    public Boolean cnCodesCompliant;
    public Boolean cpaCodesCompliant;

    public String getDefaultCountryCOde() {
        return this.defaultCountryCode;
    }

    public String getId() {
        return this.id.toString();
    }

}
