package bat.batcg.card;

import bat.batcg.Batcg;
import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModCardComponents {

    private static boolean INITIALIZED = false;

    public static ComponentType<String> POKEMON_ID;
    public static ComponentType<String> CARD_TIER;

    private ModCardComponents() {}

    public static void initialize() {
        if (INITIALIZED) return;
        INITIALIZED = true;

        POKEMON_ID = register("pokemon_id", Codec.STRING);
        CARD_TIER  = register("card_tier",  Codec.STRING);
    }

    /** Útil para fallar con un error claro si algo se usa antes del init. */
    public static void ensureInitialized() {
        if (!INITIALIZED || POKEMON_ID == null || CARD_TIER == null) {
            throw new IllegalStateException(
                    "ModCardComponents not initialized. " +
                            "Call ModCardComponents.initialize() in Batcg#onInitialize (before registries freeze)."
            );
        }
    }

    private static <T> ComponentType<T> register(String name, Codec<T> codec) {
        return Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of(Batcg.MOD_ID, name),
                ComponentType.<T>builder().codec(codec).build()
        );
    }
}
