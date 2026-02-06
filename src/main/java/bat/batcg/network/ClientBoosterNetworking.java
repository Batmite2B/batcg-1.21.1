package bat.batcg.network;

import bat.batcg.network.payload.RevealCardC2SPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ClientBoosterNetworking {

    private ClientBoosterNetworking() {}

    public static void requestReveal(int handOrdinal, int slot) {
        ClientPlayNetworking.send(new RevealCardC2SPayload(handOrdinal, slot));
    }
}
