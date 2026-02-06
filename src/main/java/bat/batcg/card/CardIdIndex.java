package bat.batcg.card;

import bat.batcg.Batcg;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class CardIdIndex {

    // OJO: ahora apunta DENTRO de assets/batcg/
    private static final String RESOURCE = "assets/batcg/batcg_card_ids.txt";
    private static Set<String> IDS;

    private CardIdIndex() {}

    public static Collection<String> all() {
        ensureLoaded();
        return IDS;
    }

    public static boolean exists(String id) {
        ensureLoaded();
        return IDS.contains(normalize(id));
    }

    public static String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }

    private static void ensureLoaded() {
        if (IDS != null) return;

        Set<String> set = new HashSet<>();

        try (InputStream in = CardIdIndex.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                Batcg.LOGGER.error("[BATCG] NO se encontró {}", RESOURCE);
                IDS = Collections.unmodifiableSet(set);
                return;
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String s = normalize(line);
                    if (!s.isEmpty()) set.add(s);
                }
            }

            Batcg.LOGGER.info("[BATCG] CardIdIndex cargó {} ids desde {}", set.size(), RESOURCE);

        } catch (Exception e) {
            Batcg.LOGGER.error("[BATCG] Error leyendo " + RESOURCE, e);
        }

        IDS = Collections.unmodifiableSet(set);
    }
}
