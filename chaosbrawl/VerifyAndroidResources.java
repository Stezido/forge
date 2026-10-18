import forge.app.ChaosBrawlResources;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.zip.*;

public class VerifyAndroidResources {
    static byte[] bundle(String name, String text) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer)) {
            zip.putNextEntry(new ZipEntry(name));zip.write(text.getBytes(StandardCharsets.UTF_8));zip.closeEntry();
        }
        return buffer.toByteArray();
    }
    static String hash(byte[] data) throws Exception {
        StringBuilder result = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-256").digest(data)) result.append(String.format("%02x", b & 255));
        return result.toString();
    }
    static void install(Path root, byte[] data) throws Exception {
        ChaosBrawlResources.install(new ByteArrayInputStream(data), root.toFile(), hash(data));
    }
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("chaosbrawl-android-test");
        install(root, bundle("data/decks/constructed/test.dck", "seed"));
        Files.writeString(root.resolve("data/decks/constructed/test.dck"), "player edits");
        install(root, bundle("data/decks/constructed/test.dck", "new seed"));
        if (!Files.readString(root.resolve("data/decks/constructed/test.dck")).equals("player edits")) throw new AssertionError("Overwrote user deck");
        install(root, bundle("res/cards/custom.txt", "first"));install(root, bundle("res/cards/custom.txt", "updated"));
        if (!Files.readString(root.resolve("res/cards/custom.txt")).equals("updated")) throw new AssertionError("Resource upgrade failed");
        byte[] malicious = bundle("../escaped.txt", "unsafe");
        try { install(root, malicious);throw new AssertionError("Traversal accepted"); } catch (IOException expected) {}
        if (Files.exists(root.getParent().resolve("escaped.txt"))) throw new AssertionError("Escaped sandbox");
        try { ChaosBrawlResources.install(new ByteArrayInputStream(bundle("res/fake.txt", "bad")), root.toFile(), "0".repeat(64));throw new AssertionError("Hash accepted"); } catch (IOException expected) {}
        if (Files.exists(root.resolve("res/fake.txt"))) throw new AssertionError("Unverified data installed");
        byte[] identical = bundle("res/unchanged.txt", "original");install(root, identical);
        Files.writeString(root.resolve("res/unchanged.txt"), "keep");install(root, identical);
        if (!Files.readString(root.resolve("res/unchanged.txt")).equals("keep")) throw new AssertionError("Same-version reinstall");
        System.out.println("PASS sandbox, digest, resource upgrade, seed persistence and same-version restart");
    }
}
