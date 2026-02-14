package bat.batcg.card;

import bat.batcg.Batcg;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CardIdIndex {

    private static volatile List<String> ALL_IDS;   // sorted lowercase
    private static volatile List<String> NORMAL_IDS;
    private static volatile List<String> SHINY_IDS;

    private CardIdIndex() {}

    // --- Compat con tu ModCommands ---
    public static List<String> all() { return allIds(); }

    public static boolean exists(String id) { return isValid(id); }

    /**
     * Normaliza inputs tipo:
     * - "60Poliwag" -> "060poliwag"
     * - "060Poliwag.json" -> "060poliwag"
     * - "001bulbasaurShiny" -> "001bulbasaurshiny"
     * - "bulbasaur" -> intenta resolver si hay match único
     */
    public static String normalize(String raw) {
        if (raw == null) return "";
        String s = raw.trim().toLowerCase(Locale.ROOT);

        // strip extensions
        if (s.endsWith(".json")) s = s.substring(0, s.length() - 5);
        if (s.endsWith(".png"))  s = s.substring(0, s.length() - 4);

        // basic cleanup
        s = s.replace(" ", "")
                .replace("_", "")
                .replace("-", "");

        // pad dex if starts with 1-3 digits
        int i = 0;
        while (i < s.length() && Character.isDigit(s.charAt(i)) && i < 3) i++;
        if (i > 0) {
            String num = s.substring(0, i);
            String rest = s.substring(i);
            num = String.format(Locale.ROOT, "%03d", Integer.parseInt(num));
            s = num + rest;
            return s;
        }

        // If user typed only a name (no leading digits), try to resolve uniquely.
        ensureLoaded();
        String candidate = s;

        // exact match
        if (Collections.binarySearch(ALL_IDS, candidate) >= 0) return candidate;

        // unique suffix match (e.g. "bulbasaur" matches "001bulbasaur")
        List<String> matches = ALL_IDS.stream()
                .filter(id -> id.endsWith(candidate))
                .collect(Collectors.toList());

        if (matches.size() == 1) return matches.get(0);

        // also try shiny suffix
        matches = ALL_IDS.stream()
                .filter(id -> id.endsWith(candidate + "shiny"))
                .collect(Collectors.toList());

        if (matches.size() == 1) return matches.get(0);

        return candidate; // fallback
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

    /**
     * "060poliwag" -> 60
     * "1bulbasaur" -> 1
     * "bulbasaur"  -> -1
     */
    public static int dexNumber(String pokemonId) {
        if (pokemonId == null || pokemonId.isBlank()) return -1;
        String s = pokemonId.trim();
        int end = 0;
        while (end < s.length() && Character.isDigit(s.charAt(end)) && end < 3) end++;
        if (end == 0) return -1;
        try {
            return Integer.parseInt(s.substring(0, end));
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static void ensureLoaded() {
        if (ALL_IDS != null) return;

        synchronized (CardIdIndex.class) {
            if (ALL_IDS != null) return;

            final List<String> ids = new ArrayList<>();

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
