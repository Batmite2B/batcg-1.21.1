package bat.batcg.card;

import bat.batcg.Batcg;
import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModCardComponents {

    private ModCardComponents() {}

    public static final ComponentType<String> POKEMON_ID =
            Registry.register(
                    Registries.DATA_COMPONENT_TYPE,
                    Identifier.of(Batcg.MOD_ID, "pokemon_id"),
                    ComponentType.<String>builder().codec(Codec.STRING).build()
            );

    public static final ComponentType<String> CARD_TIER =
            Registry.register(
                    Registries.DATA_COMPONENT_TYPE,
                    Identifier.of(Batcg.MOD_ID, "card_tier"),
                    ComponentType.<String>builder().codec(Codec.STRING).build()
            );

    public static void initialize() {
        // Intentionally empty: class-load registers the types.
    }
}
