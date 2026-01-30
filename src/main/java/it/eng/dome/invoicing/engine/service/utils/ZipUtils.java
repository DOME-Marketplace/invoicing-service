package it.eng.dome.invoicing.engine.service.utils;

import it.eng.dome.invoicing.engine.service.render.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility class for building ZIP archives from collections of {@link Envelope}.
 * <p>
 * Supports exporting a set of envelopes to a single ZIP file ({@link #createZip})
 * or building a ZIP that contains one nested ZIP per invoice ({@link #zipPerInvoice}).
 */
public class ZipUtils {

    private static final Logger log = LoggerFactory.getLogger(ZipUtils.class);

    /**
     * Creates a ZIP archive containing one file per provided {@link Envelope}.
     * <p>
     * Each entry uses the envelope name and format (e.g. <code>INV001.xml</code>).
     * Only String and ByteArrayOutputStream contents are supported.
     *
     * @param envelopes the envelopes to include in the ZIP
     * @return a byte array containing the ZIP data
     * @throws IOException if an I/O error occurs while writing the ZIP
     */
    public static byte[] createZip(Collection<? extends Envelope<?>> envelopes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {

            if (envelopes == null || envelopes.isEmpty()) {
                log.warn("createZip: no envelopes provided → returning EMPTY but VALID zip");
            } else {
                for (Envelope<?> env : envelopes) {
                    if (env == null) {
                        log.warn("createZip: null envelope found → skipping");
                        continue;
                    }

                    String name = env.getName();
                    String format = env.getFormat();
                    Object content = env.getContent();

                    if (name == null || name.isBlank()
                            || format == null || format.isBlank()
                            || content == null) {
                        log.warn("createZip: invalid envelope → skipping (name={}, format={}, content={})",
                                name, format, content == null ? "null" : content.getClass().getName());
                        continue;
                    }

                    String fileName = name + "." + format;
                    zos.putNextEntry(new ZipEntry(fileName));

                    try {
                        if (content instanceof String s) {
                            zos.write(s.getBytes(StandardCharsets.UTF_8));
                        } else if (content instanceof ByteArrayOutputStream b) {
                            zos.write(b.toByteArray());
                        } else {
                            log.error("createZip: unsupported content type for '{}' → skipping (type={})",
                                    fileName, content.getClass().getName());
                        }
                    } finally {
                        zos.closeEntry();
                    }
                }
            }

            // Ensures the central directory is written even for empty ZIPs
            zos.finish();
        }

        return baos.toByteArray();
    }

    /**
     * Builds a ZIP archive containing one nested ZIP per invoice.
     * <p>
     * All envelopes are grouped by their name (invoice identifier).
     * For each group a ZIP is generated, and all resulting ZIPs are then packed
     * into the final parent ZIP.
     *
     * @param collections one or more collections of envelopes (e.g. XML, HTML, PDF)
     * @return a ZIP file containing one ZIP per invoice
     * @throws IOException if an error occurs during ZIP creation
     */
    @SafeVarargs
    public static byte[] zipPerInvoice(Collection<? extends Envelope<?>>... collections) throws IOException {

        Map<String, List<Envelope<?>>> grouped = new HashMap<>();

        if (collections == null) {
            log.warn("zipPerInvoice: collections is null → returning empty zip");
            return createZip(Collections.emptyList());
        }

        // Group envelopes by their name (only non-null/blank names)
        for (Collection<? extends Envelope<?>> col : collections) {
            if (col == null) {
                log.warn("zipPerInvoice: one of the provided collections is null → skipping");
                continue;
            }

            for (Envelope<?> env : col) {
                if (env == null) {
                    log.warn("zipPerInvoice: null envelope found → skipping");
                    continue;
                }

                String name = env.getName();
                if (name == null || name.isBlank()) {
                    log.warn("zipPerInvoice: envelope with null/blank name detected → skipping (env={})", env);
                    continue;
                }

                grouped.computeIfAbsent(name, x -> new ArrayList<>()).add(env);
            }
        }

        if (grouped.isEmpty()) {
            log.warn("zipPerInvoice: no valid envelopes found → returning empty zip");
            return createZip(Collections.emptyList());
        }

        // zip per each invoice
        Map<String, byte[]> perInvoiceZips = new HashMap<>();

        for (var e : grouped.entrySet()) {
            List<Envelope<?>> entries = e.getValue();

            if (entries.isEmpty()) {
                log.warn("zipPerInvoice: invoice '{}' has no valid entries → skipping", e.getKey());
                continue;
            }

            try {
                byte[] zip = createZip(entries);
                perInvoiceZips.put(e.getKey() + ".zip", zip);
            } catch (Exception ex) {
                log.error("zipPerInvoice: failed to create zip for invoice '{}' → skipping. Error: {}",
                        e.getKey(), ex.getMessage());
            }
        }

        // Create final zip containing per-invoice zips
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            for (var entry : perInvoiceZips.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                try {
                    zos.write(entry.getValue());
                } finally {
                    zos.closeEntry();
                }
            }
            zos.finish();
        }

        return out.toByteArray();
    }
}