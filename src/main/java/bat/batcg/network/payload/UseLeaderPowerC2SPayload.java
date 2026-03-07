package bat.batcg.network.payload;

import bat.batcg.Batcg;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * C2S: cliente pide usar el poder del líder.
 * forward/strafe en rango -1..1 (W/S y A/D).
 */
public record UseLeaderPowerC2SPayload(byte forward, byte strafe) implements CustomPayload {

    public static final Id<UseLeaderPowerC2SPayload> ID =
            new Id<>(Identifier.of(Batcg.MOD_ID, "use_leader_power"));

    public static final PacketCodec<RegistryByteBuf, UseLeaderPowerC2SPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BYTE, UseLeaderPowerC2SPayload::forward,
                    PacketCodecs.BYTE, UseLeaderPowerC2SPayload::strafe,
                    UseLeaderPowerC2SPayload::new
            );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}