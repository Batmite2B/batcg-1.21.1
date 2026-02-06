package bat.batcg.card;

import bat.batcg.Batcg;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CardIdIndex {

    private static volatile List<String> ALL_IDS; // sorted lowercase
    private static volatile List<String> NORMAL_IDS;
    private static volatile List<String> SHINY_IDS;

    private CardIdIndex() {}

    // --- Compat con tu ModCommands ---
    public static List<String> all() { return allIds(); }
    public static boolean exists(String id) { return isValid(id); }
    public static String normalize(String raw) {
        if (raw == null) return "";
        return raw.toLowerCase(Locale.ROOT).trim();
    }
    // -------------------------------

    public static List<String> allIds() {
        ensureLoaded();
        return ALL_IDS;
    }

    public static List<String> normalIds() {
        ensureLoaded();
        return NORMAL_IDS;
    }

    public static List<String> shinyIds() {
        ensureLoaded();
        return SHINY_IDS;
    }

    public static boolean isValid(String id) {
        if (id == null) return false;
        ensureLoaded();
        String n = normalize(id);
        return Collections.binarySearch(ALL_IDS, n) >= 0;
    }

    private static void ensureLoaded() {
        if (ALL_IDS != null) return;

        synchronized (CardIdIndex.class) {
            if (ALL_IDS != null) return;

            final List<String> ids = new ArrayList<>(); // <-- ya no se reasigna

            var container = FabricLoader.getInstance()
                    .getModContainer(Batcg.MOD_ID)
                    .orElseThrow(() -> new IllegalStateException("Mod container not found: " + Batcg.MOD_ID));

            for (Path root : container.getRootPaths()) {
                Path icons = root.resolve("assets")
                        .resolve(Batcg.MOD_ID)
                        .resolve("models")
                        .resolve("item")
                        .resolve("card")
                        .resolve("icon");

                if (!Files.exists(icons)) continue;

                try (Stream<Path> walk = Files.walk(icons)) {
                    walk.filter(p -> p.getFileName().toString().endsWith(".json"))
                            .forEach(p -> {
                                String file = p.getFileName().toString();
                                String base = file.substring(0, file.length() - 5).toLowerCase(Locale.ROOT);
                                ids.add(base);
                            });
                } catch (IOException e) {
                    throw new RuntimeException("Failed reading card icon models from: " + icons, e);
                }
            }

            // Creamos una lista nueva en vez de reasignar "ids"
            List<String> sortedDistinct = ids.stream()
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());

            List<String> shiny = sortedDistinct.stream()
                    .filter(s -> s.endsWith("shiny"))
                    .collect(Collectors.toList());

            List<String> normal = sortedDistinct.stream()
                    .filter(s -> !s.endsWith("shiny"))
                    .collect(Collectors.toList());

            ALL_IDS = sortedDistinct;
            SHINY_IDS = shiny;
            NORMAL_IDS = normal;
        }
    }
}
