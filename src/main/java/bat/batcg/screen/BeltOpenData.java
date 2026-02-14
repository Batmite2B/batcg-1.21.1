package bat.batcg.screen;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

public record BeltOpenData(int handId) {
    public static final PacketCodec<RegistryByteBuf, BeltOpenData> PACKET_CODEC =
            PacketCodec.tuple(PacketCodecs.VAR_INT, BeltOpenData::handId, BeltOpenData::new);
}
