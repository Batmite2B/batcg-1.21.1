package bat.batcg;

import bat.batcg.client.BatcgBeltKeybinds;
import bat.batcg.client.BatcgModelLoading;
import bat.batcg.client.render.PokemonCardItemRenderer;
import bat.batcg.client.screen.BoosterPackScreen;
import bat.batcg.network.payload.OpenBoosterS2CPayload;
import bat.batcg.network.payload.RevealResultS2CPayload;
import bat.batcg.screen.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import bat.batcg.block.ModBlocks;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.client.render.RenderLayer;

// ✅ Belt feature renderer registration
import bat.batcg.client.render.BeltFeatureRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import bat.batcg.client.screen.BeltScreen;
import bat.batcg.screen.ModScreenHandlers;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import bat.batcg.client.screen.BeltScreen;
import bat.batcg.screen.ModScreenHandlers;
import net.minecraft.client.gui.screen.ingame.HandledScreens;




public class BatcgClient implements ClientModInitializer {

    private static BoosterPackScreen OPEN_SCREEN;

    @Override
    public void onInitializeClient() {



        HandledScreens.register(ModScreenHandlers.BELT, BeltScreen::new);



        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.POKEDOLLAR, RenderLayer.getCutout());

        // ✅ 1) Registrar TODOS los modelos extra (frames + icons)
        BatcgModelLoading.init();

        BatcgBeltKeybinds.init();



        // ✅ 2) Registrar el renderer del item de carta (para inventario / mano / suelo / GUI)
        BuiltinItemRendererRegistry.INSTANCE.register(bat.batcg.item.ModItems.POKEMONCARD, PokemonCardItemRenderer.INSTANCE);

        // ✅ 3) Añadir renderer del cinturón al jugador (sin Mixins)
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof PlayerEntityRenderer playerRenderer) {
                @SuppressWarnings("unchecked")
                FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx =
                        (FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>>) (Object) playerRenderer;

                registrationHelper.register(new BeltFeatureRenderer(ctx));
            }
        });

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
