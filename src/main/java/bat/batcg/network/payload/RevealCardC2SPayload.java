package bat.batcg.network.payload;

import bat.batcg.Batcg;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RevealCardC2SPayload(int handOrdinal, int slot) implements CustomPayload {

    public static final Id<RevealCardC2SPayload> ID =
            new Id<>(Identifier.of(Batcg.MOD_ID, "reveal_card"));

    public static final PacketCodec<RegistryByteBuf, RevealCardC2SPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, RevealCardC2SPayload::handOrdinal,
                    PacketCodecs.VAR_INT, RevealCardC2SPayload::slot,
                    RevealCardC2SPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
