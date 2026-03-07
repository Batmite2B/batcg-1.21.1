package bat.batcg.item;

import bat.batcg.card.CardTier;
import bat.batcg.card.ModCardComponents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Locale;

public class PokemonCardItem extends Item {

    public PokemonCardItem(Settings settings) {
        super(settings);
    }

    public static ItemStack createCard(String pokemonId, CardTier tier) {
        ModCardComponents.ensureInitialized();

        ItemStack stack = new ItemStack(ModItems.POKEMONCARD);
        stack.set(ModCardComponents.POKEMON_ID, pokemonId.toLowerCase(Locale.ROOT));
        stack.set(ModCardComponents.CARD_TIER, tier.name());
        return stack;
    }

    /** ✅ Getter seguro: devuelve null si no existe */
    public static String getPokemonId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String id = stack.get(ModCardComponents.POKEMON_ID);
        return (id == null || id.isBlank()) ? null : id;
    }

    /** ✅ Getter seguro: devuelve null si no existe / inválido */
    public static CardTier getTier(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        String tierName = stack.get(ModCardComponents.CARD_TIER);
        if (tierName == null || tierName.isBlank()) return null;

        try {
            return CardTier.valueOf(tierName.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }
    }

    // ✅ Nombre del item: color por rareza + estrella si SHINY
    @Override
    public Text getName(ItemStack stack) {
        String pokemonId = getPokemonId(stack);
        CardTier tier = getTier(stack);

        if (pokemonId == null) {
            return Text.literal("Invalid Card").formatted(Formatting.RED);
        }
        if (tier == null) tier = CardTier.COMMON;

        MutableText name = getPokemonNameText(pokemonId).copy().formatted(tier.getColor());

        if (tier == CardTier.SHINY) {
            return Text.literal("★ ").formatted(Formatting.GOLD).append(name);
        }

        return name;
    }

    // ✅ Tooltip SIN nombre (solo rareza)
    @Override
    public void appendTooltip(ItemStack stack, Item.TooltipContext context, List<Text> tooltip, TooltipType type) {
        CardTier tier = getTier(stack);

        if (tier == null) {
            tooltip.add(Text.literal("Invalid Card").formatted(Formatting.RED));
            return;
        }

        // Rarity line (siempre)
        tooltip.add(
                Text.literal("Rarity: ").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal(tier.getDisplayName()).formatted(tier.getColor()))
        );


        int g = bat.batcg.card.CardGradeData.getGrade(stack);
        tooltip.add(
                Text.literal("Grade: ").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal(bat.batcg.card.CardGradeData.label(g)).formatted(
                                switch (g) {
                                    case 1 -> Formatting.GRAY;         // C
                                    case 2 -> Formatting.AQUA;         // B
                                    case 3 -> Formatting.LIGHT_PURPLE; // A
                                    case 4 -> Formatting.GOLD;         // S
                                    default -> Formatting.DARK_GRAY;   // None
                                }
                        ))
        );

        tooltip.add(
                Text.literal("Hidden Power: ").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal(g >= 1 ? "Unlocked" : "Locked")
                                .formatted(g >= 1 ? Formatting.GREEN : Formatting.RED))
        );


        // Hint si no hay shift
        if (!isShiftDown()) {
            tooltip.add(Text.literal("Hold Shift to view belt effects").formatted(Formatting.DARK_GRAY));
            return;
        }

        String pokemonId = getPokemonId(stack);
        if (pokemonId == null) {
            tooltip.add(Text.literal("No data found").formatted(Formatting.DARK_GRAY));
            return;
        }

        // Perks base desde config
        var perks = bat.batcg.belt.CardPerkTable.get(pokemonId);
        if (perks.isEmpty()) {
            tooltip.add(Text.empty());
            tooltip.add(Text.literal("(none)").formatted(Formatting.DARK_GRAY));
            return;
        }

        // Escala por rareza (SHINY = 2.0x)
        double mult = rarityMultiplier(tier);
        var scaled = perks.scaled(mult);

        tooltip.add(Text.empty());
        tooltip.add(Text.literal("Tier multiplier: x" + nice(mult, 2)).formatted(Formatting.DARK_GRAY));

        // Mostrar SOLO lo que sea != 0
        addPercentLine(tooltip, "Mining Speed", scaled.miningBonus());
        addValueLine(tooltip, "Attack Damage", scaled.attackBonus());
        addValueLine(tooltip, "Armor", scaled.armorBonus());
        addPercentLine(tooltip, "Movement Speed", scaled.speedBonus());
        addHealthLine(tooltip, "Max Health", scaled.maxHealthBonus());
        addValueLine(tooltip, "Attack Speed", scaled.attackSpeedBonus());
        addValueLine(tooltip, "Luck", scaled.luckBonus());
        addValueLine(tooltip, "Knockback Resistance", scaled.knockbackResBonus());
    }

// -------------------------
// Tooltip helpers
// -------------------------

    private static void addPercentLine(List<Text> tooltip, String label, double value) {
        if (Math.abs(value) < 1.0E-9) return;
        double pct = value * 100.0;
        tooltip.add(Text.literal(label + ": ").formatted(Formatting.GRAY)
                .append(Text.literal(signed(nice(pct, 1)) + "%").formatted(Formatting.WHITE)));
    }

    private static void addValueLine(List<Text> tooltip, String label, double value) {
        if (Math.abs(value) < 1.0E-9) return;
        tooltip.add(Text.literal(label + ": ").formatted(Formatting.GRAY)
                .append(Text.literal(signed(nice(value, 2))).formatted(Formatting.WHITE)));
    }

    private static void addHealthLine(List<Text> tooltip, String label, double value) {
        if (Math.abs(value) < 1.0E-9) return;
        double hearts = value / 2.0;
        tooltip.add(Text.literal(label + ": ").formatted(Formatting.GRAY)
                .append(Text.literal(signed(nice(value, 2)) + " HP").formatted(Formatting.WHITE))
                .append(Text.literal(" (" + signed(nice(hearts, 2)) + " hearts)").formatted(Formatting.DARK_GRAY)));
    }

    private static String signed(String s) {
        return s.startsWith("-") ? s : "+" + s;
    }

    private static String nice(double v, int decimals) {
        String s = String.format(Locale.ROOT, "%." + decimals + "f", v);
        // trim trailing zeros
        while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static double rarityMultiplier(CardTier tier) {
        if (tier == null) return 1.0;
        return switch (tier) {
            case COMMON -> 0.6;
            case UNCOMMON -> 0.9;
            case RARE -> 1.15;
            case EPIC -> 1.4;
            case LEGENDARY -> 1.7;
            case SHINY -> 2.0; // ✅ Shiny más potente
        };
    }

    // Safe Shift detection (no client-only imports en common code)
    private static java.lang.reflect.Method SHIFT_DOWN;

    private static boolean isShiftDown() {
        try {
            if (SHIFT_DOWN == null) {
                Class<?> screen = Class.forName("net.minecraft.client.gui.screen.Screen");
                SHIFT_DOWN = screen.getMethod("hasShiftDown");
            }
            return (boolean) SHIFT_DOWN.invoke(null);
        } catch (Throwable ignored) {
            return false;
        }
    }

    // -------------------------
    // Helpers
    // -------------------------

    private static MutableText getPokemonNameText(String pokemonId) {
        String clean = pokemonId.toLowerCase(Locale.ROOT).trim();

        boolean shiny = clean.endsWith("shiny") && clean.length() > "shiny".length();
        String baseId = shiny ? clean.substring(0, clean.length() - "shiny".length()) : clean;

        String key = "pokemon.batcg." + baseId;

        MutableText name = Text.translatable(key);

        // fallback si no existe traducción
        if (name.getString().equals(key)) {
            name = Text.literal(prettyNameFromId(baseId));
        }

        return name;
    }

    private static String prettyNameFromId(String baseId) {
        String s = baseId.replaceFirst("^\\d+", "");
        if (s.isEmpty()) return "Unknown";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
