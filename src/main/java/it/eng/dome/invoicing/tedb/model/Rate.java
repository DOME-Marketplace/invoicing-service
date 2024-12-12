package it.eng.dome.invoicing.tedb.model;

import java.text.NumberFormat;
import java.text.ParseException;

public class Rate {

    public String label;
    public String value;

    public Number getValue() throws ParseException {
        final NumberFormat defaultFormat = NumberFormat.getPercentInstance();
        Number value = defaultFormat.parse(this.value.replaceAll(" ", ""));
        return value;
    }

}
