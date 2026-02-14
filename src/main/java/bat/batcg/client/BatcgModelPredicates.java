package bat.batcg.client;

import bat.batcg.Batcg;
import bat.batcg.card.CardIdIndex;
import bat.batcg.card.CardTier;
import bat.batcg.item.PokemonCardItem;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class BatcgModelPredicates {

    private BatcgModelPredicates() {}

    public static void init() {
        Item cardItem = null;
        for (Item it : Registries.ITEM) {
            if (it instanceof PokemonCardItem) {
                cardItem = it;
                break;
            }
        }
        if (cardItem == null) return;




        FabricModelPredicateProviderRegistry.register(
                cardItem,
                Identifier.of(Batcg.MOD_ID, "tier"),
                (stack, world, entity, seed) -> {
                    CardTier tier = PokemonCardItem.getTier(stack);
                    return tier == null ? 0f : (float) tier.ordinal();
                }
        );

        FabricModelPredicateProviderRegistry.register(
                cardItem,
                Identifier.of(Batcg.MOD_ID, "dex"),
                (stack, world, entity, seed) -> {
                    String id = PokemonCardItem.getPokemonId(stack);
                    int dex = CardIdIndex.dexNumber(id);
                    return dex < 0 ? 0f : (float) dex;
                }
        );
    }
}
