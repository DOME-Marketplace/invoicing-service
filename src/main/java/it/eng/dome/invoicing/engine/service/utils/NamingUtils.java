package it.eng.dome.invoicing.engine.service.utils;

import java.util.Collection;
import java.util.Objects;

import it.eng.dome.invoicing.engine.service.render.Envelope;

/**
 * Utility class for generating and sanitizing filenames used when exporting
 * invoices or other Envelope-based resources.
 */
public class NamingUtils {

    /**
     * Extracts the first valid (non-null and non-blank) name from a collection of
     * {@link Envelope}.
     * <p>
     * If no valid name is found, the default value <code>"inv"</code> is returned.
     *
     * @param envelopes the collection of envelope objects
     * @param <T>       the content type inside the envelope
     * @return the first valid envelope name, or <code>"inv"</code> if none found
     */
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

    /**
     * Sanitizes a filename by removing invalid or unsafe characters.
     * <p>
     * Removes special characters, whitespace, and non-printable characters.
     * If the resulting name is empty, the fallback value <code>"file"</code>
     * is returned.
     *
     * @param filename the original filename
     * @return a sanitized and safe filename
     */
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