package bat.batcg;

import bat.batcg.client.BatcgModelLoading;
import bat.batcg.client.render.PokemonCardItemRenderer;
import bat.batcg.client.screen.BoosterPackScreen;
import bat.batcg.item.ModItems;
import bat.batcg.network.payload.OpenBoosterS2CPayload;
import bat.batcg.network.payload.RevealResultS2CPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import bat.batcg.block.ModBlocks;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.render.RenderLayer;


public class BatcgClient implements ClientModInitializer {

    private static BoosterPackScreen OPEN_SCREEN;

    @Override
    public void onInitializeClient() {


        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.POKEDOLLAR, RenderLayer.getCutout());


        // ✅ 1) Registrar TODOS los modelos extra (frames + icons)
        BatcgModelLoading.init();

        // ✅ 2) Registrar el renderer del item de carta (para inventario / mano / suelo / GUI)
        BuiltinItemRendererRegistry.INSTANCE.register(bat.batcg.item.ModItems.POKEMONCARD, PokemonCardItemRenderer.INSTANCE);

        // --- Networking (booster) ---
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
