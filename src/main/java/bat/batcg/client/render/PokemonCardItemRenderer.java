package bat.batcg.client.render;

import bat.batcg.Batcg;
import bat.batcg.card.CardTier;
import bat.batcg.card.ModCardComponents;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class PokemonCardItemRenderer extends SpecialItemRenderer {

    public static final PokemonCardItemRenderer INSTANCE = new PokemonCardItemRenderer();

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        String tierName = stack.get(ModCardComponents.CARD_TIER);
        String pokemonIdRaw = stack.get(ModCardComponents.POKEMON_ID);

        CardTier tier = CardTier.COMMON;
        if (tierName != null) {
            try { tier = CardTier.valueOf(tierName); } catch (Exception ignored) {}
        }

        // Frame siempre por tier
        Identifier frameModelId = Identifier.of(
                Batcg.MOD_ID,
                "item/card/frame/" + tier.name().toLowerCase()
        );

        renderModel(frameModelId, stack, mode, false, matrices, vertexConsumers, light, overlay);

        if (pokemonIdRaw == null || pokemonIdRaw.isBlank()) return;

        String pokemonId = normalizePokemonIdForTier(pokemonIdRaw, tier);

        // Icon según ID ya normalizado
        Identifier iconModelId = Identifier.of(
                Batcg.MOD_ID,
                "item/card/icon/" + pokemonId.toLowerCase()
        );

        renderModel(iconModelId, stack, mode, false, matrices, vertexConsumers, light, overlay, () -> {
            matrices.translate(0.0F, 0.0F, -0.0012F);
        });
    }

    /**
     * Regla:
     * - Tier SHINY => icon shiny obligado.
     * - Otros tiers => icon shiny prohibido.
     *
     * Convención: shiny = "<base>shiny"
     * Ej: "001bulbasaur" <-> "001bulbasaurshiny"
     */
    private static String normalizePokemonIdForTier(String id, CardTier tier) {
        String clean = id.trim().toLowerCase();

        boolean isShinyId = clean.endsWith("shiny");
        boolean wantsShiny = (tier == CardTier.SHINY);

        if (wantsShiny) {
            // Forzar shiny
            return isShinyId ? clean : (clean + "shiny");
        } else {
            // Prohibir shiny
            return isShinyId ? clean.substring(0, clean.length() - "shiny".length()) : clean;
        }
    }
}
