package bat.batcg;

import bat.batcg.client.BatcgModelLoading;
import bat.batcg.client.render.PokemonCardItemRenderer;
import bat.batcg.item.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;

public class BatcgClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        BatcgModelLoading.init();
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.POKEMONCARD, PokemonCardItemRenderer.INSTANCE);
    }
}
