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
import bat.batcg.item.PokedollarBlockItem;


public class ModBlocks {

    public static final Block POKEDOLLAR = registerBlock(
            "pokedollar",
            new PokedollarBlock(AbstractBlock.Settings.create().strength(1.0f).nonOpaque())
    );



    private static Block registerBlock(String name, Block block) {
        Registry.register(Registries.BLOCK, Identifier.of(Batcg.MOD_ID, name), block);

        // ✅ Usa el parámetro 'block', NO la variable estática
        Registry.register(Registries.ITEM, Identifier.of(Batcg.MOD_ID, name),
                name.equals("pokedollar")
                        ? new PokedollarBlockItem(block, new Item.Settings())
                        : new BlockItem(block, new Item.Settings())
        );

        return block;
    }


    public static final Block GRADE_STATION = registerBlock(
            "grade_station",
            new GradeStationBlock(AbstractBlock.Settings.create().strength(2.5f).nonOpaque())
    );


    public static void RegisterModBlocks() {
        Batcg.LOGGER.info("Registering Mod Blocks for " + Batcg.MOD_ID);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register(entries -> {
            entries.add(POKEDOLLAR);
        });
    }
}
