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

    // ✅ Label del item (lo que ves como "nombre" del item)
    @Override
    public Text getName(ItemStack stack) {
        String pokemonId = stack.get(ModCardComponents.POKEMON_ID);
        if (pokemonId == null || pokemonId.isBlank()) {
            return Text.literal("Invalid Card").formatted(Formatting.RED);
        }
        return getPokemonNameText(pokemonId);
    }

    // ✅ Tooltip: nombre + rareza con color
    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        String pokemonId = stack.get(ModCardComponents.POKEMON_ID);
        String tierName  = stack.get(ModCardComponents.CARD_TIER);

        if (pokemonId == null || pokemonId.isBlank() || tierName == null || tierName.isBlank()) {
            tooltip.add(Text.literal("Invalid Card").formatted(Formatting.RED));
            return;
        }

        CardTier tier = safeTier(tierName);

        // Línea 1: Nombre bonito (gris)
        tooltip.add(getPokemonNameText(pokemonId).copy().formatted(Formatting.GRAY));

        // Línea 2: Rareza con color
        tooltip.add(
                Text.literal("Rarity: ").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal(tier.getDisplayName()).formatted(tier.getColor()))
        );
    }

    // -------------------------
    // Helpers
    // -------------------------

    private static CardTier safeTier(String tierName) {
        try {
            return CardTier.valueOf(tierName.toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return CardTier.COMMON;
        }
    }

    /**
     * - Usa traducción si existe: "pokemon.batcg.<id>"
     * - Si no existe: fallback: "001bulbasaur" -> "Bulbasaur"
     * - Si termina en "shiny": añade "(Shiny)"
     */
    private static MutableText getPokemonNameText(String pokemonId) {
        String clean = pokemonId.toLowerCase(Locale.ROOT).trim();

        boolean shiny = clean.endsWith("shiny");
        String baseId = shiny ? clean.substring(0, clean.length() - "shiny".length()) : clean;

        String key = "pokemon.batcg." + baseId;

        // Text.translatable devuelve MutableText
        MutableText name = Text.translatable(key);

        // Si no existe la traducción, getString() devuelve la key tal cual
        if (name.getString().equals(key)) {
            name = Text.literal(prettyNameFromId(baseId));
        }

        if (shiny) {
            name = name.append(Text.literal(" (Shiny)").formatted(Formatting.AQUA));
        }

        return name;
    }

    private static String prettyNameFromId(String baseId) {
        String s = baseId.replaceFirst("^\\d+", ""); // quita números al inicio
        if (s.isEmpty()) return "Unknown";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
