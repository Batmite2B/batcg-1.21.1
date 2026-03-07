package bat.batcg.block.entity;

import bat.batcg.Batcg;
import bat.batcg.block.ModBlocks;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBlockEntities {

    public static final BlockEntityType<GradeStationBlockEntity> GRADE_STATION =
            Registry.register(
                    Registries.BLOCK_ENTITY_TYPE,
                    Identifier.of(Batcg.MOD_ID, "grade_station"),
                    BlockEntityType.Builder.create(GradeStationBlockEntity::new, ModBlocks.GRADE_STATION).build(null)
            );

    private ModBlockEntities() {}

    public static void init() { }
}