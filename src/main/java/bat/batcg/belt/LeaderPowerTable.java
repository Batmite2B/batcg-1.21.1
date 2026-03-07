package bat.batcg.belt;

import bat.batcg.Batcg;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LeaderPowerTable {

    private LeaderPowerTable() {}

    private static final String CONFIG_FILE = "batcg-leader-powers.txt";
    private static volatile boolean LOADED = false;

    public enum PowerId {
        NONE,
        ENTANGLE,
        FIRE_BREATH,
        BARRIER,
        SLEEP_PULSE,
        VENOM_CLOUD,
        TAILWIND,
        DASH,
        SHOCK_NOVA,
        SLAM,
        CHARGE,
        WATER_JET,
        BUBBLE_BEAM,
        ICE_NOVA,
        BLINK,
        SHADOW_PHASE,
        HEAL,
        REGEN_AURA,
        MAGNET,
        WATER_HOP,
        DRAGON_RUSH,
        INTIMIDATE_ROAR,
        MIMIC,
        PSYCHIC_CRUSH,
        ARCANE_SHIFT
    }

    /** value / radius / duration / cooldown / amp: interpretados según el PowerId */
    public record LeaderPower(PowerId id, double value, double radius, int durationTicks, int cooldownTicks, int amp) {
        public static final LeaderPower NONE = new LeaderPower(PowerId.NONE, 0, 0, 0, 0, 0);
        public boolean isNone() { return id == PowerId.NONE; }
    }

    private static final Map<String, LeaderPower> MAP = new HashMap<>();

    public static LeaderPower get(String pokemonId) {
        ensureLoaded();
        if (pokemonId == null || pokemonId.isBlank()) return LeaderPower.NONE;

        String k = normalize(pokemonId);
        LeaderPower p = MAP.get(k);
        if (p != null) return p;

        // fallback 1: remover tokens comunes de shiny si existieran
        String k2 = k.replace("_shiny", "").replace("shiny", "");
        p = MAP.get(k2);
        if (p != null) return p;

        // fallback 2: si viene con prefijo raro, intenta quedarte con los últimos 12-15 chars
        // (por si el id viene como "batcg:025pikachu" o algo similar)
        int idx = k.lastIndexOf(':');
        if (idx >= 0 && idx + 1 < k.length()) {
            p = MAP.get(k.substring(idx + 1));
            if (p != null) return p;
        }

        return LeaderPower.NONE;
    }

    private static String normalize(String s) {
        return s.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace("-", "").replace("__", "_");
    }

    public static void reload() {
        synchronized (LeaderPowerTable.class) {
            LOADED = false;
            load();
            LOADED = true;
        }
    }

    private static void ensureLoaded() {
        if (LOADED) return;
        synchronized (LeaderPowerTable.class) {
            if (LOADED) return;
            load();
            LOADED = true;
        }
    }

    private static void load() {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            Path file = configDir.resolve(CONFIG_FILE);

            if (!Files.exists(file)) {
                Batcg.LOGGER.warn("Leader powers file not found: {} (create it in config/).", file.toAbsolutePath());
                return;
            }

            MAP.clear();
            List<String> lines = Files.readAllLines(file);

            int ok = 0;
            for (String raw : lines) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] p = line.split("\\s+");
                // pokemon_id POWER value radius duration_s cooldown_s amp
                if (p.length < 7) continue;

                String pokemonId = p[0].toLowerCase(Locale.ROOT);

                PowerId id;
                try { id = PowerId.valueOf(p[1].toUpperCase(Locale.ROOT)); }
                catch (Exception e) { id = PowerId.NONE; }

                double value = parseDouble(p[2]);
                double radius = parseDouble(p[3]);
                int durationTicks = Math.max(0, parseInt(p[4]) * 20);
                int cooldownTicks = Math.max(0, parseInt(p[5]) * 20);
                int amp = Math.max(0, parseInt(p[6]));

                MAP.put(pokemonId, new LeaderPower(id, value, radius, durationTicks, cooldownTicks, amp));
                ok++;
            }

            Batcg.LOGGER.info("Loaded leader powers: {} entries from {}", ok, file.toAbsolutePath());
        } catch (Exception e) {
            Batcg.LOGGER.error("Failed to load leader powers file", e);
        }
    }

    private static double parseDouble(String s) { try { return Double.parseDouble(s); } catch (Exception e) { return 0; } }
    private static int parseInt(String s) { try { return Integer.parseInt(s); } catch (Exception e) { return 0; } }
}