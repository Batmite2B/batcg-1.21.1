package bat.batcg.belt;

import bat.batcg.Batcg;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class CardPerkTable {

    private CardPerkTable() {}

    // Se crea en: .minecraft/config/ o config/ del servidor
    private static final String CONFIG_FILE = "batcg-card-attributes.txt";

    // Default dentro del JAR (resources)
    private static final String DEFAULT_RESOURCE_PATH = "/assets/batcg/default_card_attributes.txt";

    private static volatile boolean LOADED = false;
    private static final Map<String, Perks> PERKS = new HashMap<>();

    public record Perks(
            double miningBonus,            // 0.10 = +10% velocidad minado (multiplicador)
            double attackBonus,            // +daño
            double armorBonus,             // +armadura
            double speedBonus,             // 0.02 = +2% speed (mult total)
            double maxHealthBonus,         // +vida (2 = +1 corazón)
            double attackSpeedBonus,       // +attack speed
            double luckBonus,              // +luck
            double knockbackResBonus       // +knockback resistance
    ) {
        public static Perks empty() {
            return new Perks(0,0,0,0,0,0,0,0);
        }

        public boolean isEmpty() {
            return miningBonus == 0 && attackBonus == 0 && armorBonus == 0 && speedBonus == 0
                    && maxHealthBonus == 0 && attackSpeedBonus == 0 && luckBonus == 0 && knockbackResBonus == 0;
        }

        public Perks scaled(double mult) {
            if (mult == 1.0) return this;
            return new Perks(
                    miningBonus * mult,
                    attackBonus * mult,
                    armorBonus * mult,
                    speedBonus * mult,
                    maxHealthBonus * mult,
                    attackSpeedBonus * mult,
                    luckBonus * mult,
                    knockbackResBonus * mult
            );
        }
    }

    public static Perks get(String pokemonId) {
        ensureLoaded();
        if (pokemonId == null || pokemonId.isBlank()) return Perks.empty();
        return PERKS.getOrDefault(normalizeKey(pokemonId), Perks.empty());
    }

    /** ✅ Opcional: recargar sin reiniciar */
    public static void reload() {
        synchronized (CardPerkTable.class) {
            LOADED = false;
            load();
            LOADED = true;
        }
    }

    private static void ensureLoaded() {
        if (LOADED) return;
        synchronized (CardPerkTable.class) {
            if (LOADED) return;
            load();
            LOADED = true;
        }
    }

    private static void load() {
        PERKS.clear();

        Path cfg = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);

        try {
            // Asegura que exista el directorio config
            Files.createDirectories(cfg.getParent());

            // Si no existe el archivo, lo crea copiando desde resources (default)
            if (!Files.exists(cfg)) {
                boolean copied = copyDefaultFromResources(cfg);
                if (!copied) {
                    // fallback: si por alguna razón no existe el resource
                    writeFallbackTemplate(cfg);
                }
            }

            readConfig(cfg);
            Batcg.LOGGER.info("Loaded card attributes: {} entries from {}", PERKS.size(), cfg.toAbsolutePath());

        } catch (Exception e) {
            Batcg.LOGGER.error("Failed loading card attributes config: " + cfg.toAbsolutePath(), e);
        }
    }

    /** Copia el default del JAR a config/batcg-card-attributes.txt */
    private static boolean copyDefaultFromResources(Path cfg) {
        try (InputStream in = Batcg.class.getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
            if (in == null) {
                Batcg.LOGGER.warn("Missing default resource: {}", DEFAULT_RESOURCE_PATH);
                return false;
            }
            Files.copy(in, cfg, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            Batcg.LOGGER.error("Failed to copy default card attributes from resources to " + cfg.toAbsolutePath(), e);
            return false;
        }
    }

    /** Fallback mínimo si no existe el resource por error */
    private static void writeFallbackTemplate(Path cfg) throws IOException {
        String template =
                "# BATCG - Card Attributes (FALLBACK)\n" +
                        "# No se encontró " + DEFAULT_RESOURCE_PATH + " dentro del JAR.\n" +
                        "# Formato: <pokemonId> key=value key=value ...\n" +
                        "# Ejemplo: 025pikachu speed=0.02 mining=0.08\n" +
                        "#\n" +
                        "# Keys soportadas:\n" +
                        "#  mining=0.10        -> +10% minado (multiplicador)\n" +
                        "#  attack=1.0         -> +1 daño\n" +
                        "#  armor=1.0          -> +1 armadura\n" +
                        "#  speed=0.02         -> +2% velocidad movimiento\n" +
                        "#  health=2.0         -> +2 vida (1 corazón)\n" +
                        "#  attackspeed=0.2    -> +0.2 attack speed\n" +
                        "#  luck=1.0           -> +1 luck\n" +
                        "#  knockbackres=0.1   -> +0.1 knockback resistance\n" +
                        "\n" +
                        "025pikachu speed=0.02 mining=0.08\n" +
                        "001bulbasaur armor=0.6 health=1.0\n" +
                        "006charizard attack=1.2 attackspeed=0.10\n" +
                        "150mewtwo attack=2.0 attackspeed=0.35 luck=0.5\n";

        Files.writeString(cfg, template, StandardCharsets.UTF_8);
    }

    private static void readConfig(Path cfg) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(cfg, StandardCharsets.UTF_8)) {
            String line;
            int ln = 0;
            while ((line = br.readLine()) != null) {
                ln++;
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("#")) continue;
                if (line.startsWith("//")) continue;

                String[] parts = line.split("\\s+");
                if (parts.length < 2) continue;

                String id = normalizeKey(parts[0]);

                double mining=0, attack=0, armor=0, speed=0, health=0, atkSpd=0, luck=0, kb=0;

                for (int i = 1; i < parts.length; i++) {
                    String token = parts[i];
                    int eq = token.indexOf('=');
                    if (eq <= 0 || eq >= token.length()-1) continue;

                    String key = token.substring(0, eq).toLowerCase(Locale.ROOT);
                    String valStr = token.substring(eq+1);

                    double val;
                    try { val = Double.parseDouble(valStr); }
                    catch (NumberFormatException nfe) {
                        Batcg.LOGGER.warn("Bad number in {} line {}: {}", cfg.getFileName(), ln, token);
                        continue;
                    }

                    switch (key) {
                        case "mining" -> mining = val;
                        case "attack" -> attack = val;
                        case "armor" -> armor = val;
                        case "speed" -> speed = val;
                        case "health" -> health = val;
                        case "attackspeed" -> atkSpd = val;
                        case "luck" -> luck = val;
                        case "knockbackres" -> kb = val;
                        default -> Batcg.LOGGER.warn("Unknown key in {} line {}: {}", cfg.getFileName(), ln, key);
                    }
                }

                PERKS.put(id, new Perks(mining, attack, armor, speed, health, atkSpd, luck, kb));
            }
        }
    }

    private static String normalizeKey(String raw) {
        String s = raw.trim().toLowerCase(Locale.ROOT);

        // Permite que alguien ponga "batcg:025pikachu"
        int colon = s.indexOf(':');
        if (colon >= 0 && colon < s.length()-1) s = s.substring(colon + 1);

        // Por si alguien pone "001bulbasaurshiny" por error
        if (s.endsWith("shiny")) s = s.substring(0, s.length() - "shiny".length());

        return s;
    }
}