package it.eng.dome.invoicing.engine.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"TMF_ENDPOINT=https://dome-dev.eng.it"})
public class InvoicingServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private BuildProperties buildProperties;

    @Test
    public void shouldReturnExpectedMessage() throws Exception {

        mockMvc.perform(get("/invoicing/info").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.version").value(buildProperties.getVersion()))
            .andExpect(jsonPath("$.name").value(buildProperties.getName()));
    }
    
    @Test
    public void shouldSendPreviewTaxes() throws Exception {

    	String json = Files.readString(Paths.get("src/test/resources/previewTaxes.json"));

        mockMvc.perform(post("/invoicing/previewTaxes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isOk());
    }
    
    @Test
    public void shouldSendApplyTaxes() throws Exception {

    	String json = Files.readString(Paths.get("src/test/resources/applyTaxes.json"));

        mockMvc.perform(post("/invoicing/applyTaxes")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
            .andExpect(status().isOk());
    }
}
