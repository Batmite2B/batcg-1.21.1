package bat.batcg.belt;

import bat.batcg.Batcg;
import com.mojang.serialization.Codec;
import net.minecraft.component.ComponentType;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModBeltComponents {

    private ModBeltComponents() {}

    // Si tu mod ya tenía un nombre distinto aquí, ponle el mismo nombre que ya estabas usando.
    // La idea: almacenar NBT con codec + packetCodec válidos.
    public static final ComponentType<NbtComponent> BELT_DATA =
            register("belt_data", NbtComponent.CODEC, NbtComponent.PACKET_CODEC);

    /** Llamado desde tu Batcg.onInitialize(). No tiene que hacer nada; con esto fuerzas a cargar la clase. */


    public static void initialize() {
        register();
    }


    public static void register() {
        // Intencionalmente vacío
    }

    private static <T> ComponentType<T> register(String path, Codec<T> codec, PacketCodec<? super RegistryByteBuf, T> packetCodec) {
        return Registry.register(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.of(Batcg.MOD_ID, path),
                ComponentType.<T>builder()
                        .codec(codec)              // ✅ ESTO ES LO QUE TE FALTABA
                        .packetCodec(packetCodec)  // ✅ para sync red
                        .build()
        );
    }
}
