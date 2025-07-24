package it.eng.dome.invoicing.engine.md;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.util.*;

@Component
public class OpenApiMarkdownGenerator {

    private final ObjectMapper mapper = new ObjectMapper();

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
}