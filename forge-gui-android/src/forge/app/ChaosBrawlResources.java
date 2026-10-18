package forge.app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Installs reviewed resources in the app sandbox; seeds, never replaces, user data. */
public final class ChaosBrawlResources {
    private ChaosBrawlResources() {}

    public static synchronized void install(InputStream archive, File destination, String expected) throws Exception {
        if (!expected.matches("[a-f0-9]{64}")) throw new IOException("Invalid bundle digest");
        Path root = destination.toPath();
        Files.createDirectories(root);
        Path marker = root.resolve(".chaosbrawl-resources.sha256");
        if (Files.exists(marker) && expected.equals(new String(Files.readAllBytes(marker), StandardCharsets.US_ASCII).trim())) return;
        Path temporary = Files.createTempFile(root, "resources-", ".zip");
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[65536];
            long packed = 0;
            try (OutputStream output = Files.newOutputStream(temporary)) {
                int read;
                while ((read = archive.read(buffer)) != -1) {
                    packed += read;
                    if (packed > 1_000_000_000L) throw new IOException("Bundle too large");
                    hash.update(buffer, 0, read);
                    output.write(buffer, 0, read);
                }
            }
            StringBuilder actual = new StringBuilder();
            for (byte value : hash.digest()) actual.append(String.format("%02x", value & 255));
            if (!expected.equals(actual.toString())) throw new IOException("Bundle integrity check failed");
            long expanded = 0;
            try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(temporary))) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    String name = entry.getName();
                    if (name.startsWith("/") || name.contains("\\") || name.contains("../")) throw new IOException("Unsafe bundle path");
                    Path target = root.resolve(name).normalize();
                    if (!target.startsWith(root) || target.equals(root)) throw new IOException("Unsafe bundle path");
                    if (entry.isDirectory()) { Files.createDirectories(target); continue; }
                    boolean seed = name.startsWith("data/");
                    if (seed && Files.exists(target)) continue;
                    Files.createDirectories(target.getParent());
                    Path part = Files.createTempFile(target.getParent(), ".install-", ".tmp");
                    try {
                        try (OutputStream output = Files.newOutputStream(part)) {
                            int read;
                            while ((read = zip.read(buffer)) != -1) {
                                expanded += read;
                                if (expanded > 1_500_000_000L) throw new IOException("Expanded bundle too large");
                                output.write(buffer, 0, read);
                            }
                        }
                        Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
                    } finally { Files.deleteIfExists(part); }
                }
            }
            Files.write(marker, expected.getBytes(StandardCharsets.US_ASCII));
        } finally { Files.deleteIfExists(temporary); }
    }
}
