package bat.batcg.block;

import bat.batcg.Batcg;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final Block POKEDOLLAR = registerBlock(
            "pokedollar",
            new PokedollarBlock(AbstractBlock.Settings.create().strength(1.0f).nonOpaque())
    );



    private static Block registerBlock(String name, Block block) {
        Registry.register(Registries.BLOCK, Identifier.of(Batcg.MOD_ID, name), block);
        Registry.register(Registries.ITEM, Identifier.of(Batcg.MOD_ID, name), new BlockItem(block, new Item.Settings()));
        return block;
    }

    public static void RegisterModBlocks() {
        Batcg.LOGGER.info("Registering Mod Blocks for " + Batcg.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.add(POKEDOLLAR);
        });
    }
}
