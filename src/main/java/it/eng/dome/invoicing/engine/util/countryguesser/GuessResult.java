package it.eng.dome.invoicing.engine.util.countryguesser;

import java.util.HashSet;
import java.util.Set;

public class GuessResult {
    private String countryCode;
    private Float score;
    private Set<String> matchedPatterns;

    public GuessResult(String countryCode) {
        this(countryCode, 0f);
    }

    public GuessResult(String countryCode, Float score) {
        this.countryCode = countryCode;
        this.score = score;
        this.matchedPatterns = new HashSet<>();
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public Float getScore() {
        return this.score;
    }

    public Set<String> getMatchedPatterns() {
        return this.matchedPatterns;
    }

    public void addMatchedPattern(String pattern) {
        this.matchedPatterns.add(pattern);
    }

    public void incScore() {
        this.score++;
    }

}