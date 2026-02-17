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

        tooltip.add(
                Text.literal("Rarity: ").formatted(Formatting.DARK_GRAY)
                        .append(Text.literal(tier.getDisplayName()).formatted(tier.getColor()))
        );
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
