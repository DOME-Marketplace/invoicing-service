package it.eng.dome.invoicing.engine.listener;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

import it.eng.dome.invoicing.engine.md.OpenApiMarkdownGenerator;

@Component
public class MarkdownGenerationListener {
	
	private static final Logger logger = LoggerFactory.getLogger(MarkdownGenerationListener.class);
	
	private final String API_DOCS_PATH = "/v3/api-docs";
	
	private final RestTemplate restTemplate;
	
	private final OpenApiMarkdownGenerator generator;

    @Value("${markdown_generation.api_docs:false}")
    private boolean generateApiDocs;

	@Value("${server.port}")
	private int serverPort;

	@Value("${server.servlet.context-path}")
	private String contextPath;

    public MarkdownGenerationListener(OpenApiMarkdownGenerator generator, RestTemplate restTemplate) {
        this.generator = generator;
        this.restTemplate = restTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void generateReadmeAfterStartup() {
    	
    	String path = contextPath + API_DOCS_PATH;
		String url = "http://localhost:" + serverPort + path.replaceAll("//+", "/");
		
		logger.info("GET call to {}", url);
    	
        if (!generateApiDocs) {
        	logger.info("No API doc generated. Set true the markdown_generation.api_docs in the application file");
            return;
        }

        try {

            logger.info("GET OpenAPI call to {}", url);            
            String json = restTemplate.getForObject(url, String.class);
            generator.generateMarkdownFromJson(json, "REST_APIs.md");

        } catch (Exception e) {
            logger.error("Error: {}", e.getMessage());
        }
    }
    
/*    
    public void generateMarkdownFromJson(String json, String outputPath) throws Exception {
        JsonNode root = mapper.readTree(json);

        String title = root.path("info").path("title").asText();
        String version = root.path("info").path("version").asText();
        String description = root.path("info").path("description").asText();

        // Header
        StringBuilder md = new StringBuilder();
        md.append("# ").append(title).append("\n\n")
          .append("**Version:** ").append(version).append("  \n")
          .append("**Description:** ").append(description).append("  \n")
          .append("\n\n")
          .append("## REST API Endpoints\n\n");

        // map of endpoints
        Map<String, List<String>> tagToEndpoints = new LinkedHashMap<>();

        JsonNode paths = root.path("paths");
        paths.fieldNames().forEachRemaining(path -> {
            JsonNode methods = paths.path(path);
            methods.fieldNames().forEachRemaining(method -> {
                JsonNode operation = methods.path(method);
                String tag = operation.path("tags").get(0).asText();
                String opId = operation.path("operationId").asText();
                String line = String.format("| %s | %s | %s |", method.toUpperCase(), path, opId);
                tagToEndpoints.computeIfAbsent(tag, k -> new ArrayList<>()).add(line);
            });
        });

        // Stampa sezioni per ogni tag/controller
        for (Map.Entry<String, List<String>> entry : tagToEndpoints.entrySet()) {
            md.append("### ").append(entry.getKey()).append("\n")
              .append("| Verb | Path | Task |\n")
              .append("|------|------|------|\n");
            entry.getValue().forEach(line -> md.append(line).append("\n"));
            md.append("\n");
        }

        // Scrivi su file
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.write(md.toString());
        }

        //System.out.println("✅ README.md generato in: " + outputPath);
    }
*/
}
