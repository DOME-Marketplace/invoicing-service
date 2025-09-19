
package it.eng.dome.invoicing.util.countryguesser;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import it.eng.dome.tmforum.tmf632.v4.model.Organization;

public class CountryGuesser {

    private List<CountryPattern> patterns;

    public CountryGuesser() throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream("country-guesser/country-hints.json");
        String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        this.patterns = new ArrayList<>();
        this.patterns.addAll(this.loadPatternsFromJSON(json));
    }

    public CountryGuesser(String fileName) throws IOException {
        this.patterns = new ArrayList<>();
        this.patterns.addAll(this.loadPatterns(fileName));
    }

    private List<CountryPattern> loadPatternsFromJSON(String jsonString) throws IOException {

        List<CountryPattern> out = new ArrayList<>();

        JSONArray patterns = new JSONArray(jsonString);

        for (int i = 0; i < patterns.length(); ++i) {
            JSONObject pattern = patterns.getJSONObject(i);
            CountryPattern cp = new CountryPattern(pattern);
            out.add(cp);
        }
        
        return out;

    }

    private List<CountryPattern> loadPatterns(String fileName) throws IOException {

        List<CountryPattern> out = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), StandardCharsets.UTF_8));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        String ls = System.getProperty("line.separator");
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }
        reader.close();
        String jsonString = stringBuilder.toString();

        JSONArray patterns = new JSONArray(jsonString);

        for (int i = 0; i < patterns.length(); ++i) {
            JSONObject pattern = patterns.getJSONObject(i);
            CountryPattern cp = new CountryPattern(pattern);
            out.add(cp);
        }
        
        return out;

    }

    /**
     * Guess the country of the given organisation
     * @param organization
     * @return a sorted list of guess results
     * @throws Exception
     */
    public List<GuessResult> guessCountry(Organization organization) throws Exception {
        List<GuessResult> out = new ArrayList<>();
        for(CountryPattern cp:this.patterns) {
            GuessResult gr = cp.matches(organization);
            if(gr.getScore()>0) {
                out.add(gr);
            }
        }
        Collections.sort(out, (a,b) -> b.getScore().compareTo(a.getScore()));
        return out;
    }



}
