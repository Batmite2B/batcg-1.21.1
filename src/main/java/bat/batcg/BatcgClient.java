package bat.batcg;

import bat.batcg.client.screen.BoosterPackScreen;
import bat.batcg.network.payload.OpenBoosterS2CPayload;
import bat.batcg.network.payload.RevealResultS2CPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;

public class BatcgClient implements ClientModInitializer {

    private static BoosterPackScreen OPEN_SCREEN;

    @Override
    public void onInitializeClient() {

        ClientPlayNetworking.registerGlobalReceiver(OpenBoosterS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                OPEN_SCREEN = new BoosterPackScreen(payload.handOrdinal(), payload.revealedMask());
                MinecraftClient.getInstance().setScreen(OPEN_SCREEN);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(RevealResultS2CPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                if (OPEN_SCREEN != null) {
                    OPEN_SCREEN.applyRevealFromServer(
                            payload.slot(),
                            payload.pokemonId(),
                            payload.tierName(),
                            payload.revealedMask()
                    );
                }
            });
        });
    }
}
