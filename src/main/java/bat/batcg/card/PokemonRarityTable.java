package bat.batcg.card;

import bat.batcg.Batcg;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Exact-tier rarity table:
 * - Each pokemonId belongs to ONE CardTier bucket.
 * - When booster rolls a tier, we only pick from that bucket.
 *
 * Reads: assets/batcg/batcg_rarity.txt
 * Format per line:
 *   <id> <TIER> [weight]
 * Example:
 *   149dragonite EPIC 2
 */
public final class PokemonRarityTable {

    private record Rule(CardTier tier, int weight) {}

    private static final Map<String, Rule> RULES = new HashMap<>();
    private static volatile boolean LOADED = false;

    private PokemonRarityTable() {}

    /** For debug: what tier bucket is this id assigned to? (defaults to COMMON). */
    public static CardTier getAssignedTier(String rawId) {
        ensureLoaded();
        Rule r = RULES.get(normalizeBase(rawId));
        return (r != null) ? r.tier : CardTier.COMMON;
    }

    /** Pick pokemonId from the EXACT rolled tier bucket (weighted). */
    public static String pickPokemonId(Random rng, CardTier rolledTier) {
        ensureLoaded();

        if (rolledTier == CardTier.SHINY) {
            // Shiny tier: pick only from shiny IDs (weighted by base rule if present)
            List<String> pool = CardIdIndex.shinyIds();
            if (pool.isEmpty()) return null;

            long total = 0;
            for (String id : pool) {
                int w = weightFor(id);
                total += w;
            }
            if (total <= 0) return normalize(pool.get(rng.nextInt(pool.size())));

            long roll = nextLongBounded(rng, total);
            for (String id : pool) {
                roll -= weightFor(id);
                if (roll < 0) return normalize(id);
            }
            return normalize(pool.get(rng.nextInt(pool.size())));
        }

        // Normal tiers: pick only from normal IDs whose assigned tier == rolledTier
        List<String> pool = CardIdIndex.normalIds();
        if (pool.isEmpty()) return null;

        long total = 0;
        for (String id : pool) {
            Rule r = RULES.get(normalizeBase(id));
            CardTier bucket = (r != null) ? r.tier : CardTier.COMMON;
            if (bucket != rolledTier) continue;

            int w = (r != null) ? r.weight : 10; // default weight for unspecified COMMONs
            total += Math.max(1, w);
        }

        if (total <= 0) {
            // This means your batcg_rarity.txt doesn't define anything for this tier.
            Batcg.LOGGER.warn("[BATCG] No candidates for tier {}. Check batcg_rarity.txt", rolledTier);
            return null;
        }

        long roll = nextLongBounded(rng, total);
        for (String id : pool) {
            Rule r = RULES.get(normalizeBase(id));
            CardTier bucket = (r != null) ? r.tier : CardTier.COMMON;
            if (bucket != rolledTier) continue;

            int w = (r != null) ? r.weight : 10;
            roll -= Math.max(1, w);
            if (roll < 0) return normalize(id);
        }

        return null;
    }

    // ---------------- internals ----------------

    private static int weightFor(String shinyOrNormalId) {
        Rule r = RULES.get(normalizeBase(shinyOrNormalId));
        if (r == null) return 1;
        return Math.max(1, r.weight);
    }

    private static void ensureLoaded() {
        if (LOADED) return;
        synchronized (PokemonRarityTable.class) {
            if (LOADED) return;

            RULES.clear();

            // Load from resources:
            String path = "assets/" + Batcg.MOD_ID + "/batcg_rarity.txt";
            try (InputStream in = PokemonRarityTable.class.getClassLoader().getResourceAsStream(path)) {
                if (in == null) {
                    Batcg.LOGGER.warn("[BATCG] Missing {} (rarity table). Using defaults: all COMMON.", path);
                    LOADED = true;
                    return;
                }

                try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    String line;
                    int count = 0;

                    while ((line = br.readLine()) != null) {
                        line = stripBom(line).trim();
                        if (line.isEmpty()) continue;
                        if (line.startsWith("#") || line.startsWith("//")) continue;

                        String[] parts = line.split("\\s+");
                        if (parts.length < 2) continue;

                        String id = normalize(parts[0]);
                        String tierStr = parts[1].toUpperCase(Locale.ROOT);

                        CardTier tier;
                        try {
                            tier = CardTier.valueOf(tierStr);
                        } catch (Exception ignored) {
                            continue;
                        }

                        int weight = 1;
                        if (parts.length >= 3) {
                            try {
                                weight = Integer.parseInt(parts[2]);
                            } catch (Exception ignored) {
                                weight = 1;
                            }
                        }

                        RULES.put(normalizeBase(id), new Rule(tier, Math.max(1, weight)));
                        count++;
                    }

                    Batcg.LOGGER.info("[BATCG] Loaded {} rarity rules from {}", count, path);
                }
            } catch (Exception e) {
                Batcg.LOGGER.error("[BATCG] Failed loading rarity table", e);
            }

            LOADED = true;
        }
    }

    private static String normalize(String id) {
        return (id == null) ? "" : id.toLowerCase(Locale.ROOT).trim();
    }

    private static String normalizeBase(String raw) {
        String id = normalize(raw);
        if (id.endsWith("shiny")) {
            id = id.substring(0, id.length() - "shiny".length());
        }
        return id;
    }

    private static String stripBom(String s) {
        if (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF') return s.substring(1);
        return s;
    }

    private static long nextLongBounded(Random rng, long boundExclusive) {
        if (boundExclusive <= Integer.MAX_VALUE) return rng.nextInt((int) boundExclusive);

        long r;
        do {
            r = rng.nextLong() >>> 1;
        } while (r == Long.MAX_VALUE);
        return r % boundExclusive;
    }
}
