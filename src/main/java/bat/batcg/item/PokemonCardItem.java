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
        if (pokemonId != null) {
            stack.set(ModCardComponents.POKEMON_ID, pokemonId.toLowerCase(Locale.ROOT));
        }
        stack.set(ModCardComponents.CARD_TIER, (tier == null ? CardTier.COMMON : tier).name());
        return stack;
    }


    public static String getPokemonId(ItemStack stack) {
        String id = stack.get(ModCardComponents.POKEMON_ID);
        return id == null ? "" : id;
    }

    public static CardTier getTier(ItemStack stack) {
        String tierName = stack.get(ModCardComponents.CARD_TIER);
        if (tierName == null || tierName.isBlank()) return CardTier.COMMON;

        try {
            return CardTier.valueOf(tierName.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (Exception ignored) {
            return CardTier.COMMON;
        }
    }




    @Override
    public Text getName(ItemStack stack) {
        String pokemonId = stack.get(ModCardComponents.POKEMON_ID);
        String tierName  = stack.get(ModCardComponents.CARD_TIER);

        if (pokemonId == null || pokemonId.isBlank()) {
            return Text.literal("Invalid Card").formatted(Formatting.RED);
        }

        CardTier tier = safeTier(tierName);

        MutableText name = getPokemonNameText(pokemonId).copy().formatted(tier.getColor());

        // ⭐ solo para SHINY
        if (tier == CardTier.SHINY) {
            return Text.literal("★ ").formatted(Formatting.GOLD).append(name);
        }

        return name;
    }

    // ✅ OJO: TooltipContext viene de Item (nested), NO se importa.
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
