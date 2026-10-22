import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Release dependency audit: both restored entry points must enable custom mechanics. */
public class VerifyWindowsLaunchers {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        for (String mode : List.of("chaos-brawl", "adventure")) {
            String jar = mode.equals("adventure") ? "forge-adventure.jar" : "forge.jar";
            Path script = root.resolve("support/launch-" + mode + ".cmd");
            String command = Files.readString(script, StandardCharsets.US_ASCII);
            for (String required : List.of("cd /d \"%~dp0..\"", "-Dforge.chaosbrawl=true", "-Dsentry.enabled=false",
                    "support\\runtime\\bin\\javaw.exe", "-jar support/" + jar)) {
                if (!command.contains(required)) throw new IllegalStateException(script + " missing " + required);
            }
            if (!command.contains("\r\n")) throw new IllegalStateException("Windows CRLF required: " + script);
            for (String dependency : List.of("support/" + jar, "support/runtime/bin/javaw.exe", "forge.profile.properties")) {
                if (!Files.isRegularFile(root.resolve(dependency))) throw new IllegalStateException("Missing " + dependency);
            }
        }
        for (String executable : List.of("Chaos Brawl Forge.exe", "Chaos Brawl Adventure.exe")) {
            byte[] bytes = Files.readAllBytes(root.resolve(executable));
            String ascii = new String(bytes, StandardCharsets.ISO_8859_1);
            String unicode = new String(bytes, StandardCharsets.UTF_16LE);
            for (String property : List.of("-Dforge.chaosbrawl=true", "-Dsentry.enabled=false")) {
                if (!ascii.contains(property) && !unicode.contains(property)) {
                    throw new IllegalStateException(executable + " missing " + property);
                }
            }
        }
        System.out.println("AUDIT: both CMD launchers, EXE custom-mode flags and runtime dependencies verified");
    }
}
