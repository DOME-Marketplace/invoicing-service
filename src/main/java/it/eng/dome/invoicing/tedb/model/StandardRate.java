package it.eng.dome.invoicing.tedb.model;

public class StandardRate {

    public String label;
    public Rate rate;

    public Clarification clarification;

    public Rate getRate() {
        return this.rate;
    }

}
