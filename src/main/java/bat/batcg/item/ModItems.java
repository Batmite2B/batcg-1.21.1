package bat.batcg.item;

import bat.batcg.Batcg;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    // ✅ IMPORTANTE: registrar como PokemonCardItem, NO como Item normal
    public static final Item POKEMONCARD =
            registerItem("pokemoncard", new PokemonCardItem(new Item.Settings().maxCount(1)));


    public static final Item BOOSTER_PACK = registerItem(
            "booster_pack",
            new BoosterPackItem(new Item.Settings().maxCount(1))
    );


    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(Batcg.MOD_ID, name), item);
    }

    public static void RegisterModItems() {
        Batcg.LOGGER.info("Registering Mod Items for " + Batcg.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
            entries.add(POKEMONCARD);
        });
    }
}
