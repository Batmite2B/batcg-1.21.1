package bat.batcg.item;

import bat.batcg.Batcg;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item POKEMONCARD = registerItem("pokemoncard",new Item(new Item.Settings()));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(Batcg.MOD_ID, name), item);
    }




    public static void RegisterModItems(){
        Batcg.LOGGER.info("Registering Mod Items for "+ Batcg.MOD_ID);


        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(fabricItemGroupEntries -> {

            fabricItemGroupEntries.add(POKEMONCARD);

        });

    }

}
