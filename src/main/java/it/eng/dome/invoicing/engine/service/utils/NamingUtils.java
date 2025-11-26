package it.eng.dome.invoicing.engine.service.utils;

import java.util.Collection;
import java.util.Objects;

import it.eng.dome.invoicing.engine.service.render.Envelope;

public class NamingUtils {

    public static <T> String extractFileNameFromEnvelopes(Collection<Envelope<T>> envelopes) {
        if (envelopes == null || envelopes.isEmpty()) {
            return null;
        }
        return envelopes.stream()
                .filter(Objects::nonNull)
                .map(Envelope::getName)
                .filter(n -> n != null && !n.isBlank())
                .findFirst()
                .orElse("inv");
    }

    public static String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "file";
        }
        String sanitized = filename.replaceAll("[\\\\/:*?\"<>|.,-]", "");
        sanitized = sanitized.replaceAll("\\s+", "");
        sanitized = sanitized.replaceAll("[\\p{Cntrl}]", "");
        if (sanitized.isBlank()) {
            return "file";
        }
        return sanitized;
    }
}