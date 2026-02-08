package bat.batcg.client;

import bat.batcg.Batcg;
import bat.batcg.card.CardIdIndex;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.util.Identifier;

public final class BatcgModelLoading {

    private BatcgModelLoading() {}

    public static void init() {
        ModelLoadingPlugin.register(ctx -> {

            // Frames
            addItem(ctx, "item/card/frame/common");
            addItem(ctx, "item/card/frame/uncommon");
            addItem(ctx, "item/card/frame/rare");
            addItem(ctx, "item/card/frame/epic");
            addItem(ctx, "item/card/frame/legendary");
            addItem(ctx, "item/card/frame/shiny");
            addItem(ctx, "item/card/base");

            // Booster pack
            addItem(ctx, "item/booster_pack");

            // Icons
            for (String id : CardIdIndex.allIds()) {
                addItem(ctx, "item/card/icon/" + id);
            }
        });
    }

    private static void addItem(ModelLoadingPlugin.Context ctx, String path) {
        Identifier base = Identifier.of(Batcg.MOD_ID, path);
        ctx.addModels(base);
    }
}
