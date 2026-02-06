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
        ItemStack stack = new ItemStack(ModItems.POKEMONCARD);
        stack.set(ModCardComponents.POKEMON_ID, pokemonId.toLowerCase(Locale.ROOT));
        stack.set(ModCardComponents.CARD_TIER, tier.name());
        return stack;
    }

    // ✅ Nombre del item: color por rareza + estrella si SHINY
    @Override
    public Text getName(ItemStack stack) {
        String pokemonId = stack.get(ModCardComponents.POKEMON_ID);
        String tierName  = stack.get(ModCardComponents.CARD_TIER);

        if (pokemonId == null || pokemonId.isBlank()) {
            return Text.literal("Invalid Card").formatted(Formatting.RED);
        }

        CardTier tier = safeTier(tierName);

        MutableText name = getPokemonNameText(pokemonId).copy().formatted(tier.getColor());

        // ⭐ Solo para SHINY (sin "(Shiny)")
        if (tier == CardTier.SHINY) {
            return Text.literal("★ ").formatted(Formatting.GOLD).append(name);
        }

        return name;
    }

    // ✅ Tooltip SIN nombre (solo rareza)
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        String tierName = stack.get(ModCardComponents.CARD_TIER);

        if (tierName == null || tierName.isBlank()) {
            tooltip.add(Text.literal("Invalid Card").formatted(Formatting.RED));
            return;
        }

        CardTier tier = safeTier(tierName);

        tooltip.add(
                Text.literal("Rarity: ").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal(tier.getDisplayName()).formatted(tier.getColor()))
        );

        // (Opcional) Si quieres depurar IDs, descomenta:
        // String pokemonId = stack.get(ModCardComponents.POKEMON_ID);
        // if (pokemonId != null && !pokemonId.isBlank()) {
        //     tooltip.add(Text.literal(pokemonId).formatted(Formatting.DARK_GRAY));
        // }
    }

    // -------------------------
    // Helpers
    // -------------------------

    private static CardTier safeTier(String tierName) {
        if (tierName == null) return CardTier.COMMON;
        try {
            return CardTier.valueOf(tierName.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return CardTier.COMMON;
        }
    }

    /**
     * Nombre del Pokémon:
     * - Usa traducción si existe: "pokemon.batcg.<id>"
     * - Si no existe, fallback: "001bulbasaur" -> "Bulbasaur"
     * - Si el id termina en "shiny", usa el baseId (sin shiny)
     */
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
