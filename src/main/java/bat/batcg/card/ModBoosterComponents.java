package bat.batcg.card;

import bat.batcg.Batcg;
import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBoosterComponents {

    public static final ComponentType<Long> BOOSTER_SEED = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Batcg.MOD_ID, "booster_seed"),
            ComponentType.<Long>builder().codec(Codec.LONG).build()
    );

    // bitmask: 1<<0, 1<<1, 1<<2
    public static final ComponentType<Integer> BOOSTER_REVEALED = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Batcg.MOD_ID, "booster_revealed"),
            ComponentType.<Integer>builder().codec(Codec.INT).build()
    );

    public static final ComponentType<String> BOOSTER_ID0 = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Batcg.MOD_ID, "booster_id0"),
            ComponentType.<String>builder().codec(Codec.STRING).build()
    );

    public static final ComponentType<String> BOOSTER_ID1 = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Batcg.MOD_ID, "booster_id1"),
            ComponentType.<String>builder().codec(Codec.STRING).build()
    );

    public static final ComponentType<String> BOOSTER_ID2 = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Batcg.MOD_ID, "booster_id2"),
            ComponentType.<String>builder().codec(Codec.STRING).build()
    );

    public static final ComponentType<String> BOOSTER_TIER0 = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Batcg.MOD_ID, "booster_tier0"),
            ComponentType.<String>builder().codec(Codec.STRING).build()
    );

    public static final ComponentType<String> BOOSTER_TIER1 = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Batcg.MOD_ID, "booster_tier1"),
            ComponentType.<String>builder().codec(Codec.STRING).build()
    );

    public static final ComponentType<String> BOOSTER_TIER2 = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            Identifier.of(Batcg.MOD_ID, "booster_tier2"),
            ComponentType.<String>builder().codec(Codec.STRING).build()
    );

    public static void initialize() {}
}
