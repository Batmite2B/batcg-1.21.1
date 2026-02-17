package bat.batcg.network.payload;

import bat.batcg.Batcg;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: cliente pide abrir el belt (tecla K).
 * No lleva data.
 */
public record OpenBeltC2SPayload(boolean ignored) implements CustomPayload {

    public OpenBeltC2SPayload() {
        this(true);
    }

    public static final Id<OpenBeltC2SPayload> ID =
            new Id<>(Identifier.of(Batcg.MOD_ID, "open_belt"));

    public static final PacketCodec<RegistryByteBuf, OpenBeltC2SPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BOOL, OpenBeltC2SPayload::ignored,
                    OpenBeltC2SPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
