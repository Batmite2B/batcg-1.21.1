package bat.batcg.network.payload;

import bat.batcg.Batcg;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record OpenBoosterS2CPayload(int handOrdinal, int revealedMask) implements CustomPayload {

    public static final Id<OpenBoosterS2CPayload> ID =
            new Id<>(Identifier.of(Batcg.MOD_ID, "open_booster"));

    public static final PacketCodec<RegistryByteBuf, OpenBoosterS2CPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, OpenBoosterS2CPayload::handOrdinal,
                    PacketCodecs.VAR_INT, OpenBoosterS2CPayload::revealedMask,
                    OpenBoosterS2CPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
