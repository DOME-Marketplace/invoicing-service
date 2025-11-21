package it.eng.dome.invoicing.engine.service.utils;

import it.eng.dome.invoicing.engine.service.render.Envelope;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    /**
     * Crea uno ZIP a partire da una collezione di Envelope generici.
     * Gestisce automaticamente String (come UTF-8) e ByteArrayOutputStream.
     */
    public static <T> byte[] createZip(Collection<Envelope<T>> envelopes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            int counter = 1;

            for (Envelope<?> env : envelopes) {
                String name = (env.getName() != null && !env.getName().isEmpty()) ? env.getName() : "inv";
                String format = (env.getFormat() != null && !env.getFormat().isEmpty()) ? env.getFormat() : "dat";
                String fileName = name + "-" + counter++ + "." + format;

                ZipEntry entry = new ZipEntry(fileName);
                zos.putNextEntry(entry);

                if (env.getContent() instanceof String s) {
                    zos.write(s.getBytes(StandardCharsets.UTF_8));
                } else if (env.getContent() instanceof ByteArrayOutputStream b) {
                    zos.write(b.toByteArray());
                } else {
                    throw new IllegalArgumentException("Unsupported content type: " + env.getContent().getClass());
                }

                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}