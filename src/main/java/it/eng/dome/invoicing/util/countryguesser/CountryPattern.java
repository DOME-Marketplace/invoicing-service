package it.eng.dome.invoicing.util.countryguesser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.eng.dome.tmforum.tmf632.v4.model.Characteristic;
import it.eng.dome.tmforum.tmf632.v4.model.ContactMedium;
import it.eng.dome.tmforum.tmf632.v4.model.ExternalReference;
import it.eng.dome.tmforum.tmf632.v4.model.MediumCharacteristic;
import it.eng.dome.tmforum.tmf632.v4.model.Organization;

class CountryPattern {

    private static final Logger log = LoggerFactory.getLogger(CountryPattern.class);

    private String countryCode;
    private List<Pattern> countryNamePatterns;
    private List<Pattern> vatPatterns;
    private Pattern phonePattern;
    private Pattern domainPattern;
    private List<Pattern> legalEntityPatterns;

    public CountryPattern() {
        this.countryNamePatterns = new ArrayList<Pattern>();
        this.vatPatterns = new ArrayList<Pattern>();
        this.legalEntityPatterns = new ArrayList<Pattern>();
    }

    public CountryPattern(String jsonString) {
        this();
        this.setRulesFromJSONString(jsonString);
    }

    public CountryPattern(JSONObject json) {
        this();
        this.setRulesFromJSON(json);
    }

    public void setRulesFromFile(String fileName) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        String ls = System.getProperty("line.separator");
            while ((line = reader.readLine()) != null) {
	        stringBuilder.append(line);
	        stringBuilder.append(ls);
        }
        reader.close();
        String content = stringBuilder.toString();
        this.setRulesFromJSONString(content);
    }

    public void setRulesFromJSONString(String jsonString) {
        JSONObject obj = new JSONObject(jsonString);
        this.setRulesFromJSON(obj);
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public void setRulesFromJSON(JSONObject obj) {

        // the country code
        this.countryCode = obj.getString("country_code");

        // country names
        JSONArray name_res = obj.getJSONArray("country_names");
        for (int i = 0; i < name_res.length(); i++) {
            String name_re = name_res.getString(i);
            this.countryNamePatterns.add(Pattern.compile(name_re, Pattern.CASE_INSENSITIVE));
        }

        // VAT regular expressions
        JSONArray vat_res = obj.getJSONArray("vat_res");
        for (int i = 0; i < vat_res.length(); i++) {
            String vat_re = vat_res.getString(i);
            this.vatPatterns.add(Pattern.compile(vat_re, Pattern.CASE_INSENSITIVE));
        }

        // Phone RE
        String phone_re = obj.getString("phone_re");
        this.phonePattern = Pattern.compile(phone_re, Pattern.CASE_INSENSITIVE);

        // Domain RE
        try {
            String domain_re = obj.getString("domain_re");
            this.domainPattern = Pattern.compile(domain_re, Pattern.CASE_INSENSITIVE);
        } catch(Exception e) {
            e.printStackTrace();
        }

        // Phone regular expressions
        JSONArray legal_entity_res = obj.getJSONArray("legal_entity_res");
        for (int i = 0; i < legal_entity_res.length(); i++) {
            String legal_entity_re = legal_entity_res.getString(i);
            this.legalEntityPatterns.add(Pattern.compile(legal_entity_re, Pattern.CASE_INSENSITIVE));
        }
        
    }


    public GuessResult matches(Organization org) {

        GuessResult out = new GuessResult(this.countryCode, 0f);

        // properties to be matched against the domain patterns
        List<String> domainTexts = new ArrayList<>();
        // properties to be matched against the phone patterns
        List<String> phoneTexts = new ArrayList<>();
        // properties to be matched against the country patterns
        List<String> countryTexts = new ArrayList<>();
        // properties to be matched against the vat patterns
        List<String> vatTexts = new ArrayList<>();
        // properties to be matched against the legal entity
        List<String> leTexts = new ArrayList<>();

        // populate the texts
        leTexts.add(org.getTradingName());

        if(org.getContactMedium()!=null) {
            for(ContactMedium cm: org.getContactMedium()) {
                MediumCharacteristic c = cm.getCharacteristic();
                if(c.getEmailAddress()!=null)
                    domainTexts.add(c.getEmailAddress());
                if(c.getPhoneNumber()!=null)
                    phoneTexts.add(c.getPhoneNumber());
                if(c.getFaxNumber()!=null)
                    phoneTexts.add(c.getFaxNumber());
                if(c.getCountry()!=null)
                    countryTexts.add(c.getCountry());
            }
        }

        if(org.getPartyCharacteristic()!=null) {
            for(Characteristic c:org.getPartyCharacteristic()) {
                if("website".equals(c.getName()))
                    domainTexts.add(c.getValue().toString());
            }
        }

        if(org.getExternalReference()!=null) {
            for(ExternalReference er:org.getExternalReference()) {
                if("idm_id".equals(er.getExternalReferenceType())) {
                    vatTexts.add(er.getName());
                }
            }
        }

        log.debug("Texts to be matched against contry patterns: " + countryTexts);
        log.debug("Texts to be matched against domain patterns: " + domainTexts);
        log.debug("Texts to be matched against LE patterns:     " + leTexts);
        log.debug("Texts to be matched against VAT patterns  :  " + vatTexts);
        log.debug("Texts to be matched against phone patterns:  " + phoneTexts);

        for(Pattern p: this.legalEntityPatterns) {
            for(String text: leTexts) {
                Matcher m = p.matcher(text.trim());
                if(m.matches()) {
                    out.addMatchedPattern("legal entity");
                    out.incScore();
                }
            }
        }

        for(Pattern p: this.vatPatterns) {
            for(String text: vatTexts) {
                Matcher m = p.matcher(text.trim());
                if(m.matches()) {
                    out.addMatchedPattern("vat");
                    out.incScore();
                }
            }
        }

        for(Pattern p: this.countryNamePatterns) {
            for(String text: countryTexts) {
                Matcher m = p.matcher(text.trim());
                if(m.matches()) {
                    out.addMatchedPattern("country");
                    out.incScore();
                }
            }
        }

        for(String text: phoneTexts) {
            Matcher m = this.phonePattern.matcher(text.trim());
            if(m.matches()) {
                out.addMatchedPattern("phone");
                out.incScore();
            }
        }

        for(String text: domainTexts) {
            Matcher m = this.domainPattern.matcher(text.trim());
            if(m.matches()) {
                out.addMatchedPattern("domain");
                out.incScore();
            }
        }
        
        return out;
    }

}