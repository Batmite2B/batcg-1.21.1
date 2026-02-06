package bat.batcg.network.payload;

import bat.batcg.Batcg;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RevealResultS2CPayload(int slot, String pokemonId, String tierName, int revealedMask) implements CustomPayload {

    public static final Id<RevealResultS2CPayload> ID =
            new Id<>(Identifier.of(Batcg.MOD_ID, "reveal_result"));

    public static final PacketCodec<RegistryByteBuf, RevealResultS2CPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, RevealResultS2CPayload::slot,
                    PacketCodecs.STRING, RevealResultS2CPayload::pokemonId,
                    PacketCodecs.STRING, RevealResultS2CPayload::tierName,
                    PacketCodecs.VAR_INT, RevealResultS2CPayload::revealedMask,
                    RevealResultS2CPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}
